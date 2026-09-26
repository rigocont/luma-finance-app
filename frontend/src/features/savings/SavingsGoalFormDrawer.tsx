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
  CONTRIBUTION_MODES,
  contributionModeHelp,
  contributionModeLabel,
  type ContributionMode,
  type SavingsGoal,
  type SavingsGoalPayload,
} from './types';

interface SavingsGoalFormDrawerProps {
  open: boolean;
  goal: SavingsGoal | null;
  pending: boolean;
  error: unknown;
  onSubmit: (payload: SavingsGoalPayload) => void;
  onClose: () => void;
}

interface FormValues {
  name: string;
  target: string;
  mode: ContributionMode;
  targetDate: string;
  plannedPerCycle: string;
}

function valoresIniciales(goal: SavingsGoal | null): FormValues {
  if (!goal) {
    return {
      name: '',
      target: '',
      mode: 'AUTO_BY_TARGET_DATE',
      targetDate: '',
      plannedPerCycle: '',
    };
  }

  return {
    name: goal.name,
    target: goal.target.amount,
    mode: goal.contributionMode,
    targetDate: goal.targetDate ?? '',
    plannedPerCycle: goal.plannedPerCycle.amount,
  };
}

/**
 * Captura y edicion de una meta de ahorro.
 *
 * <p>La validacion la manda el backend y llega por campo en {@code errors[]}.
 * Aqui solo se decide QUE campos se piden, que depende del modo de aporte:
 * pedir fecha objetivo a quien aporta cuando puede seria pedir un dato que no
 * existe.
 */
export function SavingsGoalFormDrawer({
  open,
  goal,
  pending,
  error,
  onSubmit,
  onClose,
}: SavingsGoalFormDrawerProps) {
  const { t } = useTranslation();
  const [values, setValues] = useState<FormValues>(() => valoresIniciales(goal));

  useEffect(() => {
    if (open) setValues(valoresIniciales(goal));
  }, [open, goal]);

  const apiError = error instanceof ApiError ? error : null;
  const fieldErrors = apiError?.fieldErrorMap ?? {};
  const generalMessage = apiError && apiError.fieldErrors.length === 0 ? apiError.message : null;

  const editando = goal !== null;
  const pideFecha = values.mode === 'AUTO_BY_TARGET_DATE';
  const pideMonto = values.mode === 'FIXED_PER_CYCLE';

  function set<K extends keyof FormValues>(key: K, value: FormValues[K]) {
    setValues((previous) => ({ ...previous, [key]: value }));
  }

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const fecha = pideFecha && values.targetDate ? values.targetDate : null;

    onSubmit({
      name: values.name.trim(),
      target: values.target.trim(),
      targetDate: fecha,
      // Al editar, "quitale la fecha" tiene que distinguirse de "no mande el
      // campo": las dos cosas llegan como nulo y significan lo contrario.
      clearTargetDate: editando && fecha === null && goal.targetDate !== null,
      mode: values.mode,
      plannedPerCycle: pideMonto && values.plannedPerCycle ? values.plannedPerCycle.trim() : null,
    });
  }

  return (
    <Drawer
      anchor="right"
      open={open}
      onClose={pending ? undefined : onClose}
      data-testid={testIds.savings.drawer}
      slotProps={{ paper: { sx: { width: { xs: '100%', sm: 460 }, maxWidth: '100%' } } }}
    >
      <Box component="form" onSubmit={handleSubmit} noValidate sx={{ p: 6 }}>
        <Stack spacing={5}>
          <Stack spacing={1}>
            <Typography variant="overline" color="text.disabled">
              {editando ? t('savings.form.editing') : t('savings.form.new')}
            </Typography>
            <Typography variant="h3" data-testid={testIds.savings.drawerTitle}>
              {editando
                ? values.name || t('savings.form.editTitleFallback')
                : t('savings.form.newTitle')}
            </Typography>
          </Stack>

          {generalMessage && (
            <Alert severity="error" data-testid={testIds.savings.formError}>
              {generalMessage}
            </Alert>
          )}

          <TextField
            label={t('savings.form.name')}
            value={values.name}
            onChange={(event) => set('name', event.target.value)}
            error={Boolean(fieldErrors.name)}
            helperText={fieldErrors.name ?? t('savings.form.nameHelp')}
            disabled={pending}
            fullWidth
            autoFocus
            slotProps={{ htmlInput: { 'data-testid': testIds.savings.nameInput, maxLength: 120 } }}
          />

          <TextField
            label={t('savings.form.target')}
            value={values.target}
            onChange={(event) => set('target', event.target.value)}
            error={Boolean(fieldErrors.target)}
            helperText={fieldErrors.target ?? t('savings.form.targetHelp')}
            disabled={pending}
            fullWidth
            slotProps={{
              htmlInput: {
                'data-testid': testIds.savings.targetInput,
                inputMode: 'decimal',
                pattern: '\\d{1,13}(\\.\\d{1,2})?',
              },
            }}
          />

          <Divider />

          <TextField
            select
            label={t('savings.form.mode')}
            value={values.mode}
            onChange={(event) => set('mode', event.target.value as ContributionMode)}
            error={Boolean(fieldErrors.mode)}
            helperText={fieldErrors.mode ?? contributionModeHelp(values.mode)}
            disabled={pending}
            fullWidth
            data-testid={testIds.savings.modeInput}
          >
            {CONTRIBUTION_MODES.map((mode) => (
              <MenuItem key={mode} value={mode}>
                {contributionModeLabel(mode)}
              </MenuItem>
            ))}
          </TextField>

          {pideFecha && (
            <TextField
              label={t('savings.form.targetDate')}
              type="date"
              value={values.targetDate}
              onChange={(event) => set('targetDate', event.target.value)}
              error={Boolean(fieldErrors.targetDate)}
              helperText={fieldErrors.targetDate ?? t('savings.form.targetDateHelp')}
              disabled={pending}
              fullWidth
              slotProps={{
                inputLabel: { shrink: true },
                htmlInput: { 'data-testid': testIds.savings.targetDateInput },
              }}
            />
          )}

          {pideMonto && (
            <TextField
              label={t('savings.form.perCycle')}
              value={values.plannedPerCycle}
              onChange={(event) => set('plannedPerCycle', event.target.value)}
              error={Boolean(fieldErrors.plannedPerCycle)}
              helperText={fieldErrors.plannedPerCycle ?? t('savings.form.perCycleHelp')}
              disabled={pending}
              fullWidth
              slotProps={{
                htmlInput: {
                  'data-testid': testIds.savings.perCycleInput,
                  inputMode: 'decimal',
                  pattern: '\\d{1,13}(\\.\\d{1,2})?',
                },
              }}
            />
          )}

          {values.mode === 'MANUAL' && <Alert severity="info">{t('savings.manualNotice')}</Alert>}

          {editando && (
            <Typography variant="body2" color="text.secondary">
              {t('savings.form.editNotice')}
            </Typography>
          )}

          <Stack direction="row" spacing={3} sx={{ justifyContent: 'flex-end', pt: 2 }}>
            <Button onClick={onClose} disabled={pending} data-testid={testIds.savings.cancelButton}>
              {t('common.cancel')}
            </Button>
            <Button
              type="submit"
              variant="contained"
              disabled={pending}
              data-testid={testIds.savings.submitButton}
            >
              {pending
                ? t('common.oneMoment')
                : editando
                  ? t('savings.form.submitEdit')
                  : t('savings.form.submitNew')}
            </Button>
          </Stack>
        </Stack>
      </Box>
    </Drawer>
  );
}
