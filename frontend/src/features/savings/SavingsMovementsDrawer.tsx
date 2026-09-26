import Box from '@mui/material/Box';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import Drawer from '@mui/material/Drawer';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { LoadingState } from '@/components/ui/LoadingState';
import { formatDay } from '@/lib/date';
import { formatMoney, isNegative } from '@/lib/money';
import { testIds } from '@/lib/testids';

import { MOVEMENT_TYPE_LABELS, type SavingsGoal, type SavingsMovement } from './types';

interface SavingsMovementsDrawerProps {
  open: boolean;
  goal: SavingsGoal | null;
  movements: SavingsMovement[] | undefined;
  loading: boolean;
  error: unknown;
  onRetry: () => void;
  onClose: () => void;
}

/**
 * El historial de una meta: de donde salio cada peso.
 *
 * <p>Los aportes que nacen del ciclo se marcan aparte de los sueltos. Sin esa
 * distincion, un aporte que entro solo se confundiria con uno registrado a
 * mano, y son cosas distintas cuando algo no cuadra.
 */
export function SavingsMovementsDrawer({
  open,
  goal,
  movements,
  loading,
  error,
  onRetry,
  onClose,
}: SavingsMovementsDrawerProps) {
  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={onClose}
      data-testid={testIds.savings.movementsDrawer}
      slotProps={{ paper: { sx: { width: { xs: '100%', sm: 440 }, maxWidth: '100%' } } }}
    >
      <Box sx={{ p: 6 }}>
        <Stack spacing={5}>
          <Stack spacing={1}>
            <Typography variant="overline" color="text.disabled">
              Movimientos
            </Typography>
            <Typography variant="h3">{goal?.name ?? 'Meta'}</Typography>
            {goal && (
              <Typography variant="body2" color="text.secondary">
                Llevas {formatMoney(goal.saved)} de {formatMoney(goal.target)}.
              </Typography>
            )}
          </Stack>

          {loading && <LoadingState rows={3} />}

          {Boolean(error) && <ErrorState error={error} onRetry={onRetry} />}

          {movements && movements.length === 0 && (
            <Box data-testid={testIds.savings.movementsEmpty}>
              <EmptyState
                title="Todavia no hay movimientos"
                description="Aqui van a aparecer los aportes que confirmes en tus ciclos y los que registres por tu cuenta."
              />
            </Box>
          )}

          {movements && movements.length > 0 && (
            <Stack divider={<Divider flexItem />}>
              {movements.map((movement) => {
                const salida = isNegative(movement.amount);

                return (
                  <Stack
                    key={movement.id}
                    spacing={1}
                    data-testid={testIds.savings.movementRow}
                    sx={{ py: 3 }}
                  >
                    <Stack
                      direction="row"
                      spacing={3}
                      sx={{ justifyContent: 'space-between', alignItems: 'baseline' }}
                    >
                      <Typography variant="body2" color="text.secondary">
                        {formatDay(movement.date)}
                      </Typography>
                      <Typography
                        variant="body1"
                        sx={{
                          fontVariantNumeric: 'tabular-nums',
                          color: salida ? 'warning.main' : 'text.primary',
                        }}
                      >
                        {formatMoney(movement.amount)}
                      </Typography>
                    </Stack>

                    <Stack
                      direction="row"
                      spacing={2}
                      sx={{ alignItems: 'center', flexWrap: 'wrap', gap: 1 }}
                    >
                      <Chip
                        size="small"
                        variant="outlined"
                        label={MOVEMENT_TYPE_LABELS[movement.type]}
                      />
                      {movement.fromCycle && (
                        <Typography variant="caption" color="text.disabled">
                          Entro al confirmar el ciclo
                        </Typography>
                      )}
                    </Stack>

                    {movement.notes && (
                      <Typography variant="body2" color="text.secondary">
                        {movement.notes}
                      </Typography>
                    )}
                  </Stack>
                );
              })}
            </Stack>
          )}
        </Stack>
      </Box>
    </Drawer>
  );
}
