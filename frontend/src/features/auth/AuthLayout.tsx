import Box from '@mui/material/Box';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import type { ReactNode } from 'react';

import { LumaMark } from '@/components/layout/LumaMark';

interface AuthLayoutProps {
  title: string;
  subtitle: string;
  children: ReactNode;
  footer?: ReactNode;
}

/**
 * Marco de las pantallas de sesion.
 *
 * Sin barra lateral ni encabezado: aqui todavia no hay sesion, asi que no hay
 * nada que navegar.
 */
export function AuthLayout({ title, subtitle, children, footer }: AuthLayoutProps) {
  return (
    <Box
      sx={{
        minHeight: '100dvh',
        display: 'flex',
        alignItems: 'center',
        justifyContent: 'center',
        px: 4,
        py: 12,
        backgroundColor: 'background.default',
      }}
    >
      <Stack spacing={8} sx={{ width: '100%', maxWidth: 380 }}>
        <Stack spacing={4}>
          <Stack direction="row" spacing={2.5} sx={{ alignItems: 'center' }}>
            <LumaMark size={32} />
            <Typography
              component="span"
              sx={{ fontWeight: 800, letterSpacing: '0.14em', fontSize: '1.1875rem' }}
            >
              LUMA
            </Typography>
          </Stack>

          <Stack spacing={1.5}>
            <Typography variant="h2">{title}</Typography>
            <Typography variant="body1" color="text.secondary">
              {subtitle}
            </Typography>
          </Stack>
        </Stack>

        {children}

        {footer}
      </Stack>
    </Box>
  );
}
