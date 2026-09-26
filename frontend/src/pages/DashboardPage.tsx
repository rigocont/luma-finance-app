import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useState } from 'react';
import { useNavigate } from 'react-router';

import { PageHeader } from '@/components/layout/PageHeader';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { LoadingState } from '@/components/ui/LoadingState';
import { useCurrentUser } from '@/features/auth/useAuth';
import { BalanceHero } from '@/features/budget/BalanceHero';
import { CompositionCard } from '@/features/budget/CompositionCard';
import { CycleActions } from '@/features/budget/CycleActions';
import { DeficitAdviceCard } from '@/features/budget/DeficitAdviceCard';
import { TrendCard } from '@/features/budget/TrendCard';
import { UpcomingPayments } from '@/features/budget/UpcomingPayments';
import {
  isNoCycleYet,
  useCloseCycle,
  useCurrentSummary,
  useCycleItems,
  useDeficitAdvice,
  useOpenNextCycle,
  useSkipItem,
  useTrends,
} from '@/features/budget/useBudget';
import { formatDay } from '@/lib/date';
import { testIds } from '@/lib/testids';
import { paths } from '@/routes/paths';

/**
 * El resumen financiero.
 *
 * <p>Es la pantalla donde todo lo anterior se convierte en una respuesta. El
 * orden de la pagina es el orden de las preguntas: cuanto me queda, en que se
 * reparte, que sigue, y —solo si hace falta— de donde podria salir lo que falta.
 *
 * <p>Ninguna cifra se calcula aqui. Todas llegan del motor presupuestal, que es
 * el unico lugar donde se suma dinero en este producto.
 */
export function DashboardPage() {
  const user = useCurrentUser();
  const navigate = useNavigate();

  const resumen = useCurrentSummary();
  const abrir = useOpenNextCycle();
  const cerrar = useCloseCycle();

  const cycleId = resumen.data?.cycle.id;
  const balance = resumen.data?.balance;
  const enDeficit = balance?.state === 'DEFICIT';

  const renglones = useCycleItems(cycleId);
  const consejo = useDeficitAdvice(cycleId, enDeficit === true);
  const tendencias = useTrends();
  const quitar = useSkipItem(cycleId);

  const [quitando, setQuitando] = useState<string | null>(null);

  const sinCiclo = isNoCycleYet(resumen.error);

  return (
    <Stack data-testid={testIds.dashboard.page}>
      <PageHeader
        eyebrow="Tu dinero"
        title={user ? `Hola, ${user.name.split(' ')[0]}` : 'Hola'}
        description={resumen.data ? undefined : 'Aqui va a vivir el resumen de tu ciclo en curso.'}
        action={
          resumen.data && balance ? (
            <CycleActions
              balance={balance}
              closing={cerrar.isPending}
              onClose={() => cerrar.mutate(resumen.data.cycle.id)}
            />
          ) : undefined
        }
      />

      {resumen.isPending && <LoadingState rows={4} />}

      {sinCiclo && (
        <EmptyState
          title="Todavia no tienes un ciclo abierto"
          description="Un ciclo es la quincena o el mes que estas presupuestando. Al abrirlo, tus ingresos, gastos y metas entran como renglones y aparece tu balance."
          action={
            <Button
              variant="contained"
              onClick={() => abrir.mutate()}
              disabled={abrir.isPending}
              data-testid={testIds.dashboard.openCycleButton}
            >
              {abrir.isPending ? 'Abriendo...' : 'Abrir mi primer ciclo'}
            </Button>
          }
        />
      )}

      {resumen.isError && !sinCiclo && (
        <ErrorState error={resumen.error} onRetry={() => resumen.refetch()} />
      )}

      {resumen.data && balance && (
        <Stack spacing={8}>
          <Typography
            variant="body2"
            color="text.secondary"
            data-testid={testIds.dashboard.cycleRange}
          >
            Del {formatDay(resumen.data.cycle.period.start)} al{' '}
            {formatDay(resumen.data.cycle.period.end)}
          </Typography>

          <BalanceHero balance={balance} onReviewClick={() => navigate(paths.currentCycle)} />

          {enDeficit && (
            <DeficitAdviceCard
              advice={consejo.data}
              loading={consejo.isPending}
              pendingItemId={quitando}
              onSkip={(cut) => {
                setQuitando(cut.itemId);
                void quitar
                  .mutateAsync(cut.itemId)
                  .catch(() => undefined)
                  .finally(() => setQuitando(null));
              }}
            />
          )}

          <CompositionCard
            totals={balance.planned}
            savingsRate={balance.savingsRate}
            hasEstimates={balance.requiresReview}
          />

          <UpcomingPayments
            items={renglones.data ?? []}
            onSettle={() => navigate(paths.currentCycle)}
          />

          <TrendCard trends={tendencias.data} loading={tendencias.isPending} />
        </Stack>
      )}
    </Stack>
  );
}
