import { api } from '@/lib/api/client';

export interface ForgotPasswordPayload {
  email: string;
}

export interface ResetPasswordPayload {
  token: string;
  newPassword: string;
}

export interface ChangePasswordPayload {
  currentPassword: string;
  newPassword: string;
}

/**
 * Pide el enlace de recuperacion.
 *
 * Responde 202 exista o no la cuenta, asi que el cliente no puede distinguir un
 * caso del otro. La interfaz tampoco debe intentarlo.
 */
export async function requestPasswordReset(payload: ForgotPasswordPayload): Promise<void> {
  await api.post('/auth/password/forgot', payload);
}

export async function resetPassword(payload: ResetPasswordPayload): Promise<void> {
  await api.post('/auth/password/reset', payload);
}

/** Cierra todas las sesiones del usuario, incluida la de este dispositivo. */
export async function changePassword(payload: ChangePasswordPayload): Promise<void> {
  await api.post('/auth/password/change', payload);
}
