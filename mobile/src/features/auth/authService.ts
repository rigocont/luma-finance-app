import { api } from '@/lib/api/client';
import {
  clearStoredRefreshToken,
  getStoredRefreshToken,
  setStoredRefreshToken,
} from '@/lib/storage/secureStore';

import { clearSession, setSession } from './authStore';
import type { AuthResponse, LoginPayload, RegisterPayload } from './types';

async function applySession(auth: AuthResponse): Promise<void> {
  setSession(auth.accessToken, auth.user);
  await setStoredRefreshToken(auth.refreshToken);
}

export async function login(payload: LoginPayload): Promise<void> {
  const { data } = await api.post<AuthResponse>('/auth/login', payload);
  await applySession(data);
}

export async function register(payload: RegisterPayload): Promise<void> {
  const { data } = await api.post<AuthResponse>('/auth/register', payload);
  await applySession(data);
}

/**
 * Cierra la sesion de este dispositivo. Es idempotente del lado del backend:
 * cerrar una sesion que ya no existe tambien responde bien, asi que aqui no
 * hace falta distinguir el caso "ya no habia refresh token guardado".
 */
export async function logout(): Promise<void> {
  try {
    const refreshToken = await getStoredRefreshToken();
    await api.post(
      '/auth/logout',
      {},
      refreshToken ? { headers: { 'X-Refresh-Token': refreshToken } } : undefined,
    );
  } finally {
    clearSession();
    await clearStoredRefreshToken();
  }
}
