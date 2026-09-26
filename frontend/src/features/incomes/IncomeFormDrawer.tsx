import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Divider from '@mui/material/Divider';
import Drawer from '@mui/material/Drawer';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useEffect, useState, type FormEvent } from 'react';

import { ApiError } from '@/lib/api/types';
import { testIds } from '@/lib/testids';

import {
  FREQUENCIES,
  FREQUENCY_LABELS,
  INCOME_TYPES,
  INCOME_TYPE_LABELS,
  needsExpectedDay,
  typeRequiresReview,
  type Frequency,
  type Income,
  type IncomePayload,
  type IncomeType,
} from './types';

interface IncomeFormDrawerProps {
  open: boolean;
  /** Nulo para capturar uno nuevo; el ingreso a editar en caso contrario. */
  income: Income | null;
  pending: boolean;
  error: unknown;
  onSubmit: (payload: IncomePayload) => void;
  onClose: () => void;
}

interface FormValues {
  name: string;
  type: IncomeType;
  amount: string;
  frequency: Frequency;
  expectedDay: string;
  startDate: string;
  endDate: string;
  notes: string;
}

function hoy(): string {
  return new Date().toISOString().slice(0, 10);
}

function valoresIniciales(income: Income | null): FormValues {
  if (!income) {
    return {
      name: '',
      type: 'RECURRENT',
      amount: '',
      frequency: 'BIWEEKLY',
      expectedDay: '15',
      startDate: hoy(),
      endDate: '',
      notes: '',
    };
  }

  return {
    name: income.name,
    type: income.type,
    amount: income.amount.amount,
    frequency: income.frequency,
    expectedDay: income.expectedDay?.toString() ?? '',
    startDate: income.startDate,
    endDate: income.endDate ?? '',
    notes: income.notes ?? '',
  };
}

/**
 * Captura y edicion de un ingreso.
 *
 * Es un cajon y no un dialogo porque el formulario tiene ocho campos: en un
 * telefono un dialogo de ese tamano acaba siendo una pantalla completa mal
 * hecha, y aqui ocupa el ancho completo de forma deliberada.
 *
 * La validacion de campos la manda el backend y llega en {@code errors[]}. No
 * se duplica aqui: dos definiciones de las mismas reglas se separan con el
 * tiempo, y la que gobierna de verdad es la del servidor.
 */
