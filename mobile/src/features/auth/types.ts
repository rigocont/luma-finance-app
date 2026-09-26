export interface AuthenticatedUser {
  id: string;
  email: string;
  name: string;
  status: string;
  onboardingCompleted: boolean;
}

/**
 * Respuesta de /auth/login, /auth/register y /auth/refresh vista por el
 * cliente movil.
 *
 * `refreshToken` SI viaja en el cuerpo (a diferencia del cliente web, que lo
 * recibe en una cookie HttpOnly): la cabecera `X-Client-Type: mobile` le pide
 * al backend ese formato porque un dispositivo movil no tiene donde recibir
 * una cookie de navegador. Ver AuthController.mobileSession en el backend.
 */
export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthenticatedUser;
  refreshToken: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface RegisterPayload {
  email: string;
  name: string;
  password: string;
}
