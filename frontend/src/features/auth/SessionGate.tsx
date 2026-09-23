import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useEffect, type ReactNode } from 'react';

import { LumaMark } from '@/components/layout/LumaMark';
import { testIds } from '@/lib/testids';

import { refreshSession } from './api';
import { clearSession, useAuthStore } from './authStore';

/**
 * Decide si hay sesion antes de pintar nada.
 *
 * Al cargar la aplicacion se intenta renovar con la cookie. Sin este paso, un
 * usuario con sesion valida veria parpadear la pantalla de inicio de sesion
 * durante un instante antes de entrar.
 *
 * La renovacion tiene un solo vuelo, asi que el doble montaje de StrictMode en
 * desarrollo no dispara dos peticiones con la misma cookie.
 */
export function SessionGate({ children }: { children: ReactNode }) {
  const status = useAuthStore((state) => state.status);

  useEffect(() => {
    if (useAuthStore.getState().status !== 'bootstrapping') {
      return;
    }

    refreshSession().catch(() => {
      // Sin cookie valida simplemente no hay sesion. No es un error que reportar.
      clearSession();
    });
  }, []);

  if (status === 'bootstrapping') {
    return <BootSplash />;
  }

  return <>{children}</>;
}

function BootSplash() {
  return (
    <Box
      data-testid={testIds.auth.bootSplash}
      sx={{
        minHeight: '100dvh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        backgroundColor: 'background.default',
      }}
    >
      <Stack spacing={3} sx={{ alignItems: 'center' }}>
        <LumaMark size={36} />
        <Typography variant="caption" color="text.disabled" sx={{ letterSpacing: '0.14em' }}>
          LUMA
        </Typography>
      </Stack>
    </Box>
  );
}
