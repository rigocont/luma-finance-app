import DragIndicatorIcon from '@mui/icons-material/DragIndicator';
import KeyboardArrowDownIcon from '@mui/icons-material/KeyboardArrowDown';
import KeyboardArrowUpIcon from '@mui/icons-material/KeyboardArrowUp';
import MoreVertIcon from '@mui/icons-material/MoreVert';
import Box from '@mui/material/Box';
import Card from '@mui/material/Card';
import Chip from '@mui/material/Chip';
import IconButton from '@mui/material/IconButton';
import LinearProgress from '@mui/material/LinearProgress';
import Menu from '@mui/material/Menu';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';
import { useEffect, useState, type DragEvent, type MouseEvent } from 'react';
import { useTranslation } from 'react-i18next';

import { formatDay } from '@/lib/date';
import { formatMoney, isPositive } from '@/lib/money';
import { testIds } from '@/lib/testids';

import {
  contributionModeLabel,
  goalStatusLabel,
  progressPercent,
  type SavingsGoal,
} from './types';

interface SavingsGoalListProps {
  goals: SavingsGoal[];
  onEdit: (goal: SavingsGoal) => void;
  onContribute: (goal: SavingsGoal) => void;
  onWithdraw: (goal: SavingsGoal) => void;
  onTogglePause: (goal: SavingsGoal) => void;
  onShowMovements: (goal: SavingsGoal) => void;
  onDelete: (goal: SavingsGoal) => void;
  /** Recibe el orden COMPLETO, de mayor a menor prioridad. */
  onReorder: (goalIds: string[]) => void;
}

/** Mueve un elemento de una posicion a otra sin mutar el arreglo original. */
function mover<T>(items: T[], desde: number, hasta: number): T[] {
  const elemento = items[desde];
  // Indice fuera de rango: se devuelve el arreglo tal cual. Con
  // noUncheckedIndexedAccess el compilador obliga a decidirlo, y reacomodar a
  // ciegas seria peor que no hacer nada.
  if (elemento === undefined) return items;

  const copia = [...items];
  copia.splice(desde, 1);
  copia.splice(hasta, 0, elemento);
  return copia;
}

/**
 * Las metas en orden de prioridad, reordenables.
 *
 * <p>El arrastre usa la API nativa de HTML5: cero dependencias. Pero el arrastre
 * nativo NO funciona en pantallas tactiles, asi que cada tarjeta lleva tambien
 * botones de subir y bajar. No son una alternativa de segunda: en telefono son
 * la unica forma de reordenar, y con teclado tambien.
 */
