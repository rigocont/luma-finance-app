import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useQuery } from '@tanstack/react-query';

import { ErrorState } from '@/components/ui/ErrorState';
import { LoadingState } from '@/components/ui/LoadingState';
import { fetchSystemInfo, systemKeys } from '@/lib/api/system';
import { testIds } from '@/lib/testids';

/**
 * Estado de la conexion con la API.
 *
 * <p>Vivia en el resumen desde la Fase 1.5, cuando la unica pregunta interesante
 * era si la pila respondia de punta a punta. Ahora el resumen responde una
 * pregunta de dinero y este diagnostico no tiene por que competir con ella, pero
 * sigue siendo util cuando algo no carga: dice si el problema es la aplicacion o
 * la conexion.
 */
export function SystemStatusCard() {
  const { data, isPending, isError, error, refetch } = useQuery({
    queryKey: systemKeys.info,
    queryFn: fetchSystemInfo,
  });

  return (
    <Card data-testid={testIds.settings.systemCard}>
      <CardContent>
        <Stack spacing={4}>
          <Stack
            direction="row"
            spacing={3}
            useFlexGap
            sx={{ alignItems: 'center', justifyContent: 'space-between', flexWrap: 'wrap' }}
          >
            <Typography variant="h3">Estado del sistema</Typography>
            {data && (
              <Chip
                label="Respondiendo"
                color="success"
                variant="outlined"
                size="small"
                data-testid={testIds.settings.systemStatus}
              />
            )}
          </Stack>

          {isPending && <LoadingState rows={1} />}

          {isError && <ErrorState error={error} onRetry={() => refetch()} />}

          {data && (
            <Stack direction="row" spacing={8} useFlexGap sx={{ flexWrap: 'wrap', rowGap: 3 }}>
              <Campo label="Version" value={data.version} testId={testIds.settings.systemVersion} />
              <Campo
                label="Perfil"
                value={data.profiles.length > 0 ? data.profiles.join(', ') : 'default'}
              />
              <Campo
                label="Hora del servidor"
                value={new Date(data.serverTime).toLocaleString('es-MX')}
              />
            </Stack>
          )}
        </Stack>
      </CardContent>
    </Card>
  );
}

function Campo({ label, value, testId }: { label: string; value: string; testId?: string }) {
  return (
    <Stack spacing={0.5} sx={{ minWidth: 0 }}>
      <Typography variant="caption" color="text.disabled">
        {label}
      </Typography>
      <Typography variant="body2" data-testid={testId}>
        {value}
      </Typography>
    </Stack>
  );
}
