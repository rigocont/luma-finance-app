export interface AuthenticatedUser {
  id: string;
  email: string;
  name: string;
  status: string;
  onboardingCompleted: boolean;
}

export interface AuthResponse {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
  user: AuthenticatedUser;
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
