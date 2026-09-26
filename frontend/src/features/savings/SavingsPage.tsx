import AddIcon from '@mui/icons-material/Add';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import { useState } from 'react';
import { useTranslation } from 'react-i18next';

import { PageHeader } from '@/components/layout/PageHeader';
import { ConfirmDialog } from '@/components/ui/ConfirmDialog';
import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { LoadingState } from '@/components/ui/LoadingState';
import { testIds } from '@/lib/testids';

import { SavingsGoalFormDrawer } from './SavingsGoalFormDrawer';
import { SavingsGoalList } from './SavingsGoalList';
import { SavingsMovementDialog } from './SavingsMovementDialog';
import { SavingsMovementsDrawer } from './SavingsMovementsDrawer';
import {
  GOAL_STATUSES,
  goalStatusLabel,
  type GoalStatus,
  type MovementPayload,
  type SavingsGoal,
  type SavingsGoalPayload,
} from './types';
import {
  useCreateGoal,
  useDeleteGoal,
  useGoalMovements,
  useRegisterMovement,
  useReorderGoals,
  useSavingsGoals,
  useSetGoalPaused,
  useUpdateGoal,
} from './useSavings';

type MovementKind = MovementPayload['type'];

export function SavingsPage() {
  const { t } = useTranslation();
  const [status, setStatus] = useState<GoalStatus | undefined>(undefined);
  const [enEdicion, setEnEdicion] = useState<SavingsGoal | null>(null);
  const [cajonAbierto, setCajonAbierto] = useState(false);
  const [movimientoDe, setMovimientoDe] = useState<SavingsGoal | null>(null);
  const [tipoMovimiento, setTipoMovimiento] = useState<MovementKind>('EXTRA');
  const [historialDe, setHistorialDe] = useState<SavingsGoal | null>(null);
  const [porEliminar, setPorEliminar] = useState<SavingsGoal | null>(null);

  const consulta = useSavingsGoals(status);
  const movimientos = useGoalMovements(historialDe?.id ?? null);
  const crear = useCreateGoal();
  const actualizar = useUpdateGoal();
  const registrar = useRegisterMovement();
  const reordenar = useReorderGoals();
  const pausar = useSetGoalPaused();
  const eliminar = useDeleteGoal();

  const guardando = crear.isPending || actualizar.isPending;

  function abrirNueva() {
    setEnEdicion(null);
    crear.reset();
    actualizar.reset();
    setCajonAbierto(true);
  }

  function abrirEdicion(goal: SavingsGoal) {
    setEnEdicion(goal);
    crear.reset();
    actualizar.reset();
    setCajonAbierto(true);
  }

  function abrirMovimiento(goal: SavingsGoal, kind: MovementKind) {
    registrar.reset();
    setTipoMovimiento(kind);
    setMovimientoDe(goal);
  }

  function guardar(payload: SavingsGoalPayload) {
    const mutacion = enEdicion
      ? actualizar.mutateAsync({ id: enEdicion.id, payload })
      : crear.mutateAsync(payload);

    // Solo se cierra si salio bien: si el backend rechazo algo, el cajon se
    // queda abierto con el error junto a su campo y sin perder lo capturado.
    void mutacion.then(() => setCajonAbierto(false)).catch(() => undefined);
  }

  function guardarMovimiento(payload: MovementPayload) {
    if (!movimientoDe) return;
    void registrar
      .mutateAsync({ goalId: movimientoDe.id, payload })
      .then(() => setMovimientoDe(null))
      .catch(() => undefined);
  }

  function confirmarEliminacion() {
    if (!porEliminar) return;
    void eliminar
      .mutateAsync({ id: porEliminar.id, name: porEliminar.name })
      .then(() => setPorEliminar(null))
      .catch(() => setPorEliminar(null));
  }

  const metas = consulta.data;
  const hayFiltro = status !== undefined;

  return (
    <Stack data-testid={testIds.savings.page}>
      <PageHeader
        eyebrow={t('savings.eyebrow')}
        title={t('savings.title')}
        description={t('savings.description')}
        action={
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={abrirNueva}
            data-testid={testIds.savings.createButton}
          >
            {t('savings.create')}
          </Button>
        }
      />

      <Stack direction="row" spacing={3} sx={{ pb: 6, flexWrap: 'wrap' }}>
        <TextField
          select
          size="small"
          label={t('savings.filters.status')}
          value={status ?? ''}
          onChange={(event) =>
            setStatus((event.target.value || undefined) as GoalStatus | undefined)
          }
          sx={{ minWidth: 180 }}
          data-testid={testIds.savings.filterStatus}
        >
          <MenuItem value="">{t('savings.filters.all')}</MenuItem>
          {GOAL_STATUSES.map((value) => (
            <MenuItem key={value} value={value}>
              {goalStatusLabel(value)}
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      {consulta.isPending && <LoadingState rows={3} />}

      {consulta.isError && <ErrorState error={consulta.error} onRetry={() => consulta.refetch()} />}

      {metas && metas.length === 0 && (
        <EmptyState
          title={hayFiltro ? t('savings.empty.filteredTitle') : t('savings.empty.title')}
          description={
            hayFiltro ? t('savings.empty.filteredDescription') : t('savings.empty.description')
          }
          action={
            hayFiltro ? (
              <Button onClick={() => setStatus(undefined)}>{t('savings.clearFilter')}</Button>
            ) : (
              <Button variant="contained" onClick={abrirNueva}>
                {t('savings.createFirst')}
              </Button>
            )
          }
        />
      )}

      {metas && metas.length > 0 && (
        <Stack spacing={4}>
          {metas.length > 1 && !hayFiltro && (
            <Alert severity="info">{t('savings.priorityNotice')}</Alert>
          )}

          <SavingsGoalList
            goals={metas}
            onEdit={abrirEdicion}
            onContribute={(goal) => abrirMovimiento(goal, 'EXTRA')}
            onWithdraw={(goal) => abrirMovimiento(goal, 'WITHDRAWAL')}
            onTogglePause={(goal) =>
              pausar.mutate({ id: goal.id, paused: goal.status !== 'PAUSED' })
            }
            onShowMovements={setHistorialDe}
            onDelete={setPorEliminar}
            onReorder={(goalIds) => reordenar.mutate(goalIds)}
          />
        </Stack>
      )}

      <SavingsGoalFormDrawer
        open={cajonAbierto}
        goal={enEdicion}
        pending={guardando}
        error={enEdicion ? actualizar.error : crear.error}
        onSubmit={guardar}
        onClose={() => setCajonAbierto(false)}
      />

      <SavingsMovementDialog
        open={movimientoDe !== null}
        goal={movimientoDe}
        kind={tipoMovimiento}
        pending={registrar.isPending}
        error={registrar.error}
        onSubmit={guardarMovimiento}
        onClose={() => setMovimientoDe(null)}
      />

      <SavingsMovementsDrawer
        open={historialDe !== null}
        goal={historialDe}
        movements={movimientos.data}
        loading={movimientos.isPending && historialDe !== null}
        error={movimientos.error}
        onRetry={() => movimientos.refetch()}
        onClose={() => setHistorialDe(null)}
      />

      <ConfirmDialog
        open={porEliminar !== null}
        title={t('savings.deleteConfirm.title', { name: porEliminar?.name ?? '' })}
        description={t('savings.deleteConfirm.description')}
        confirmLabel={t('common.delete')}
        destructive
        pending={eliminar.isPending}
        onConfirm={confirmarEliminacion}
        onCancel={() => setPorEliminar(null)}
      />
    </Stack>
  );
}
