import Skeleton from '@mui/material/Skeleton';
import Stack from '@mui/material/Stack';

import { testIds } from '@/lib/testids';

/**
 * Estado de carga.
 *
 * Se usan esqueletos con la forma del contenido real en lugar de un spinner:
 * la pantalla no salta cuando llegan los datos.
 */
export function LoadingState({ rows = 3 }: { rows?: number }) {
  return (
    <Stack spacing={3} data-testid={testIds.state.loading} aria-busy="true" aria-live="polite">
      <Skeleton variant="rectangular" height={112} />
      {Array.from({ length: rows }).map((_, index) => (
        <Skeleton key={index} variant="rectangular" height={64} />
      ))}
    </Stack>
  );
}