export function IncomeFormDrawer({
  open,
  income,
  pending,
  error,
  onSubmit,
  onClose,
}: IncomeFormDrawerProps) {
  const [values, setValues] = useState<FormValues>(() => valoresIniciales(income));

  // Al abrirse, el formulario refleja el ingreso que se va a editar (o queda
  // limpio). Sin esto, editar uno y luego otro mostraria los datos del primero.
  useEffect(() => {
    if (open) setValues(valoresIniciales(income));
  }, [open, income]);

  const apiError = error instanceof ApiError ? error : null;
  const fieldErrors = apiError?.fieldErrorMap ?? {};
  const generalMessage = apiError && apiError.fieldErrors.length === 0 ? apiError.message : null;

  const editando = income !== null;
  const pideDia = needsExpectedDay(values.frequency);
  const pedira = typeRequiresReview(values.type);

  function set<K extends keyof FormValues>(key: K, value: FormValues[K]) {
    setValues((previous) => ({ ...previous, [key]: value }));
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    onSubmit({
      name: values.name.trim(),
      type: values.type,
      amount: values.amount.trim(),
      frequency: values.frequency,
      // Una cadena vacia no es lo mismo que "sin valor": el backend espera nulo.
      expectedDay: pideDia && values.expectedDay ? Number(values.expectedDay) : null,
      startDate: values.startDate,
      endDate: values.endDate || null,
      notes: values.notes.trim() || null,
    });
  }

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={pending ? undefined : onClose}
      data-testid={testIds.incomes.drawer}
      slotProps={{ paper: { sx: { width: { xs: '100%', sm: 460 }, maxWidth: '100%' } } }}
    >
      <Box component="form" onSubmit={handleSubmit} noValidate sx={{ p: 6 }}>
        <Stack spacing={5}>
          <Stack spacing={1}>
            <Typography variant="overline" color="text.disabled">
              {editando ? 'Editar' : 'Nuevo'}
            </Typography>
            <Typography variant="h3" data-testid={testIds.incomes.drawerTitle}>
              {editando ? values.name || 'Ingreso' : 'Capturar un ingreso'}
            </Typography>
          </Stack>

          {generalMessage && (
            <Alert severity="error" data-testid={testIds.incomes.formError}>
              {generalMessage}
            </Alert>
          )}

          <TextField
            label="Nombre"
            value={values.name}
            onChange={(event) => set('name', event.target.value)}
            error={Boolean(fieldErrors.name)}
            helperText={
              fieldErrors.name ?? 'Como lo reconoces: Sueldo, Comisiones, Renta del local'
            }
            disabled={pending}
            fullWidth
            autoFocus
            slotProps={{ htmlInput: { 'data-testid': testIds.incomes.nameInput, maxLength: 120 } }}
          />

          <TextField
            select
            label="Tipo"
            value={values.type}
            onChange={(event) => set('type', event.target.value as IncomeType)}
            error={Boolean(fieldErrors.type)}
            helperText={fieldErrors.type}
            disabled={pending}
            fullWidth
            data-testid={testIds.incomes.typeInput}
          >
            {INCOME_TYPES.map((type) => (
              <MenuItem key={type} value={type}>
                {INCOME_TYPE_LABELS[type]}
              </MenuItem>
            ))}
          </TextField>

          {pedira && (
            <Alert severity="info" data-testid={testIds.incomes.reviewNotice}>
              Como el monto cambia, cada ciclo te va a pedir confirmar cuanto fue antes de darlo por
              seguro.
            </Alert>
          )}

          <TextField
            label="Monto"
            value={values.amount}
            onChange={(event) => set('amount', event.target.value)}
            error={Boolean(fieldErrors.amount)}
            helperText={fieldErrors.amount ?? 'Hasta dos decimales, sin signos ni comas'}
            disabled={pending}
            fullWidth
            slotProps={{
              htmlInput: {
                'data-testid': testIds.incomes.amountInput,
                inputMode: 'decimal',
                // El punto y hasta dos decimales: el mismo patron que valida el
                // backend, para que el teclado ayude en lugar de estorbar.
                pattern: '\\d{1,13}(\\.\\d{1,2})?',
              },
            }}
          />

          <Divider />

          <TextField
            select
            label="Cada cuanto"
            value={values.frequency}
            onChange={(event) => set('frequency', event.target.value as Frequency)}
            error={Boolean(fieldErrors.frequency)}
            helperText={fieldErrors.frequency}
            disabled={pending}
            fullWidth
            data-testid={testIds.incomes.frequencyInput}
          >
            {FREQUENCIES.map((frequency) => (
              <MenuItem key={frequency} value={frequency}>
                {FREQUENCY_LABELS[frequency]}
              </MenuItem>
            ))}
          </TextField>

          {pideDia && (
            <TextField
              label="Dia del mes"
              type="number"
              value={values.expectedDay}
              onChange={(event) => set('expectedDay', event.target.value)}
              error={Boolean(fieldErrors.expectedDay)}
              helperText={
                fieldErrors.expectedDay ?? 'Si pones 31, en los meses cortos cae el ultimo dia'
              }
              disabled={pending}
              fullWidth
              slotProps={{
                htmlInput: { 'data-testid': testIds.incomes.expectedDayInput, min: 1, max: 31 },
              }}
            />
          )}

          <TextField
            label="Desde"
            type="date"
            value={values.startDate}
            onChange={(event) => set('startDate', event.target.value)}
            error={Boolean(fieldErrors.startDate)}
            helperText={fieldErrors.startDate}
            disabled={pending}
            fullWidth
            slotProps={{
              inputLabel: { shrink: true },
              htmlInput: { 'data-testid': testIds.incomes.startDateInput },
            }}
          />

          <TextField
            label="Hasta (opcional)"
            type="date"
            value={values.endDate}
            onChange={(event) => set('endDate', event.target.value)}
            error={Boolean(fieldErrors.endDate)}
            helperText={fieldErrors.endDate ?? 'Dejalo vacio si no tiene fecha de termino'}
            disabled={pending}
            fullWidth
            slotProps={{
              inputLabel: { shrink: true },
              htmlInput: { 'data-testid': testIds.incomes.endDateInput },
            }}
          />

          <TextField
            label="Notas (opcional)"
            value={values.notes}
            onChange={(event) => set('notes', event.target.value)}
            error={Boolean(fieldErrors.notes)}
            helperText={fieldErrors.notes}
            disabled={pending}
            fullWidth
            multiline
            minRows={2}
            slotProps={{ htmlInput: { 'data-testid': testIds.incomes.notesInput, maxLength: 500 } }}
          />

          {editando && (
            <Typography variant="body2" color="text.secondary">
              Los cambios aplican desde el siguiente ciclo. El ciclo en curso conserva lo que ya
              tenia.
            </Typography>
          )}

          <Stack direction="row" spacing={3} sx={{ justifyContent: 'flex-end', pt: 2 }}>
            <Button onClick={onClose} disabled={pending} data-testid={testIds.incomes.cancelButton}>
              Cancelar
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={pending}
              data-testid={testIds.incomes.submitButton}
            >
              {pending ? 'Un momento...' : editando ? 'Guardar cambios' : 'Capturar ingreso'}
            </Button>
          </Stack>
        </Stack>
      </Box>
    </Drawer>
  );
}
