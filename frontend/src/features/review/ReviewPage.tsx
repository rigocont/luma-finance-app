import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';
import { useNavigate } from 'react-router';

import { PageHeader } from '@/components/layout/PageHeader';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { LoadingState } from '@/components/ui/LoadingState';
import { ApiError } from '@/lib/api/types';
import { formatDay } from '@/lib/date';
import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';
import { paths } from '@/routes/paths';

import { ItemHistoryDialog } from './ItemHistoryDialog';
import { ReviewList } from './ReviewList';
import { SettleSavingDialog } from './SettleSavingDialog';
import { proposedAmount, type CycleItem, type ReviewItem, type SettlePayload } from './types';
import {
  isNoCycleYet,
  useConfirmAmounts,
  useCurrentCycle,
  useItemHistory,
  usePendingReview,
  useSavingItems,
  useSettleItem,
} from './useReview';

const MONTO_VALIDO = /^\d{1,13}(\.\d{1,2})?$/;

export function ReviewPage() {
  const { t } = useTranslation();
  const navigate = useNavigate();

  const ciclo = useCurrentCycle();
  const revision = usePendingReview();
  const ahorros = useSavingItems(ciclo.data?.id);
  const confirmar = useConfirmAmounts(ciclo.data?.id);
  const registrar = useSettleItem(ciclo.data?.id);

  const [montos, setMontos] = useState<Record<string, string>>({});
  const [errores, setErrores] = useState<Record<string, string>>({});
  const [historialDe, setHistorialDe] = useState<ReviewItem | null>(null);
  const [porRegistrar, setPorRegistrar] = useState<CycleItem | null>(null);

  const historial = useItemHistory(ciclo.data?.id, historialDe?.item.id ?? null);

  // El campo arranca con el monto propuesto. Se rehace cuando cambia la lista
  // —al confirmar, al abrir otro ciclo— y no en cada render, para no pisar lo
  // que la persona este escribiendo.
  useEffect(() => {
    if (!revision.data) return;

    setMontos(
      Object.fromEntries(revision.data.map((review) => [review.item.id, proposedAmount(review)])),
    );
    setErrores({});
  }, [revision.data]);

  function cambiarMonto(itemId: string, amount: string) {
    setMontos((previos) => ({ ...previos, [itemId]: amount }));
    setErrores((previos) => {
      if (!previos[itemId]) return previos;
      const { [itemId]: _quitado, ...resto } = previos;
      return resto;
    });
  }

  function confirmarTodo() {
    const pendientes = revision.data ?? [];
    const nuevosErrores: Record<string, string> = {};

    for (const review of pendientes) {
      const valor = (montos[review.item.id] ?? '').trim();

      // Se valida antes de mandar para no gastar una ida al servidor en algo
      // que el navegador ya sabe. El servidor lo valida igual: esto es
      // comodidad, no la barrera.
      if (!MONTO_VALIDO.test(valor)) {
        nuevosErrores[review.item.id] = t('review.invalidAmount');
      }
    }

    if (Object.keys(nuevosErrores).length > 0) {
      setErrores(nuevosErrores);
      return;
    }

    void confirmar
      .mutateAsync({
        items: pendientes.map((review) => ({
          itemId: review.item.id,
          amount: (montos[review.item.id] ?? '').trim(),
        })),
      })
      .catch(() => undefined);
  }

  function registrarAporte(payload: SettlePayload) {
    if (!porRegistrar) return;
    void registrar
      .mutateAsync({ itemId: porRegistrar.id, payload })
      .then(() => setPorRegistrar(null))
      .catch(() => undefined);
  }

  const sinCiclo = isNoCycleYet(ciclo.error);
  const pendientes = revision.data ?? [];
  const ahorrosPorRegistrar = (ahorros.data ?? []).filter(
    (item) => item.status === 'PENDING' || item.status === 'OVERDUE',
  );

  const errorDelLote = confirmar.error instanceof ApiError ? confirmar.error : null;

  return (
    <Stack data-testid={testIds.review.page}>
      <PageHeader
        eyebrow={t('review.eyebrow')}
        title={t('review.title')}
        description={t('review.description')}
      />

      {ciclo.isPending && <LoadingState rows={3} />}

      {sinCiclo && (
        <EmptyState
          title={t('review.noCycle.title')}
          description={t('review.noCycle.description')}
          action={
            <Button variant="contained" onClick={() => navigate(paths.expenses)}>
              {t('review.noCycle.startWithExpenses')}
            </Button>
          }
        />
      )}

      {ciclo.isError && !sinCiclo && (
        <ErrorState error={ciclo.error} onRetry={() => ciclo.refetch()} />
      )}

      {ciclo.data && (
        <Stack spacing={6}>
          <Typography
            variant="body2"
            color="text.secondary"
            data-testid={testIds.review.cycleRange}
          >
            {t('review.cycleRange', {
              start: formatDay(ciclo.data.period.start),
              end: formatDay(ciclo.data.period.end),
            })}
          </Typography>

          {revision.isPending && <LoadingState rows={3} />}

          {revision.isError && !isNoCycleYet(revision.error) && (
            <ErrorState error={revision.error} onRetry={() => revision.refetch()} />
          )}

          {revision.data && pendientes.length === 0 && (
            <EmptyState
              title={t('review.noneToReview.title')}
              description={t('review.noneToReview.description')}
            />
          )}

          {pendientes.length > 0 && (
            <Stack spacing={4}>
              <Alert severity="info">{t('review.banner')}</Alert>

              {errorDelLote && (
                <Alert severity="error" data-testid={testIds.review.formError}>
                  {errorDelLote.message}
                </Alert>
              )}

              <ReviewList
                reviews={pendientes}
                amounts={montos}
                fieldErrors={errores}
                pending={confirmar.isPending}
                onAmountChange={cambiarMonto}
                onShowHistory={setHistorialDe}
              />

              <Stack
                direction="row"
                spacing={3}
                sx={{ justifyContent: 'flex-end', alignItems: 'center' }}
              >
                <Typography
                  variant="body2"
                  color="text.secondary"
                  data-testid={testIds.review.confirmedCount}
                >
                  {pendientes.length}{' '}
                  {pendientes.length === 1
                    ? t('review.pendingCountOne')
                    : t('review.pendingCountMany')}
                </Typography>
                <Button
                  variant="contained"
                  onClick={confirmarTodo}
                  disabled={confirmar.isPending}
                  data-testid={testIds.review.confirmAllButton}
                >
                  {confirmar.isPending ? t('common.oneMoment') : t('review.confirmAll')}
                </Button>
              </Stack>
            </Stack>
          )}

          {ahorrosPorRegistrar.length > 0 && (
            <Stack spacing={4} data-testid={testIds.review.savingsSection}>
              <Divider />

              <Stack spacing={1}>
                <Typography variant="h3">{t('review.savingsSection.title')}</Typography>
                <Typography variant="body2" color="text.secondary">
                  {t('review.savingsSection.description')}
                </Typography>
              </Stack>

              <Stack spacing={3}>
                {ahorrosPorRegistrar.map((item) => (
                  <Card
                    key={item.id}
                    variant="outlined"
                    data-testid={testIds.review.savingsRow(item.id)}
                    sx={{ p: 5 }}
                  >
                    <Stack
                      direction="row"
                      spacing={3}
                      sx={{ alignItems: 'center', justifyContent: 'space-between' }}
                    >
                      <Stack spacing={1} sx={{ minWidth: 0 }}>
                        <Typography variant="h4">{item.name}</Typography>
                        <Stack direction="row" spacing={2} sx={{ alignItems: 'center' }}>
                          <Typography
                            variant="body2"
                            color="text.secondary"
                            sx={{ fontVariantNumeric: 'tabular-nums' }}
                          >
                            {formatMoney(item.plannedAmount)}
                          </Typography>
                          {item.status === 'OVERDUE' && (
                            <Chip
                              size="small"
                              color="warning"
                              label={t('review.savingsSection.overdueChip')}
                            />
                          )}
                        </Stack>
                      </Stack>

                      <Button
                        onClick={() => {
                          registrar.reset();
                          setPorRegistrar(item);
                        }}
                        data-testid={testIds.review.savingsSettleAction}
                      >
                        {t('review.savingsSection.settle')}
                      </Button>
                    </Stack>
                  </Card>
                ))}
              </Stack>
            </Stack>
          )}
        </Stack>
      )}

      <ItemHistoryDialog
        open={historialDe !== null}
        name={historialDe?.item.name ?? ''}
        history={historial.data}
        loading={historial.isPending && historialDe !== null}
        error={historial.error}
        onRetry={() => historial.refetch()}
        onClose={() => setHistorialDe(null)}
      />

      <SettleSavingDialog
        open={porRegistrar !== null}
        item={porRegistrar}
        pending={registrar.isPending}
        error={registrar.error}
        onSubmit={registrarAporte}
        onClose={() => setPorRegistrar(null)}
      />
    </Stack>
  );
}
