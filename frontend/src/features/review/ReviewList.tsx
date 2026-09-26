import HistoryIcon from '@mui/icons-material/History';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Tooltip from '@mui/material/Tooltip';
import Typography from '@mui/material/Typography';

import { formatDay } from '@/lib/date';
import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';

import type { ReviewItem } from './types';

interface ReviewListProps {
  reviews: ReviewItem[];
  /** Lo capturado por renglon, indexado por id. */
  amounts: Record<string, string>;
  fieldErrors: Record<string, string>;
  pending: boolean;
  onAmountChange: (itemId: string, amount: string) => void;
  onShowHistory: (review: ReviewItem) => void;
}

/**
 * Los gastos variables del ciclo, cada uno con su campo de monto.
 *
 * <p>Se captura TODO y se confirma de una vez, en lugar de un boton por
 * renglon: revisar la quincena es una sola sentada, y guardar seis veces para
 * seis recibos es seis veces la espera y seis avisos.
 *
 * <p>El campo llega con un monto propuesto ya escrito —lo que costo la vez
 * pasada— porque casi siempre es el correcto o esta cerca. Se ve de donde sale,
 * para que aceptarlo sea una decision y no un descuido.
 */
export function ReviewList({
  reviews,
  amounts,
  fieldErrors,
  pending,
  onAmountChange,
  onShowHistory,
}: ReviewListProps) {
  return (
    <Stack spacing={3} data-testid={testIds.review.list}>
      {reviews.map((review) => {
        const item = review.item;
        const sugerencia = review.suggestedAmount;
        const capturado = amounts[item.id] ?? '';
        const yaEsLaSugerencia = sugerencia !== null && capturado === sugerencia.amount;

        return (
          <Card
            key={item.id}
            variant="outlined"
            data-testid={testIds.review.row(item.id)}
            sx={{ p: 5 }}
          >
            <Stack
              direction={{ xs: 'column', sm: 'row' }}
              spacing={4}
              sx={{ alignItems: { xs: 'stretch', sm: 'flex-start' } }}
            >
              <Stack spacing={1} sx={{ flex: 1, minWidth: 0 }}>
                <Typography variant="h4" data-testid={testIds.review.rowName}>
                  {item.name}
                </Typography>

                <Typography
                  variant="body2"
                  color="text.secondary"
                  data-testid={testIds.review.rowPlanned}
                >
                  Estimaste {formatMoney(item.plannedAmount)}
                  {item.dueDate !== null && ` · vence el ${formatDay(item.dueDate)}`}
                </Typography>

                {sugerencia !== null && review.suggestedFromStart !== null && (
                  <Stack
                    direction="row"
                    spacing={2}
                    sx={{ alignItems: 'center', flexWrap: 'wrap', gap: 1, pt: 1 }}
                  >
                    <Typography
                      variant="caption"
                      color="text.secondary"
                      data-testid={testIds.review.rowSuggestion}
                    >
                      El ciclo del {formatDay(review.suggestedFromStart)} fueron{' '}
                      {formatMoney(sugerencia)}
                    </Typography>

                    {!yaEsLaSugerencia && (
                      <Button
                        size="small"
                        onClick={() => onAmountChange(item.id, sugerencia.amount)}
                        disabled={pending}
                        data-testid={testIds.review.rowUseSuggestion}
                      >
                        Usar ese monto
                      </Button>
                    )}
                  </Stack>
                )}
              </Stack>

              <Stack direction="row" spacing={2} sx={{ alignItems: 'flex-start' }}>
                <TextField
                  size="small"
                  label="Cuanto fue"
                  value={capturado}
                  onChange={(event) => onAmountChange(item.id, event.target.value)}
                  error={Boolean(fieldErrors[item.id])}
                  helperText={fieldErrors[item.id]}
                  disabled={pending}
                  sx={{ width: { xs: '100%', sm: 160 } }}
                  slotProps={{
                    htmlInput: {
                      'data-testid': testIds.review.rowAmountInput,
                      inputMode: 'decimal',
                      pattern: '\\d{1,13}(\\.\\d{1,2})?',
                      'aria-label': `Monto de ${item.name}`,
                    },
                  }}
                />

                <Box sx={{ pt: 1 }}>
                  <Tooltip title="Ver los ciclos anteriores">
                    <span>
                      <IconButton
                        size="small"
                        aria-label={`Historial de ${item.name}`}
                        onClick={() => onShowHistory(review)}
                        data-testid={testIds.review.rowHistory}
                      >
                        <HistoryIcon fontSize="small" />
                      </IconButton>
                    </span>
                  </Tooltip>
                </Box>
              </Stack>
            </Stack>
          </Card>
        );
      })}
    </Stack>
  );
}