export function SavingsGoalList({
  goals,
  onEdit,
  onContribute,
  onWithdraw,
  onTogglePause,
  onShowMovements,
  onDelete,
  onReorder,
}: SavingsGoalListProps) {
  const { t } = useTranslation();
  // Copia local para que la lista se reacomode al instante mientras arrastras,
  // sin esperar la respuesta del servidor.
  const [orden, setOrden] = useState(goals);
  const [arrastrando, setArrastrando] = useState<number | null>(null);
  const [menuAnchor, setMenuAnchor] = useState<HTMLElement | null>(null);
  const [seleccionada, setSeleccionada] = useState<SavingsGoal | null>(null);

  useEffect(() => {
    setOrden(goals);
  }, [goals]);

  function aplicar(nuevo: SavingsGoal[]) {
    setOrden(nuevo);
    onReorder(nuevo.map((goal) => goal.id));
  }

  function moverA(desde: number, hasta: number) {
    if (hasta < 0 || hasta >= orden.length || desde === hasta) return;
    aplicar(mover(orden, desde, hasta));
  }

  function alSoltar(event: DragEvent<HTMLDivElement>, hasta: number) {
    event.preventDefault();
    if (arrastrando === null || arrastrando === hasta) return;
    aplicar(mover(orden, arrastrando, hasta));
    setArrastrando(null);
  }

  function abrirMenu(event: MouseEvent<HTMLElement>, goal: SavingsGoal) {
    setMenuAnchor(event.currentTarget);
    setSeleccionada(goal);
  }

  function cerrarMenu() {
    setMenuAnchor(null);
    setSeleccionada(null);
  }

  function ejecutar(accion: (goal: SavingsGoal) => void) {
    if (seleccionada) accion(seleccionada);
    cerrarMenu();
  }

  return (
    <>
      <Stack spacing={4} data-testid={testIds.savings.list}>
        {orden.map((goal, index) => {
          const porcentaje = progressPercent(goal);
          const pausada = goal.status === 'PAUSED';
          const alcanzada = goal.status === 'COMPLETED';

          // El aporte por ciclo solo esta guardado cuando la persona lo fijo.
          // Con fecha objetivo lo calcula el motor presupuestal en cada ciclo,
          // asi que aqui se dice PARA CUANDO, no un cero que no significa nada.
          const aporteFijo = goal.affectsBudget && isPositive(goal.plannedPerCycle);
          const conFecha = goal.affectsBudget && !aporteFijo && goal.targetDate !== null;

          return (
            <Card
              key={goal.id}
              variant="outlined"
              data-testid={testIds.savings.card(goal.id)}
              draggable
              onDragStart={() => setArrastrando(index)}
              onDragEnd={() => setArrastrando(null)}
              onDragOver={(event) => event.preventDefault()}
              onDrop={(event) => alSoltar(event, index)}
              sx={{
                p: 5,
                opacity: arrastrando === index ? 0.4 : 1,
                cursor: arrastrando === null ? 'default' : 'grabbing',
              }}
            >
              <Stack direction="row" spacing={3} sx={{ alignItems: 'flex-start' }}>
                <Box
                  aria-hidden
                  data-testid={testIds.savings.dragHandle}
                  sx={{ color: 'text.disabled', cursor: 'grab', pt: 1 }}
                >
                  <DragIndicatorIcon fontSize="small" />
                </Box>

                <Stack spacing={3} sx={{ flex: 1, minWidth: 0 }}>
                  <Stack
                    direction="row"
                    spacing={3}
                    sx={{ alignItems: 'flex-start', justifyContent: 'space-between' }}
                  >
                    <Stack spacing={1} sx={{ minWidth: 0 }}>
                      <Typography
                        variant="h4"
                        data-testid={testIds.savings.cardName}
                        sx={{ color: pausada ? 'text.disabled' : 'text.primary' }}
                      >
                        {goal.name}
                      </Typography>

                      <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap', gap: 1 }}>
                        {(alcanzada || pausada) && (
                          <Chip
                            size="small"
                            color={alcanzada ? 'success' : 'default'}
                            label={goalStatusLabel(goal.status)}
                            data-testid={testIds.savings.cardStatus}
                          />
                        )}
                        <Typography variant="caption" color="text.disabled">
                          {contributionModeLabel(goal.contributionMode)}
                        </Typography>
                      </Stack>
                    </Stack>

                    <Stack direction="row" spacing={1} sx={{ flexShrink: 0 }}>
                      <IconButton
                        size="small"
                        aria-label={t('savings.raisePriority', { name: goal.name })}
                        disabled={index === 0}
                        onClick={() => moverA(index, index - 1)}
                        data-testid={testIds.savings.moveUp}
                      >
                        <KeyboardArrowUpIcon fontSize="small" />
                      </IconButton>
                      <IconButton
                        size="small"
                        aria-label={t('savings.lowerPriority', { name: goal.name })}
                        disabled={index === orden.length - 1}
                        onClick={() => moverA(index, index + 1)}
                        data-testid={testIds.savings.moveDown}
                      >
                        <KeyboardArrowDownIcon fontSize="small" />
                      </IconButton>
                      <IconButton
                        size="small"
                        aria-label={t('savings.actionsFor', { name: goal.name })}
                        onClick={(event) => abrirMenu(event, goal)}
                        data-testid={testIds.savings.cardMenu}
                      >
                        <MoreVertIcon fontSize="small" />
                      </IconButton>
                    </Stack>
                  </Stack>

                  <Stack spacing={2}>
                    <Stack
                      direction="row"
                      spacing={3}
                      sx={{ justifyContent: 'space-between', alignItems: 'baseline' }}
                    >
                      <Typography
                        variant="body1"
                        data-testid={testIds.savings.cardSaved}
                        sx={{ fontVariantNumeric: 'tabular-nums' }}
                      >
                        {formatMoney(goal.saved)}
                        <Typography component="span" variant="body2" color="text.secondary">
                          {' '}
                          {t('savings.savedOf', { target: formatMoney(goal.target) })}
                        </Typography>
                      </Typography>

                      <Typography
                        variant="body2"
                        color="text.secondary"
                        data-testid={testIds.savings.cardProgress}
                        sx={{ fontVariantNumeric: 'tabular-nums' }}
                      >
                        {porcentaje}%
                      </Typography>
                    </Stack>

                    <LinearProgress
                      variant="determinate"
                      value={porcentaje}
                      data-testid={testIds.savings.cardProgressBar}
                      aria-label={t('savings.progressOf', { name: goal.name })}
                      sx={{ height: 8, borderRadius: 999 }}
                    />

                    <Stack direction="row" spacing={3} sx={{ justifyContent: 'space-between' }}>
                      <Typography
                        variant="caption"
                        color="text.secondary"
                        data-testid={testIds.savings.cardRemaining}
                      >
                        {t('savings.remaining', { amount: formatMoney(goal.remaining) })}
                      </Typography>

                      {(aporteFijo || conFecha) && (
                        <Typography
                          variant="caption"
                          color="text.secondary"
                          data-testid={testIds.savings.cardPerCycle}
                        >
                          {aporteFijo
                            ? t('savings.perCycle', { amount: formatMoney(goal.plannedPerCycle) })
                            : t('savings.targetOn', { date: formatDay(goal.targetDate as string) })}
                        </Typography>
                      )}
                    </Stack>
                  </Stack>
                </Stack>
              </Stack>
            </Card>
          );
        })}
      </Stack>

      <Menu anchorEl={menuAnchor} open={Boolean(menuAnchor)} onClose={cerrarMenu}>
        <MenuItem
          onClick={() => ejecutar(onContribute)}
          data-testid={testIds.savings.contributeAction}
        >
          {t('savings.menu.contribute')}
        </MenuItem>
        <MenuItem onClick={() => ejecutar(onWithdraw)} data-testid={testIds.savings.withdrawAction}>
          {t('savings.menu.withdraw')}
        </MenuItem>
        <MenuItem
          onClick={() => ejecutar(onShowMovements)}
          data-testid={testIds.savings.movementsAction}
        >
          {t('savings.menu.movements')}
        </MenuItem>
        <MenuItem onClick={() => ejecutar(onEdit)} data-testid={testIds.savings.editAction}>
          {t('savings.menu.edit')}
        </MenuItem>
        <MenuItem onClick={() => ejecutar(onTogglePause)} data-testid={testIds.savings.pauseAction}>
          {seleccionada?.status === 'PAUSED' ? t('savings.menu.resume') : t('savings.menu.pause')}
        </MenuItem>
        <MenuItem onClick={() => ejecutar(onDelete)} data-testid={testIds.savings.deleteAction}>
          {t('savings.menu.delete')}
        </MenuItem>
      </Menu>
    </>
  );
}
