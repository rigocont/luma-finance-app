import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';

import {
  clearSession,
  getAccessToken,
  isAuthenticated,
  setSession,
} from '@/features/auth/authStore';
import type { AuthResponse } from '@/features/auth/types';

import { ApiError, type ApiProblem, type ErrorCode } from './types';

const BASE_URL = import.meta.env.VITE_API_BASE_URL ?? '/api/v1';

/**
 * Endpoints de sesion.
 *
 * Un 401 en cualquiera de estos es una respuesta legitima (credenciales malas,
 * cookie vencida), no una señal de que haya que renovar. Intentar renovar aqui
 * produciria un bucle.
 */
const SESSION_ENDPOINTS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout'];

interface RetriableConfig extends InternalAxiosRequestConfig {
  _alreadyRetried?: boolean;
}

export const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
  headers: { 'Content-Type': 'application/json' },
  // Indispensable: sin esto la cookie de renovacion no viaja.
  withCredentials: true,
});

/**
 * Cada peticion lleva un identificador de correlacion y, si hay sesion, el token.
 *
 * El identificador vuelve en la respuesta y queda escrito en los logs del
 * backend, asi que un error reportado por el usuario se rastrea hasta su linea.
 */
api.interceptors.request.use((config) => {
  config.headers.set('X-Correlation-Id', crypto.randomUUID());

  const token = getAccessToken();
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }

  return config;
});

/**
 * Renovacion con un solo vuelo.
 *
 * Si diez peticiones reciben 401 a la vez, se hace UNA sola renovacion y las
 * diez esperan su resultado. Es indispensable con rotacion de tokens: dos
 * renovaciones simultaneas presentarian la misma cookie, y la segunda llegaria
 * con un token ya rotado. El backend tiene un margen para esa carrera, pero lo
 * correcto es no provocarla.
 */
let inFlightRefresh: Promise<void> | null = null;

export function refreshSession(): Promise<void> {
  if (!inFlightRefresh) {
    inFlightRefresh = api
      .post<AuthResponse>('/auth/refresh')
      .then(({ data }) => {
        setSession(data.accessToken, data.user);
      })
      .finally(() => {
        inFlightRefresh = null;
      });
  }
  return inFlightRefresh;
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiProblem>) => {
    if (!error.response) {
      return Promise.reject(
        new ApiError({
          message: 'No pudimos conectarnos. Revisa tu conexion e intentalo de nuevo.',
          status: 0,
          code: 'NETWORK_ERROR',
        }),
      );
    }

    const original = error.config as RetriableConfig | undefined;
    const url = original?.url ?? '';
    const isSessionEndpoint = SESSION_ENDPOINTS.some((path) => url.includes(path));

    // El access token expiro: se renueva una vez y se reintenta la peticion
    // original. Para el usuario esto es invisible.
    if (
      error.response.status === 401 &&
      original &&
      !isSessionEndpoint &&
      !original._alreadyRetried &&
      isAuthenticated()
    ) {
      original._alreadyRetried = true;
      try {
        await refreshSession();
        return await api(original);
      } catch {
        // La renovacion tampoco sirvio: la sesion se acabo de verdad.
        clearSession();
      }
    }

    const problem = error.response.data;
    return Promise.reject(
      new ApiError({
        message: problem?.detail ?? fallbackMessage(error.response.status),
        status: error.response.status,
        code: problem?.errorCode ?? codeFromStatus(error.response.status),
        fieldErrors: problem?.errors ?? [],
        traceId: problem?.traceId,
      }),
    );
  },
);

function codeFromStatus(status: number): ErrorCode {
  switch (status) {
    case 400:
      return 'VALIDATION_ERROR';
    case 401:
      return 'UNAUTHORIZED';
    case 403:
      return 'FORBIDDEN';
    case 404:
      return 'RESOURCE_NOT_FOUND';
    case 409:
      return 'CONFLICT';
    case 422:
      return 'BUSINESS_RULE_VIOLATION';
    default:
      return 'INTERNAL_ERROR';
  }
}

function fallbackMessage(status: number): string {
  if (status === 401) return 'Tu sesion expiro. Inicia sesion de nuevo.';
  if (status === 403) return 'No tienes acceso a esta seccion.';
  if (status === 404) return 'No encontramos lo que buscabas.';
  return 'Algo fallo de nuestro lado. Intentalo de nuevo en un momento.';
}
