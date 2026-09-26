import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlined';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';

import { LoadingState } from '@/components/ui/LoadingState';
import { ApiError } from '@/lib/api/types';
import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';

import {
  FREQUENCIES,
  frequencyLabel,
  INCOME_TYPES,
  incomeTypeLabel,
  needsExpectedDay,
  type Frequency,
  type IncomeType,
} from '../../incomes/types';
import { useCreateIncome, useDeleteIncome, useIncomes } from '../../incomes/useIncomes';

const VACIO = {
  name: '',
  type: 'RECURRENT' as IncomeType,
  amount: '',
  frequency: 'BIWEEKLY' as Frequency,
  expectedDay: '15',
};

/**
 * Lo que entra. Es el unico paso obligatorio.
 *
 * <p>Sin un ingreso no hay presupuesto que calcular: el resumen final diria "te
 * quedan $0" y eso no le sirve a nadie.
 *
 * <p>El formulario es en linea, no un cajon. En el asistente la persona va a
 * capturar dos o tres seguidos, y abrir y cerrar un panel cada vez convierte
 * tres capturas en nueve clics.
 */
export function IncomesStep() {
  const { t } = useTranslation();
  const lista = useIncomes({ sort: 'NEWEST', page: 0, size: 50 });
  const crear = useCreateIncome();
  const eliminar = useDeleteIncome();

  const [values, setValues] = useState(VACIO);

  const apiError = crear.error instanceof ApiError ? crear.error : null;
  const fieldErrors = apiError?.fieldErrorMap ?? {};
  const generalMessage = apiError && apiError.fieldErrors.length === 0 ? apiError.message : null;

  const pideDia = needsExpectedDay(values.frequency);

  function set<K extends keyof typeof VACIO>(key: K, value: (typeof VACIO)[K]) {
    setValues((previos) => ({ ...previos, [key]: value }));
  }

  function agregar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    void crear
      .mutateAsync({
        name: values.name.trim(),
        type: values.type,
        amount: values.amount.trim(),
        frequency: values.frequency,
        expectedDay: pideDia && values.expectedDay ? Number(values.expectedDay) : null,
        startDate: new Date().toISOString().slice(0, 10),
        endDate: null,
        notes: null,
      })
      // Solo se limpia si salio bien: perder lo capturado por un error de
      // validacion obligaria a escribirlo todo de nuevo.
      .then(() => setValues(VACIO))
      .catch(() => undefined);
  }

  const ingresos = lista.data?.content ?? [];

  return (
    <Stack spacing={5} data-testid={testIds.onboarding.step('incomes')}>
      <Typography variant="body1" color="text.secondary">
        {t('onboarding.incomes.intro')}
      </Typography>

      {generalMessage && (
        <Alert severity="error" data-testid={testIds.onboarding.error}>
          {generalMessage}
        </Alert>
      )}

      <Box component="form" onSubmit={agregar} noValidate>
        <Stack spacing={4}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3}>
            <TextField
              label={t('onboarding.incomes.name')}
              value={values.name}
              onChange={(event) => set('name', event.target.value)}
              error={Boolean(fieldErrors.name)}
              helperText={fieldErrors.name ?? t('onboarding.incomes.nameHelp')}
              disabled={crear.isPending}
              fullWidth
              autoFocus
              slotProps={{
                htmlInput: { 'data-testid': testIds.onboarding.incomeName, maxLength: 120 },
              }}
            />

            <TextField
              label={t('onboarding.incomes.amount')}
              value={values.amount}
              onChange={(event) => set('amount', event.target.value)}
              error={Boolean(fieldErrors.amount)}
              helperText={fieldErrors.amount ?? t('onboarding.incomes.amountHelp')}
              disabled={crear.isPending}
              sx={{ width: { xs: '100%', sm: 200 } }}
              slotProps={{
                htmlInput: {
                  'data-testid': testIds.onboarding.incomeAmount,
                  inputMode: 'decimal',
                  pattern: '\\d{1,13}(\\.\\d{1,2})?',
                },
              }}
            />
          </Stack>

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3}>
            <TextField
              select
              label={t('onboarding.incomes.type')}
              value={values.type}
              onChange={(event) => set('type', event.target.value as IncomeType)}
              error={Boolean(fieldErrors.type)}
              helperText={fieldErrors.type}
              disabled={crear.isPending}
              fullWidth
              data-testid={testIds.onboarding.incomeType}
            >
              {INCOME_TYPES.map((value) => (
                <MenuItem key={value} value={value}>
                  {incomeTypeLabel(value)}
                </MenuItem>
              ))}
            </TextField>

            <TextField
              select
              label={t('onboarding.incomes.frequency')}
              value={values.frequency}
              onChange={(event) => set('frequency', event.target.value as Frequency)}
              error={Boolean(fieldErrors.frequency)}
              helperText={fieldErrors.frequency}
              disabled={crear.isPending}
              fullWidth
              data-testid={testIds.onboarding.incomeFrequency}
            >
              {FREQUENCIES.map((value) => (
                <MenuItem key={value} value={value}>
                  {frequencyLabel(value)}
                </MenuItem>
              ))}
            </TextField>

            {pideDia && (
              <TextField
                label={t('onboarding.incomes.day')}
                type="number"
                value={values.expectedDay}
                onChange={(event) => set('expectedDay', event.target.value)}
                error={Boolean(fieldErrors.expectedDay)}
                helperText={fieldErrors.expectedDay}
                disabled={crear.isPending}
                sx={{ width: { xs: '100%', sm: 120 } }}
                slotProps={{
                  htmlInput: {
                    'data-testid': testIds.onboarding.incomeDay,
                    min: 1,
                    max: 31,
                  },
                }}
              />
            )}
          </Stack>

          <Box>
            <Button
              type="submit"
              variant="outlined"
              disabled={crear.isPending}
              data-testid={testIds.onboarding.incomeAdd}
            >
              {crear.isPending ? t('common.oneMoment') : t('onboarding.incomes.add')}
            </Button>
          </Box>
        </Stack>
      </Box>

      {lista.isPending && <LoadingState rows={2} />}

      {ingresos.length > 0 && (
        <Card variant="outlined" data-testid={testIds.onboarding.incomeList}>
          <Stack divider={<Divider flexItem />}>
            {ingresos.map((income) => (
              <Stack
                key={income.id}
                direction="row"
                spacing={3}
                data-testid={testIds.onboarding.incomeRow}
                sx={{ alignItems: 'center', justifyContent: 'space-between', px: 5, py: 3 }}
              >
                <Stack spacing={0.5} sx={{ minWidth: 0 }}>
                  <Typography variant="body1">{income.name}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {frequencyLabel(income.frequency)}
                  </Typography>
                </Stack>

                <Stack direction="row" spacing={3} sx={{ alignItems: 'center' }}>
                  <Typography variant="body1" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                    {formatMoney(income.amount)}
                  </Typography>
                  <IconButton
                    size="small"
                    aria-label={t('onboarding.incomes.remove', { name: income.name })}
                    disabled={eliminar.isPending}
                    onClick={() => eliminar.mutate({ id: income.id, name: income.name })}
                  >
                    <DeleteOutlineIcon fontSize="small" />
                  </IconButton>
                </Stack>
              </Stack>
            ))}
          </Stack>
        </Card>
      )}
    </Stack>
  );
}
