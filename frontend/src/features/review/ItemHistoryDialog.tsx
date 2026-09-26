import Dialog from '@mui/material/Dialog';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Divider from '@mui/material/Divider';
import Stack from '@mui/material/Stack';
import Typography from '@mui/material/Typography';

import { EmptyState } from '@/components/ui/EmptyState';
import { ErrorState } from '@/components/ui/ErrorState';
import { LoadingState } from '@/components/ui/LoadingState';
import { formatDay } from '@/lib/date';
import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';

import type { ItemHistory } from './types';

interface ItemHistoryDialogProps {
  open: boolean;
  name: string;
  history: ItemHistory | undefined;
  loading: boolean;
  error: unknown;
  onRetry: () => void;
  onClose: () => void;
}

/**
 * Lo que costo este gasto en los ciclos anteriores.
 *
 * <p>Se muestran los dos montos, el estimado y el real: la diferencia entre
 * ellos es justo lo que revela que un recibo viene subiendo, y mirar solo el
 * real no lo dice.
 *
 * <p>El promedio llega calculado del servidor. Es una cifra de dinero, y aqui
 * no se deriva ninguna: dos formas de redondear el mismo numero es como empiezan
 * las cifras que no cuadran.
 */
export function ItemHistoryDialog({
  open,
  name,
  history,
  loading,
  error,
  onRetry,
  onClose,
}: ItemHistoryDialogProps) {
  return (
    <Dialog
      open={open}
      onClose={onClose}
      fullWidth
      maxWidth="xs"
      aria-labelledby="review-history-title"
      data-testid={testIds.review.historyDialog}
    >
      <DialogTitle id="review-history-title">{name}</DialogTitle>

      <DialogContent>
        <Stack spacing={4} sx={{ pb: 3 }}>
          {loading && <LoadingState rows={3} />}

          {Boolean(error) && <ErrorState error={error} onRetry={onRetry} />}

          {history && history.entries.length === 0 && (
            <EmptyState
              title="Es la primera vez"
              description="Cuando confirmes este gasto un par de ciclos, aqui vas a poder ver como se mueve."
            />
          )}

          {history && history.entries.length > 0 && (
            <>
              {history.average !== null && (
                <Typography
                  variant="body2"
                  color="text.secondary"
                  data-testid={testIds.review.historyAverage}
                >
                  En los ultimos {history.cycles} {history.cycles === 1 ? 'ciclo' : 'ciclos'} te
                  costo {formatMoney(history.average)} en promedio.
                </Typography>
              )}

              <Stack divider={<Divider flexItem />}>
                {history.entries.map((entry) => (
                  <Stack
                    key={entry.cycleId}
                    spacing={0.5}
                    data-testid={testIds.review.historyRow}
                    sx={{ py: 3 }}
                  >
                    <Stack
                      direction="row"
                      spacing={3}
                      sx={{ justifyContent: 'space-between', alignItems: 'baseline' }}
                    >
                      <Typography variant="body2" color="text.secondary">
                        {formatDay(entry.cycleStart)}
                      </Typography>
                      <Typography
                        variant="body1"
                        sx={{ fontVariantNumeric: 'tabular-nums', fontWeight: 600 }}
                      >
                        {formatMoney(entry.actualAmount)}
                      </Typography>
                    </Stack>

                    <Typography variant="caption" color="text.disabled">
                      Habias estimado {formatMoney(entry.plannedAmount)}
                    </Typography>
                  </Stack>
                ))}
              </Stack>
            </>
          )}
        </Stack>
      </DialogContent>
    </Dialog>
  );
}
