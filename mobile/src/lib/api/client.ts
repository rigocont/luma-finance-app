import axios, { AxiosError, type AxiosInstance, type InternalAxiosRequestConfig } from 'axios';
import { randomUUID } from 'expo-crypto';

import {
  clearSession,
  getAccessToken,
  isAuthenticated,
  setSession,
} from '@/features/auth/authStore';
import type { AuthResponse } from '@/features/auth/types';
import {
  clearStoredRefreshToken,
  getStoredRefreshToken,
  setStoredRefreshToken,
} from '@/lib/storage/secureStore';

import { ApiError, type ApiProblem, type ErrorCode } from './types';

/**
 * `EXPO_PUBLIC_*` es la unica forma de que una variable de entorno llegue al
 * bundle de la app (Expo la inyecta en build time). Sin ella definida cae al
 * backend local, para que `npm run start` funcione desde el primer momento
 * apuntando al mismo `localhost:8080` que usa el frontend web en desarrollo.
 */
const BASE_URL = process.env.EXPO_PUBLIC_API_BASE_URL ?? 'http://localhost:8080/api/v1';

/**
 * Endpoints de sesion.
 *
 * Un 401 en cualquiera de estos es una respuesta legitima (credenciales
 * malas, refresh token vencido), no una señal de que haya que renovar.
 * Intentar renovar aqui produciria un bucle.
 */
const SESSION_ENDPOINTS = ['/auth/login', '/auth/register', '/auth/refresh', '/auth/logout'];

interface RetriableConfig extends InternalAxiosRequestConfig {
  _alreadyRetried?: boolean;
}

export const api: AxiosInstance = axios.create({
  baseURL: BASE_URL,
  timeout: 15_000,
  headers: {
    'Content-Type': 'application/json',
    // Le dice al backend que el refresh token va por cabecera, no por
    // cookie: un dispositivo movil no maneja cookies de navegador. Ver
    // AuthController (backend) y docs/roadmap-mobile.md, seccion 2.
    'X-Client-Type': 'mobile',
  },
});

api.interceptors.request.use(async (config) => {
  config.headers.set('X-Correlation-Id', randomUUID());

  const token = getAccessToken();
  if (token) {
    config.headers.set('Authorization', `Bearer ${token}`);
  }

  return config;
});

/**
 * Renovacion con un solo vuelo (igual que en el frontend web): si varias
 * peticiones reciben 401 a la vez, se hace UNA sola renovacion y todas
 * esperan su resultado.
 */
let inFlightRefresh: Promise<void> | null = null;

export function refreshSession(): Promise<void> {
  if (!inFlightRefresh) {
    inFlightRefresh = doRefresh().finally(() => {
      inFlightRefresh = null;
    });
  }
  return inFlightRefresh;
}

async function doRefresh(): Promise<void> {
  const refreshToken = await getStoredRefreshToken();
  if (!refreshToken) {
    throw new Error('No hay refresh token guardado');
  }

  const { data } = await api.post<AuthResponse>(
    '/auth/refresh',
    {},
    { headers: { 'X-Refresh-Token': refreshToken } },
  );

  setSession(data.accessToken, data.user);
  await setStoredRefreshToken(data.refreshToken);
}

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError<ApiProblem>) => {
    if (!error.response) {
      return Promise.reject(
        new ApiError({
          message: 'No hay conexion. Revisa tu internet e intenta de nuevo.',
          status: 0,
          code: 'NETWORK_ERROR',
        }),
      );
    }

    const original = error.config as RetriableConfig | undefined;
    const url = original?.url ?? '';
    const isSessionEndpoint = SESSION_ENDPOINTS.some((path) => url.includes(path));

    // El access token expiro: se renueva una vez y se reintenta la peticion
    // original. Para la persona esto es invisible.
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
        await clearStoredRefreshToken();
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
  if (status === 401) return 'Tu sesion expiro. Vuelve a iniciar sesion.';
  if (status === 403) return 'No tienes acceso a este recurso.';
  if (status === 404) return 'No se encontro lo que buscabas.';
  return 'Algo salio mal en el servidor. Intenta de nuevo.';
}
