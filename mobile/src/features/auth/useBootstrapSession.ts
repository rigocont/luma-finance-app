import { useEffect } from 'react';

import { refreshSession } from '@/lib/api/client';
import { getStoredRefreshToken } from '@/lib/storage/secureStore';

import { useAuthStore } from './authStore';

/**
 * Se monta una sola vez, en el layout raiz (`src/app/_layout.tsx`).
 *
 * Al abrir la app no se sabe si hay sesion: solo se sabe si hay un refresh
 * token guardado en el dispositivo. Este hook lo intenta canjear por un
 * access token nuevo antes de decidir a donde navegar (login o la app), el
 * mismo patron que `useLanguageSync`/el bootstrap del frontend web, adaptado
 * a que aqui el token persistente vive en `expo-secure-store` y no en una
 * cookie.
 */
export function useBootstrapSession(): void {
  const markAnonymous = useAuthStore((state) => state.markAnonymous);

  useEffect(() => {
    let isMounted = true;

    async function bootstrap() {
      const storedRefreshToken = await getStoredRefreshToken();
      if (!storedRefreshToken) {
        if (isMounted) markAnonymous();
        return;
      }

      try {
        await refreshSession();
      } catch {
        // El refresh token guardado ya no sirve (vencio o fue revocado).
        if (isMounted) markAnonymous();
      }
    }

    bootstrap();

    return () => {
      isMounted = false;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);
}
