import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import Chip from '@mui/material/Chip';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useQuery } from '@tanstack/react-query';

import { PageHeader } from '@/components/layout/PageHeader';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { LoadingState } from '@/components/ui/LoadingState';
import { useCurrentUser } from '@/features/auth/useAuth';
import { fetchSystemInfo, systemKeys } from '@/lib/api/system';
import { testIds } from '@/lib/testids';

/**
 * Dashboard de la Fase 1.
 *
 * Todavia no calcula nada: su trabajo aqui es probar que la pila completa
 * responde (navegador -> Vite -> Spring Boot -> MySQL). El resumen financiero
 * real llega en la Fase 10, sobre el motor presupuestal de la Fase 4.
 */
export function DashboardPage() {
  const user = useCurrentUser();
  const { data, isLoading, isError, error, refetch } = useQuery({
    queryKey: systemKeys.info,
    queryFn: fetchSystemInfo,
  });

  return (
    <div data-testid={testIds.dashboard.page}>
      <PageHeader
        eyebrow="Fase 1.5 · Walking skeleton"
        title={user ? `Hola, ${user.name}` : 'Hola'}
        description="Tu sesion ya funciona de punta a punta. Falta el motor presupuestal, que es lo que va a llenar esta pantalla."
      />

      {user && (
        <Typography
          variant="body2"
          color="text.secondary"
          data-testid={testIds.dashboard.greeting}
          sx={{ mb: 6 }}
        >
          Sesion iniciada como {user.email}
        </Typography>
      )}

      {isLoading && <LoadingState rows={2} />}

      {isError && <ErrorState error={error} onRetry={() => void refetch()} />}

      {data && (
        <Stack spacing={6}>
          <Card data-testid={testIds.dashboard.connectionCard}>
            <CardContent>
              <Stack spacing={3}>
                <Stack
                  direction="row"
                  spacing={3}
                  useFlexGap
                  sx={{
                    alignItems: 'center',
                    justifyContent: 'space-between',
                    flexWrap: 'wrap',
                  }}
                >
                  <Typography variant="overline" color="text.disabled">
                    Conexion con la API
                  </Typography>
                  <Chip
                    label="Respondiendo"
                    color="success"
                    variant="outlined"
                    size="small"
                    data-testid={testIds.dashboard.connectionStatus}
                  />
                </Stack>

                <Typography variant="h3">{data.name}</Typography>

                <Stack direction="row" spacing={8} useFlexGap sx={{ flexWrap: 'wrap' }}>
                  <Field label="Version" value={data.version} testId={testIds.dashboard.apiVersion} />
                  <Field
                    label="Perfil"
                    value={data.profiles.length > 0 ? data.profiles.join(', ') : 'default'}
                  />
                  <Field
                    label="Hora del servidor"
                    value={new Date(data.serverTime).toLocaleString('es-MX')}
                  />
                </Stack>
              </Stack>
            </CardContent>
          </Card>

          <Card>
            <EmptyState
              title="Aqui va tu balance"
              description="Cuando registres tu ciclo, tus ingresos y tus gastos, esta pantalla te dira cuanto te queda disponible."
            />
          </Card>
        </Stack>
      )}
    </div>
  );
}

function Field({ label, value, testId }: { label: string; value: string; testId?: string }) {
  return (
    <Stack spacing={0.5} sx={{ minWidth: 0 }}>
      <Typography variant="caption" color="text.disabled">
        {label}
      </Typography>
      <Typography variant="body2" className="luma-tabular" data-testid={testId}>
        {value}
      </Typography>
    </Stack>
  );
}
