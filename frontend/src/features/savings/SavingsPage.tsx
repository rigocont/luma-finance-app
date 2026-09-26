import AddIcon from '@mui/icons-material/Add';
import Alert from '@mui/material/Alert';
import Button from '@mui/material/Button';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import { useState } from 'react';

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
  GOAL_STATUS_LABELS,
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
        eyebrow="Tu dinero"
        title="Ahorros"
        description="Tus metas en orden de prioridad: cuanto llevas y cuanto se aparta en cada ciclo."
        action={
          <Button
            variant="contained"
            startIcon={<AddIcon />}
            onClick={abrirNueva}
            data-testid={testIds.savings.createButton}
          >
            Crear meta
          </Button>
        }
      />

      <Stack direction="row" spacing={3} sx={{ pb: 6, flexWrap: 'wrap' }}>
        <TextField
          select
          size="small"
          label="Estado"
          value={status ?? ''}
          onChange={(event) =>
            setStatus((event.target.value || undefined) as GoalStatus | undefined)
          }
          sx={{ minWidth: 180 }}
          data-testid={testIds.savings.filterStatus}
        >
          <MenuItem value="">Todas</MenuItem>
          {GOAL_STATUSES.map((value) => (
            <MenuItem key={value} value={value}>
              {GOAL_STATUS_LABELS[value]}
            </MenuItem>
          ))}
        </TextField>
      </Stack>

      {consulta.isPending && <LoadingState rows={3} />}

      {consulta.isError && <ErrorState error={consulta.error} onRetry={() => consulta.refetch()} />}

      {metas && metas.length === 0 && (
        <EmptyState
          title={hayFiltro ? 'Nada con ese estado' : 'Todavia no tienes ninguna meta'}
          description={
            hayFiltro
              ? 'Prueba con otro estado para ver el resto.'
              : 'Empieza por un fondo de emergencia. Si la meta resta de tu presupuesto, su aporte entra a tu ciclo en curso de inmediato.'
          }
          action={
            hayFiltro ? (
              <Button onClick={() => setStatus(undefined)}>Quitar el filtro</Button>
            ) : (
              <Button variant="contained" onClick={abrirNueva}>
                Crear la primera
              </Button>
            )
          }
        />
      )}

      {metas && metas.length > 0 && (
        <Stack spacing={4}>
          {metas.length > 1 && !hayFiltro && (
            <Alert severity="info">
              El orden es la prioridad: cuando sobre dinero en un ciclo, se reparte de arriba hacia
              abajo. Arrastra una tarjeta o usa las flechas para cambiarlo.
            </Alert>
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
        title={`Eliminar "${porEliminar?.name ?? ''}"`}
        description={
          'La meta desaparece de tus listas y deja de restar de tus ciclos. Lo ' +
          'que ya habias apartado se conserva en el historial, pero la meta no ' +
          'se puede recuperar. Si solo quieres detenerla un rato, usa "Pausar".'
        }
        confirmLabel="Eliminar"
        destructive
        pending={eliminar.isPending}
        onConfirm={confirmarEliminacion}
        onCancel={() => setPorEliminar(null)}
      />
    </Stack>
  );
}
