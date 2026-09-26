import { create } from 'zustand';

import type { AuthenticatedUser } from './types';

/**
 * - `bootstrapping`: la app todavia no sabe si hay sesion. Esta intentando
 *   renovarla con el refresh token guardado en el dispositivo. No se debe
 *   pintar ni el login ni la app.
 * - `anonymous`: no hay sesion (o el refresh token guardado ya no sirve).
 * - `authenticated`: hay sesion.
 */
export type SessionStatus = 'bootstrapping' | 'anonymous' | 'authenticated';

interface AuthState {
  status: SessionStatus;
  accessToken: string | null;
  user: AuthenticatedUser | null;
  setSession: (accessToken: string, user: AuthenticatedUser) => void;
  clearSession: () => void;
  markAnonymous: () => void;
}

/**
 * Sesion del usuario en el cliente movil.
 *
 * IMPORTANTE: este store NO se persiste. El access token vive solo en
 * memoria, igual que en el frontend web (ver
 * frontend/src/features/auth/authStore.ts): perderlo al cerrar la app es
 * intencional, para eso existe el refresh token guardado con
 * expo-secure-store (`src/lib/storage/secureStore.ts`), que es lo unico que
 * sobrevive a reiniciar la app.
 */
export const useAuthStore = create<AuthState>((set) => ({
  status: 'bootstrapping',
  accessToken: null,
  user: null,

  setSession: (accessToken, user) => set({ status: 'authenticated', accessToken, user }),
  clearSession: () => set({ status: 'anonymous', accessToken: null, user: null }),
  markAnonymous: () => set({ status: 'anonymous' }),
}));

/* Accesos fuera de React, para el cliente HTTP. */

export function getAccessToken(): string | null {
  return useAuthStore.getState().accessToken;
}

export function setSession(accessToken: string, user: AuthenticatedUser): void {
  useAuthStore.getState().setSession(accessToken, user);
}

export function clearSession(): void {
  useAuthStore.getState().clearSession();
}

export function isAuthenticated(): boolean {
  return useAuthStore.getState().status === 'authenticated';
}
