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
import { useTranslation } from 'react-i18next';

import { ApiError } from '@/lib/api/types';
import { testIds } from '@/lib/testids';

import {
  EXPENSE_KINDS,
  expenseKindLabel,
  FLEXIBILITIES,
  flexibilityLabel,
  FREQUENCIES,
  frequencyLabel,
  needsDueDay,
  type Expense,
  type ExpenseCategory,
  type ExpenseKind,
  type ExpensePayload,
  type Flexibility,
  type Frequency,
} from './types';

interface ExpenseFormDrawerProps {
  open: boolean;
  expense: Expense | null;
  categories: ExpenseCategory[];
  pending: boolean;
  error: unknown;
  onSubmit: (payload: ExpensePayload) => void;
  onClose: () => void;
}

interface FormValues {
  name: string;
  kind: ExpenseKind;
  amount: string;
  categoryId: string;
  flexibility: Flexibility;
  frequency: Frequency;
  dueDay: string;
  startDate: string;
  endDate: string;
  notes: string;
}

function hoy(): string {
  return new Date().toISOString().slice(0, 10);
}

function valoresIniciales(expense: Expense | null): FormValues {
  if (!expense) {
    return {
      name: '',
      kind: 'FIXED',
      amount: '',
      categoryId: '',
      flexibility: 'IMPORTANT',
      frequency: 'MONTHLY',
      dueDay: '1',
      startDate: hoy(),
      endDate: '',
      notes: '',
    };
  }

  return {
    name: expense.name,
    kind: expense.kind,
    amount: expense.amount.amount,
    categoryId: expense.category?.id ?? '',
    flexibility: expense.flexibility,
    frequency: expense.frequency,
    dueDay: expense.dueDay?.toString() ?? '',
    startDate: expense.startDate,
    endDate: expense.endDate ?? '',
    notes: expense.notes ?? '',
  };
}

/**
 * Captura y edicion de un gasto, fijo o variable.
 *
 * La validacion la manda el backend: llega en {@code errors[]} y se reparte por
 * campo. No se duplica aqui.
 */
