import { useMutation } from '@tanstack/react-query';
import { useNavigate } from 'react-router';

import { paths } from '@/routes/paths';

import { login, logoutRequest, register } from './api';
import { useAuthStore } from './authStore';
import type { AuthResponse, LoginPayload, RegisterPayload } from './types';

/**
 * Inicio de sesion y registro comparten el final: guardar la sesion y llevar al
 * usuario a donde iba.
 */
function useAuthMutation<TPayload>(
  mutationFn: (payload: TPayload) => Promise<AuthResponse>,
  redirectTo: string,
) {
  const setSession = useAuthStore((state) => state.setSession);
  const navigate = useNavigate();

  return useMutation({
    mutationFn,
    onSuccess: (response) => {
      setSession(response.accessToken, response.user);
      navigate(redirectTo, { replace: true });
    },
  });
}

export function useLogin(redirectTo: string = paths.dashboard) {
  return useAuthMutation<LoginPayload>(login, redirectTo);
}

export function useRegister(redirectTo: string = paths.dashboard) {
  return useAuthMutation<RegisterPayload>(register, redirectTo);
}

/**
 * Cierra la sesion en el servidor y en el cliente.
 *
 * Si la llamada al servidor falla se limpia igual: dejar al usuario dentro
 * porque no hubo red seria peor que revocar de mas.
 */
export function useLogout() {
  const clearSession = useAuthStore((state) => state.clearSession);
  const navigate = useNavigate();

  return async () => {
    try {
      await logoutRequest();
    } catch {
      // Sin conexion no se puede revocar del lado del servidor. La cookie
      // caduca sola y la sesion local se cierra de todos modos.
    }
    clearSession();
    navigate(paths.login, { replace: true });
  };
}

export function useCurrentUser() {
  return useAuthStore((state) => state.user);
}

export function useSessionStatus() {
  return useAuthStore((state) => state.status);
}
