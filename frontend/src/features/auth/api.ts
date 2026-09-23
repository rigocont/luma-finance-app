import { api } from '@/lib/api/client';

import type { AuthResponse, AuthenticatedUser, LoginPayload, RegisterPayload } from './types';

export const authKeys = {
  me: ['auth', 'me'] as const,
};

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/login', payload);
  return data;
}

export async function register(payload: RegisterPayload): Promise<AuthResponse> {
  const { data } = await api.post<AuthResponse>('/auth/register', payload);
  return data;
}

export async function fetchCurrentUser(): Promise<AuthenticatedUser> {
  const { data } = await api.get<AuthenticatedUser>('/auth/me');
  return data;
}

/**
 * Cierra la sesion en el servidor: revoca el token de renovacion y borra la
 * cookie. Sin esta llamada la cookie seguiria siendo valida.
 */
export async function logoutRequest(): Promise<void> {
  await api.post('/auth/logout');
}

// La renovacion vive en el cliente HTTP, no aqui: el interceptor la necesita y
// tenerla alli evita una dependencia circular entre modulos.
export { refreshSession } from '@/lib/api/client';