export function ExpenseFormDrawer({
  open,
  expense,
  categories,
  pending,
  error,
  onSubmit,
  onClose,
}: ExpenseFormDrawerProps) {
  const { t } = useTranslation();
  const [values, setValues] = useState<FormValues>(() => valoresIniciales(expense));

  useEffect(() => {
    if (open) setValues(valoresIniciales(expense));
  }, [open, expense]);

  const apiError = error instanceof ApiError ? error : null;
  const fieldErrors = apiError?.fieldErrorMap ?? {};
  const generalMessage = apiError && apiError.fieldErrors.length === 0 ? apiError.message : null;

  const editando = expense !== null;
  const pideDia = needsDueDay(values.frequency);

  function set<K extends keyof FormValues>(key: K, value: FormValues[K]) {
    setValues((previous) => ({ ...previous, [key]: value }));
  }

  /**
   * Elegir categoria propone el tipo que suele tener: "Despensa" sugiere monto
   * que cambia, "Renta" sugiere estable. Solo al capturar uno nuevo, y solo si
   * todavia no se ha tocado el tipo: al editar seria cambiar algo que la
   * persona ya decidio.
   */
  function elegirCategoria(categoryId: string) {
    const categoria = categories.find((c) => c.id === categoryId);

    setValues((previous) => ({
      ...previous,
      categoryId,
      kind: !editando && categoria ? categoria.defaultKind : previous.kind,
    }));
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    onSubmit({
      name: values.name.trim(),
      kind: values.kind,
      amount: values.amount.trim(),
      categoryId: values.categoryId || null,
      // Al editar, "sin categoria" tiene que distinguirse de "no mande el
      // campo": el backend no puede adivinarlo desde un nulo.
      clearCategory: editando && values.categoryId === '',
      flexibility: values.flexibility,
      frequency: values.frequency,
      dueDay: pideDia && values.dueDay ? Number(values.dueDay) : null,
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
      data-testid={testIds.expenses.drawer}
      slotProps={{ paper: { sx: { width: { xs: '100%', sm: 460 }, maxWidth: '100%' } } }}
    >
      <Box component="form" onSubmit={handleSubmit} noValidate sx={{ p: 6 }}>
        <Stack spacing={5}>
          <Stack spacing={1}>
            <Typography variant="overline" color="text.disabled">
              {editando ? t('expenses.form.editing') : t('expenses.form.new')}
            </Typography>
            <Typography variant="h3" data-testid={testIds.expenses.drawerTitle}>
              {editando
                ? values.name || t('expenses.form.editTitleFallback')
                : t('expenses.form.newTitle')}
            </Typography>
          </Stack>

          {generalMessage && (
            <Alert severity="error" data-testid={testIds.expenses.formError}>
              {generalMessage}
            </Alert>
          )}

          <TextField
            label={t('expenses.form.name')}
            value={values.name}
            onChange={(event) => set('name', event.target.value)}
            error={Boolean(fieldErrors.name)}
            helperText={fieldErrors.name ?? t('expenses.form.nameHelp')}
            disabled={pending}
            fullWidth
            autoFocus
            slotProps={{ htmlInput: { 'data-testid': testIds.expenses.nameInput, maxLength: 120 } }}
          />

          <TextField
            select
            label={t('expenses.form.category')}
            value={values.categoryId}
            onChange={(event) => elegirCategoria(event.target.value)}
            error={Boolean(fieldErrors.categoryId)}
            helperText={fieldErrors.categoryId ?? t('expenses.form.categoryHelp')}
            disabled={pending}
            fullWidth
            data-testid={testIds.expenses.categoryInput}
          >
            <MenuItem value="">{t('expenses.form.noCategory')}</MenuItem>
            {categories.map((category) => (
              <MenuItem key={category.id} value={category.id}>
                {category.name}
              </MenuItem>
            ))}
          </TextField>

          <TextField
            select
            label={t('expenses.form.kind')}
            value={values.kind}
            onChange={(event) => set('kind', event.target.value as ExpenseKind)}
            error={Boolean(fieldErrors.kind)}
            helperText={fieldErrors.kind}
            disabled={pending}
            fullWidth
            data-testid={testIds.expenses.kindInput}
          >
            {EXPENSE_KINDS.map((kind) => (
              <MenuItem key={kind} value={kind}>
                {expenseKindLabel(kind)}
              </MenuItem>
            ))}
          </TextField>

          {values.kind === 'VARIABLE' && (
            <Alert severity="info" data-testid={testIds.expenses.reviewNotice}>
              {t('expenses.form.variableNotice')}
            </Alert>
          )}

          <TextField
            label={
              values.kind === 'VARIABLE'
                ? t('expenses.form.estimatedAmount')
                : t('expenses.form.amount')
            }
            value={values.amount}
            onChange={(event) => set('amount', event.target.value)}
            error={Boolean(fieldErrors.amount)}
            helperText={fieldErrors.amount ?? t('expenses.form.amountHelp')}
            disabled={pending}
            fullWidth
            slotProps={{
              htmlInput: {
                'data-testid': testIds.expenses.amountInput,
                inputMode: 'decimal',
                pattern: '\\d{1,13}(\\.\\d{1,2})?',
              },
            }}
          />

          <Divider />

          <TextField
            select
            label={t('expenses.form.flexibility')}
            value={values.flexibility}
            onChange={(event) => set('flexibility', event.target.value as Flexibility)}
            error={Boolean(fieldErrors.flexibility)}
            helperText={fieldErrors.flexibility}
            disabled={pending}
            fullWidth
            data-testid={testIds.expenses.flexibilityInput}
          >
            {FLEXIBILITIES.map((flexibility) => (
              <MenuItem key={flexibility} value={flexibility}>
                {flexibilityLabel(flexibility)}
              </MenuItem>
            ))}
          </TextField>

          {values.flexibility === 'CRITICAL' && (
            <Alert severity="info" data-testid={testIds.expenses.criticalNotice}>
              {t('expenses.form.criticalNotice')}
            </Alert>
          )}

          <TextField
            select
            label={t('expenses.form.frequency')}
            value={values.frequency}
            onChange={(event) => set('frequency', event.target.value as Frequency)}
            error={Boolean(fieldErrors.frequency)}
            helperText={fieldErrors.frequency}
            disabled={pending}
            fullWidth
            data-testid={testIds.expenses.frequencyInput}
          >
            {FREQUENCIES.map((frequency) => (
              <MenuItem key={frequency} value={frequency}>
                {frequencyLabel(frequency)}
              </MenuItem>
            ))}
          </TextField>

          {pideDia && (
            <TextField
              label={t('expenses.form.dueDay')}
              type="number"
              value={values.dueDay}
              onChange={(event) => set('dueDay', event.target.value)}
              error={Boolean(fieldErrors.dueDay)}
              helperText={fieldErrors.dueDay ?? t('expenses.form.dueDayHelp')}
              disabled={pending}
              fullWidth
              slotProps={{
                htmlInput: { 'data-testid': testIds.expenses.dueDayInput, min: 1, max: 31 },
              }}
            />
          )}

          <TextField
            label={t('expenses.form.startDate')}
            type="date"
            value={values.startDate}
            onChange={(event) => set('startDate', event.target.value)}
            error={Boolean(fieldErrors.startDate)}
            helperText={fieldErrors.startDate}
            disabled={pending}
            fullWidth
            slotProps={{
              inputLabel: { shrink: true },
              htmlInput: { 'data-testid': testIds.expenses.startDateInput },
            }}
          />

          <TextField
            label={t('expenses.form.endDate')}
            type="date"
            value={values.endDate}
            onChange={(event) => set('endDate', event.target.value)}
            error={Boolean(fieldErrors.endDate)}
            helperText={fieldErrors.endDate ?? t('expenses.form.endDateHelp')}
            disabled={pending}
            fullWidth
            slotProps={{
              inputLabel: { shrink: true },
              htmlInput: { 'data-testid': testIds.expenses.endDateInput },
            }}
          />

          <TextField
            label={t('expenses.form.notes')}
            value={values.notes}
            onChange={(event) => set('notes', event.target.value)}
            error={Boolean(fieldErrors.notes)}
            helperText={fieldErrors.notes}
            disabled={pending}
            fullWidth
            multiline
            minRows={2}
            slotProps={{
              htmlInput: { 'data-testid': testIds.expenses.notesInput, maxLength: 500 },
            }}
          />

          {editando && (
            <Typography variant="body2" color="text.secondary">
              {t('expenses.form.editNotice')}
            </Typography>
          )}

          <Stack direction="row" spacing={3} sx={{ justifyContent: 'flex-end', pt: 2 }}>
            <Button
              onClick={onClose}
              disabled={pending}
              data-testid={testIds.expenses.cancelButton}
            >
              {t('common.cancel')}
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={pending}
              data-testid={testIds.expenses.submitButton}
            >
              {pending
                ? t('common.oneMoment')
                : editando
                  ? t('expenses.form.submitEdit')
                  : t('expenses.form.submitNew')}
            </Button>
          </Stack>
        </Stack>
      </Box>
    </Drawer>
  );
}
