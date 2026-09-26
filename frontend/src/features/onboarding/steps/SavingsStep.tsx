import DeleteOutlineIcon from '@mui/icons-material/DeleteOutlined';
import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import Divider from '@mui/material/Divider';
import IconButton from '@mui/material/IconButton';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';

import { ApiError } from '@/lib/api/types';
import { formatMoney } from '@/lib/money';
import { testIds } from '@/lib/testids';

import { useCreateGoal, useDeleteGoal, useSavingsGoals } from '../../savings/useSavings';

/**
 * Una primera meta de ahorro. Opcional.
 *
 * <p>Se ofrece solo el aporte fijo por ciclo, no los tres modos. En el alta la
 * persona todavia no tiene un ciclo abierto ni sabe cuanto le sobra; elegir
 * entre tres formas de calcular el aporte es una decision que no tiene con que
 * tomar. El resto de los modos estan en la seccion de Ahorros, con la meta ya
 * creada y el presupuesto a la vista.
 */
export function SavingsStep() {
  const { t } = useTranslation();
  const metas = useSavingsGoals();
  const crear = useCreateGoal();
  const eliminar = useDeleteGoal();

  const [nombre, setNombre] = useState('');
  const [objetivo, setObjetivo] = useState('');
  const [porCiclo, setPorCiclo] = useState('');

  const apiError = crear.error instanceof ApiError ? crear.error : null;
  const fieldErrors = apiError?.fieldErrorMap ?? {};
  const generalMessage = apiError && apiError.fieldErrors.length === 0 ? apiError.message : null;

  function agregar(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    void crear
      .mutateAsync({
        name: nombre.trim(),
        target: objetivo.trim(),
        targetDate: null,
        mode: 'FIXED_PER_CYCLE',
        plannedPerCycle: porCiclo.trim(),
      })
      .then(() => {
        setNombre('');
        setObjetivo('');
        setPorCiclo('');
      })
      .catch(() => undefined);
  }

  const lista = metas.data ?? [];

  return (
    <Stack spacing={5} data-testid={testIds.onboarding.step('savings')}>
      <Typography variant="body1" color="text.secondary">
        {t('onboarding.savings.intro')}
      </Typography>

      {generalMessage && (
        <Alert severity="error" data-testid={testIds.onboarding.error}>
          {generalMessage}
        </Alert>
      )}

      <Box component="form" onSubmit={agregar} noValidate>
        <Stack spacing={4}>
          <TextField
            label={t('onboarding.savings.name')}
            value={nombre}
            onChange={(event) => setNombre(event.target.value)}
            error={Boolean(fieldErrors.name)}
            helperText={fieldErrors.name ?? t('onboarding.savings.nameHelp')}
            disabled={crear.isPending}
            fullWidth
            slotProps={{
              htmlInput: { 'data-testid': testIds.onboarding.goalName, maxLength: 120 },
            }}
          />

          <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3}>
            <TextField
              label={t('onboarding.savings.target')}
              value={objetivo}
              onChange={(event) => setObjetivo(event.target.value)}
              error={Boolean(fieldErrors.target)}
              helperText={fieldErrors.target}
              disabled={crear.isPending}
              fullWidth
              slotProps={{
                htmlInput: {
                  'data-testid': testIds.onboarding.goalTarget,
                  inputMode: 'decimal',
                  pattern: '\\d{1,13}(\\.\\d{1,2})?',
                },
              }}
            />

            <TextField
              label={t('onboarding.savings.perCycle')}
              value={porCiclo}
              onChange={(event) => setPorCiclo(event.target.value)}
              error={Boolean(fieldErrors.plannedPerCycle)}
              helperText={fieldErrors.plannedPerCycle ?? t('onboarding.savings.perCycleHelp')}
              disabled={crear.isPending}
              fullWidth
              slotProps={{
                htmlInput: {
                  'data-testid': testIds.onboarding.goalPerCycle,
                  inputMode: 'decimal',
                  pattern: '\\d{1,13}(\\.\\d{1,2})?',
                },
              }}
            />
          </Stack>

          <Box>
            <Button
              type="submit"
              variant="outlined"
              disabled={crear.isPending}
              data-testid={testIds.onboarding.goalAdd}
            >
              {crear.isPending ? t('common.oneMoment') : t('onboarding.savings.add')}
            </Button>
          </Box>
        </Stack>
      </Box>

      {lista.length > 0 && (
        <Card variant="outlined" data-testid={testIds.onboarding.goalList}>
          <Stack divider={<Divider flexItem />}>
            {lista.map((goal) => (
              <Stack
                key={goal.id}
                direction="row"
                spacing={3}
                sx={{ alignItems: 'center', justifyContent: 'space-between', px: 5, py: 3 }}
              >
                <Stack spacing={0.5} sx={{ minWidth: 0 }}>
                  <Typography variant="body1">{goal.name}</Typography>
                  <Typography variant="caption" color="text.secondary">
                    {formatMoney(goal.plannedPerCycle)} {t('onboarding.savings.perCycleSuffix')}
                  </Typography>
                </Stack>

                <Stack direction="row" spacing={3} sx={{ alignItems: 'center' }}>
                  <Typography variant="body1" sx={{ fontVariantNumeric: 'tabular-nums' }}>
                    {formatMoney(goal.target)}
                  </Typography>
                  <IconButton
                    size="small"
                    aria-label={t('onboarding.savings.remove', { name: goal.name })}
                    disabled={eliminar.isPending}
                    onClick={() => eliminar.mutate({ id: goal.id, name: goal.name })}
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
