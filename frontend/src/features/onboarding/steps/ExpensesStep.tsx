import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlined';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import Chip from '@mui/material/Chip';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';

import { LoadingState } from '@/components/ui/LoadingState';
import { ApiError } from '@/lib/api/types';
import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';

import { expenseKindLabel, type ExpenseCategory } from '../../expenses/types';
import {
  useCreateExpense,
  useDeleteExpense,
  useExpenseCategories,
  useExpenses,
} from '../../expenses/useExpenses';

/**
 * Lo que sale, a partir del catalogo.
 *
 * <p>Elegir de una lista ya armada es mucho mas rapido que inventar una
 * taxonomia desde cero, y de paso el gasto nace con su categoria y con el tipo
 * de monto que suele tener: "Renta" estable, "Despensa" variable.
 *
 * <p>Tambien se puede escribir uno que no este en la lista: el catalogo es una
 * ayuda, no una reja.
 *
 * <p>Este paso se puede saltar. Quien todavia no sabe sus gastos captura
 * cualquier cosa por salir del paso, y un presupuesto con cifras inventadas es
 * peor que uno incompleto.
 */
export function ExpensesStep() {
  const { t } = useTranslation();
  const categorias = useExpenseCategories();
  const lista = useExpenses({ sort: 'NEWEST', page: 0, size: 50 });
  const crear = useCreateExpense();
  const eliminar = useDeleteExpense();

  const [elegida, setElegida] = useState<ExpenseCategory | null>(null);
  const [nombre, setNombre] = useState('');
  const [monto, setMonto] = useState('');

  const apiError = crear.error instanceof ApiError ? crear.error : null;
  const fieldErrors = apiError?.fieldErrorMap ?? {};
  const generalMessage = apiError && apiError.fieldErrors.length === 0 ? apiError.message : null;

  function elegir(category: ExpenseCategory) {
    setElegida(category);
    // El nombre se propone con el de la categoria, que casi siempre es el que
    // la persona habria escrito.
    setNombre(category.name);
    setMonto('');
  }

  function limpiar() {
    setElegida(null);
    setNombre('');
    setMonto('');
  }

  function agregar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    void crear
      .mutateAsync({
        name: nombre.trim(),
        kind: elegida?.defaultKind ?? 'FIXED',
        amount: monto.trim(),
        categoryId: elegida?.id ?? null,
        flexibility: 'IMPORTANT',
        frequency: 'MONTHLY',
        dueDay: 1,
        startDate: new Date().toISOString().slice(0, 10),
        endDate: null,
        notes: null,
      })
      .then(limpiar)
      .catch(() => undefined);
  }

  const gastos = lista.data?.content ?? [];
  const capturado = elegida !== null || nombre.length > 0;

  return (
    <Stack spacing={5} data-testid={testIds.onboarding.step('expenses')}>
      <Typography variant="body1" color="text.secondary">
        {t('onboarding.expenses.intro')}
      </Typography>

      {generalMessage && (
        <Alert severity="error" data-testid={testIds.onboarding.error}>
          {generalMessage}
        </Alert>
      )}

      {categorias.isPending && <LoadingState rows={2} />}

      <Stack direction="row" spacing={2} sx={{ flexWrap: 'wrap', gap: 2 }}>
        {(categorias.data ?? []).map((category) => (
          <Chip
            key={category.id}
            label={category.name}
            onClick={() => elegir(category)}
            color={elegida?.id === category.id ? 'primary' : 'default'}
            variant={elegida?.id === category.id ? 'filled' : 'outlined'}
            data-testid={testIds.onboarding.categoryChip(category.code)}
          />
        ))}
      </Stack>

      <Box component="form" onSubmit={agregar} noValidate>
        <Stack spacing={4}>
          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3}>
            <TextField
              label={t('onboarding.expenses.name')}
              value={nombre}
              onChange={(event) => setNombre(event.target.value)}
              error={Boolean(fieldErrors.name)}
              helperText={
                fieldErrors.name ??
                (elegida
                  ? t('onboarding.expenses.nameHelpWithCategory', {
                      category: elegida.name,
                      kind: expenseKindLabel(elegida.defaultKind).toLowerCase(),
                    })
                  : t('onboarding.expenses.nameHelpWithoutCategory'))
              }
              disabled={crear.isPending}
              fullWidth
              slotProps={{
                htmlInput: {
                  'data-testid': testIds.onboarding.expenseCustomName,
                  maxLength: 120,
                },
              }}
            />

            <TextField
              label={t('onboarding.expenses.amount')}
              value={monto}
              onChange={(event) => setMonto(event.target.value)}
              error={Boolean(fieldErrors.amount)}
              helperText={fieldErrors.amount ?? t('onboarding.expenses.amountHelp')}
              disabled={crear.isPending}
              sx={{ width: { xs: '100%', sm: 200 } }}
              slotProps={{
                htmlInput: {
                  'data-testid': testIds.onboarding.expenseAmount,
                  inputMode: 'decimal',
                  pattern: '\\d{1,13}(\\.\\d{1,2})?',
                },
              }}
            />
          </Stack>

          <Stack direction="row" spacing={3}>
            <Button type="submit" variant="outlined" disabled={crear.isPending || !capturado}>
              {crear.isPending ? t('common.oneMoment') : t('onboarding.expenses.add')}
            </Button>
            {capturado && (
              <Button onClick={limpiar} disabled={crear.isPending}>
                {t('onboarding.expenses.clear')}
              </Button>
            )}
          </Stack>
        </Stack>
      </Box>

      {gastos.length > 0 && (
        <Card variant="outlined" data-testid={testIds.onboarding.expenseList}>
          <Stack divider={<Divider flexItem />}>
            {gastos.map((expense) => (
              <Stack
                key={expense.id}
                direction="row"
                spacing={3}
                data-testid={testIds.onboarding.expenseRow}
                sx={{ alignItems: 'center', justifyContent: 'space-between', px: 5, py: 3 }}
              >
                <Stack spacing={0.5} sx={{ minWidth: 0 }}>
                  <Typography variant="body1">{expense.name}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {expense.category?.name ?? t('onboarding.expenses.noCategory')}
                    {expense.requiresReview && t('onboarding.expenses.variableSuffix')}
                  </Typography>
                </Stack>

                <Stack direction="row" spacing={3} sx={{ alignItems: 'center' }}>
                  <Typography variant="body1" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                    {formatMoney(expense.amount)}
                  </Typography>
                  <IconButton
                    size="small"
                    aria-label={t('onboarding.expenses.remove', { name: expense.name })}
                    disabled={eliminar.isPending}
                    onClick={() => eliminar.mutate({ id: expense.id, name: expense.name })}
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
