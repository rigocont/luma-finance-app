import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useQuery } from '@tanstack/react-query';
import { useTranslation } from 'react-i18next';

import { ErrorState } from '@/components/ui/ErrorState';
import { LoadingState } from '@/components/ui/LoadingState';
import { fetchSystemInfo, systemKeys } from '@/lib/api/system';
import { intlLocaleFor } from '@/lib/money';
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
  const { t, i18n } = useTranslation();
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
            <Typography variant="h3">{t('settings.system.title')}</Typography>
            {data && (
              <Chip
                label={t('settings.system.responding')}
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
              <Campo
                label={t('settings.system.version')}
                value={data.version}
                testId={testIds.settings.systemVersion}
              />
              <Campo
                label={t('settings.system.profile')}
                value={data.profiles.length > 0 ? data.profiles.join(', ') : t('settings.system.defaultProfile')}
              />
              <Campo
                label={t('settings.system.serverTime')}
                value={new Date(data.serverTime).toLocaleString(intlLocaleFor(i18n.language))}
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
