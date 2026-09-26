import CssBaseline from '@mui/material/CssBaseline';
import { ThemeProvider } from '@mui/material/styles';
import useMediaQuery from '@mui/material/useMediaQuery';
import { QueryClient, QueryClientProvider } from '@tanstack/react-query';
import { useMemo, type ReactNode } from 'react';

import { ToastHost } from '@/components/ui/ToastHost';
import { ApiError } from '@/lib/api/types';
import { resolveThemeMode, useUiStore } from '@/store/uiStore';
import { createLumaTheme } from '@/theme';

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      refetchOnWindowFocus: false,
      retry: (failureCount, error) => {
        // Reintentar un 404 o un error de validacion no cambia el resultado.
        if (error instanceof ApiError && error.status >= 400 && error.status < 500) {
          return false;
        }
        return failureCount < 2;
      },
    },
    mutations: {
      retry: false,
    },
  },
});

export function Providers({ children }: { children: ReactNode }) {
  const themePreference = useUiStore((state) => state.themePreference);
  const systemPrefersDark = useMediaQuery('(prefers-color-scheme: dark)');

  const theme = useMemo(
    () => createLumaTheme(resolveThemeMode(themePreference, systemPrefersDark)),
    [themePreference, systemPrefersDark],
  );

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider theme={theme}>
        <CssBaseline />
        {children}
        {/* Uno solo para toda la aplicacion: cualquier pantalla pide un aviso
            con showToast() sin recibir nada por props. */}
        <ToastHost />
      </ThemeProvider>
    </QueryClientProvider>
  );
}
