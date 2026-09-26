import * as SecureStore from 'expo-secure-store';

/**
 * El unico dato de sesion que sobrevive a cerrar la app: el refresh token.
 *
 * El access token NUNCA se guarda aqui (ni en ningun almacenamiento
 * persistente): vive solo en memoria, en el authStore, igual que en el
 * frontend web. Ver docs/roadmap-mobile.md, seccion 2.
 */
const REFRESH_TOKEN_KEY = 'luma.refreshToken';

export async function getStoredRefreshToken(): Promise<string | null> {
  return SecureStore.getItemAsync(REFRESH_TOKEN_KEY);
}

export async function setStoredRefreshToken(token: string): Promise<void> {
  await SecureStore.setItemAsync(REFRESH_TOKEN_KEY, token);
}

export async function clearStoredRefreshToken(): Promise<void> {
  await SecureStore.deleteItemAsync(REFRESH_TOKEN_KEY);
}
