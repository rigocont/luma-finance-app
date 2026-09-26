import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Dialog from '@mui/material/Dialog';
import DialogActions from '@mui/material/DialogActions';
import DialogContent from '@mui/material/DialogContent';
import DialogTitle from '@mui/material/DialogTitle';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useEffect, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';

import { ApiError } from '@/lib/api/types';
import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';

import type { MovementPayload, SavingsGoal } from './types';

type MovementKind = MovementPayload['type'];

interface SavingsMovementDialogProps {
  open: boolean;
  goal: SavingsGoal | null;
  kind: MovementKind;
  pending: boolean;
  error: unknown;
  onSubmit: (payload: MovementPayload) => void;
  onClose: () => void;
}

function hoy(): string {
  return new Date().toISOString().slice(0, 10);
}

/**
 * Registra una aportacion suelta o un retiro sobre una meta.
 *
 * <p>Un solo componente para las dos cosas: cambian el titulo y el verbo, no el
 * formulario. El monto se captura SIEMPRE positivo y el signo lo decide el
 * tipo, igual que en el backend; pedir "-500" invitaria a registrar un retiro
 * disfrazado de aportacion.
 */
export function SavingsMovementDialog({
  open,
  goal,
  kind,
  pending,
  error,
  onSubmit,
  onClose,
}: SavingsMovementDialogProps) {
  const { t } = useTranslation();
  const [amount, setAmount] = useState('');
  const [date, setDate] = useState(hoy());
  const [notes, setNotes] = useState('');

  useEffect(() => {
    if (!open) return;
    setAmount('');
    setDate(hoy());
    setNotes('');
  }, [open, goal, kind]);

  const apiError = error instanceof ApiError ? error : null;
  const fieldErrors = apiError?.fieldErrorMap ?? {};
  const generalMessage = apiError && apiError.fieldErrors.length === 0 ? apiError.message : null;

  const retiro = kind === 'WITHDRAWAL';

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    onSubmit({
      amount: amount.trim(),
      date: date || null,
      type: kind,
      notes: notes.trim() || null,
    });
  }

  return (
    <Dialog
      open={open}
      onClose={pending ? undefined : onClose}
      fullWidth
      maxWidth="xs"
      aria-labelledby="savings-movement-title"
      data-testid={testIds.savings.movementDialog}
    >
      <Box component="form" onSubmit={handleSubmit} noValidate>
        <DialogTitle id="savings-movement-title">
          {retiro ? t('savings.movement.titleWithdrawal') : t('savings.movement.titleContribution')}
        </DialogTitle>

        <DialogContent>
          <Stack spacing={5} sx={{ pt: 2 }}>
            {goal && (
              <Typography variant="body2" color="text.secondary">
                {t('savings.movement.summary', {
                  name: goal.name,
                  saved: formatMoney(goal.saved),
                  target: formatMoney(goal.target),
                })}
              </Typography>
            )}

            {generalMessage && (
              <Alert severity="error" data-testid={testIds.savings.movementError}>
                {generalMessage}
              </Alert>
            )}

            <TextField
              label={t('savings.movement.amount')}
              value={amount}
              onChange={(event) => setAmount(event.target.value)}
              error={Boolean(fieldErrors.amount)}
              helperText={
                fieldErrors.amount ??
                (retiro
                  ? t('savings.movement.amountHelpWithdrawal')
                  : t('savings.movement.amountHelpContribution'))
              }
              disabled={pending}
              fullWidth
              autoFocus
              slotProps={{
                htmlInput: {
                  'data-testid': testIds.savings.movementAmount,
                  inputMode: 'decimal',
                  pattern: '\\d{1,13}(\\.\\d{1,2})?',
                },
              }}
            />

            <TextField
              label={t('savings.movement.when')}
              type="date"
              value={date}
              onChange={(event) => setDate(event.target.value)}
              error={Boolean(fieldErrors.date)}
              helperText={fieldErrors.date}
              disabled={pending}
              fullWidth
              slotProps={{
                inputLabel: { shrink: true },
                htmlInput: { 'data-testid': testIds.savings.movementDate },
              }}
            />

            <TextField
              label={t('savings.movement.notes')}
              value={notes}
              onChange={(event) => setNotes(event.target.value)}
              error={Boolean(fieldErrors.notes)}
              helperText={
                fieldErrors.notes ??
                (retiro ? t('savings.movement.notesHelpWithdrawal') : undefined)
              }
              disabled={pending}
              fullWidth
              multiline
              minRows={2}
              slotProps={{
                htmlInput: { 'data-testid': testIds.savings.movementNotes, maxLength: 500 },
              }}
            />
          </Stack>
        </DialogContent>

        <DialogActions>
          <Button onClick={onClose} disabled={pending}>
            {t('common.cancel')}
          </Button>
          <Button
            type="submit"
            variant="contained"
            color={retiro ? 'warning' : 'primary'}
            disabled={pending}
            data-testid={testIds.savings.movementSubmit}
          >
            {pending
              ? t('common.oneMoment')
              : retiro
                ? t('savings.movement.submitWithdrawal')
                : t('savings.movement.submitContribution')}
          </Button>
        </DialogActions>
      </Box>
    </Dialog>
  );
}
