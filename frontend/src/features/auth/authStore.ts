import { create } from 'zustand';

import type { AuthenticatedUser } from './types';

/**
 * - `bootstrapping`: la aplicacion aun no sabe si hay sesion. Esta intentando
 *   renovarla con la cookie. No se debe pintar ni el login ni la aplicacion.
 * - `anonymous`: no hay sesion.
 * - `authenticated`: hay sesion.
 */
export type SessionStatus = 'bootstrapping' | 'anonymous' | 'authenticated';

interface AuthState {
  status: SessionStatus;
  accessToken: string | null;
  user: AuthenticatedUser | null;
  setSession: (accessToken: string, user: AuthenticatedUser) => void;
  clearSession: () => void;
  /**
   * Marca el alta como terminada sin volver a pedir la sesion.
   *
   * El dato vive en el token de la sesion, y renovarla entera para cambiar un
   * booleano seria una vuelta al servidor por algo que ya sabemos: el servidor
   * acaba de confirmarlo al responder.
   */
  markOnboardingCompleted: () => void;
}

/**
 * Sesion del usuario.
 *
 * IMPORTANTE: este store NO se persiste. El access token vive solo en memoria.
 *
 * Guardarlo en localStorage lo deja al alcance de cualquier script inyectado en
 * la pagina, y con el se roba la sesion completa. Lo que sobrevive a una recarga
 * es el token de renovacion, que viaja en una cookie HttpOnly que el JavaScript
 * de la pagina no puede leer.
 *
 * Ver docs/00-arquitectura-fase-0.md, seccion 1.7.
 */
export const useAuthStore = create<AuthState>((set) => ({
  status: 'bootstrapping',
  accessToken: null,
  user: null,

  setSession: (accessToken, user) => set({ status: 'authenticated', accessToken, user }),
  clearSession: () => set({ status: 'anonymous', accessToken: null, user: null }),
  markOnboardingCompleted: () =>
    set((state) => (state.user ? { user: { ...state.user, onboardingCompleted: true } } : state)),
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

export function markOnboardingCompleted(): void {
  useAuthStore.getState().markOnboardingCompleted();
}
