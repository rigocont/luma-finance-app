import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Checkbox from '@mui/material/Checkbox';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import FormControlLabel from '@mui/material/FormControlLabel';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useEffect, useState, type FormEvent } from 'react';

import { ApiError } from '@/lib/api/types';
import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';

import type { CycleItem, SettlePayload } from './types';

interface SettleSavingDialogProps {
  open: boolean;
  item: CycleItem | null;
  pending: boolean;
  error: unknown;
  onSubmit: (payload: SettlePayload) => void;
  onClose: () => void;
}

function hoy(): string {
  return new Date().toISOString().slice(0, 10);
}

/**
 * Registra el aporte de ahorro que este ciclo tenia planeado.
 *
 * <p>El monto llega propuesto con lo planeado y la casilla de la meta viene
 * marcada: apartar el dinero y que cuente en la meta son la misma accion casi
 * siempre. La casilla existe para el "casi": a veces el dinero sale del
 * presupuesto y no llega al ahorro, y forzar que cuente convertiria la meta en
 * una cifra que no corresponde a nada.
 */
export function SettleSavingDialog({
  open,
  item,
  pending,
  error,
  onSubmit,
  onClose,
}: SettleSavingDialogProps) {
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState(hoy());
  const [skipGoal, setSkipGoal] = useState(false);

  useEffect(() => {
    if (!open || !item) return;
    setAmount(item.plannedAmount.amount);
    setDate(hoy());
    setSkipGoal(false);
  }, [open, item]);

  const apiError = error instanceof ApiError ? error : null;
  const fieldErrors = apiError?.fieldErrorMap ?? {};
  const generalMessage = apiError && apiError.fieldErrors.length === 0 ? apiError.message : null;

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit({
      actualAmount: amount.trim(),
      settledOn: date || null,
      registerInGoal: !skipGoal,
    });
  }

  return (
    <Dialog
      open={open}
      onClose={pending ? undefined : onClose}
      fullWidth
      maxWidth="xs"
      aria-labelledby="review-settle-title"
      data-testid={testIds.review.settleDialog}
    >
      <Box component="form" onSubmit={handleSubmit} noValidate>
        <DialogTitle id="review-settle-title">Registrar el aporte</DialogTitle>

        <DialogContent>
          <Stack spacing={5} sx={{ pt: 2 }}>
            {item && (
              <Typography variant="body2" color="text.secondary">
                {item.name}: este ciclo planeaste apartar {formatMoney(item.plannedAmount)}.
              </Typography>
            )}

            {generalMessage && (
              <Alert severity="error" data-testid={testIds.review.settleError}>
                {generalMessage}
              </Alert>
            )}

            <TextField
              label="Cuanto apartaste"
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
              error={Boolean(fieldErrors.actualAmount)}
              helperText={fieldErrors.actualAmount ?? 'Si fue menos, el renglon queda parcial.'}
              disabled={pending}
              fullWidth
              autoFocus
              slotProps={{
                htmlInput: {
                  'data-testid': testIds.review.settleAmount,
                  inputMode: 'decimal',
                  pattern: '\\d{1,13}(\\.\\d{1,2})?',
                },
              }}
            />

            <TextField
              label="Cuando"
              type="date"
              value={date}
              onChange={(event) => setDate(event.target.value)}
              error={Boolean(fieldErrors.settledOn)}
              helperText={fieldErrors.settledOn}
              disabled={pending}
              fullWidth
              slotProps={{
                inputLabel: { shrink: true },
                htmlInput: { 'data-testid': testIds.review.settleDate },
              }}
            />

            <FormControlLabel
              control={
                <Checkbox
                  checked={skipGoal}
                  onChange={(event) => setSkipGoal(event.target.checked)}
                  disabled={pending}
                  data-testid={testIds.review.settleSkipGoal}
                />
              }
              label={
                <Stack spacing={0.5}>
                  <Typography variant="body2">No registrarlo en la meta</Typography>
                  <Typography variant="caption" color="text.disabled">
                    El gasto queda en el ciclo, pero el progreso de la meta no se mueve.
                  </Typography>
                </Stack>
              }
              sx={{ alignItems: 'flex-start', m: 0 }}
            />
          </Stack>
        </DialogContent>

        <DialogActions>
          <Button onClick={onClose} disabled={pending}>
            Cancelar
          </Button>
          <Button
            type="submit"
            variant="contained"
            disabled={pending}
            data-testid={testIds.review.settleSubmit}
          >
            {pending ? 'Un momento...' : 'Registrar'}
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}
