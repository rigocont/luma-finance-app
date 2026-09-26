import Alert from '@mui/material/Alert';
import Box from '@mui/material/Box';
import Button from '@mui/material/Button';
import Card from '@mui/material/Card';
import CardContent from '@mui/material/CardContent';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useEffect, useState, type FormEvent } from 'react';
import { useTranslation } from 'react-i18next';

import { LoadingState } from '@/components/ui/LoadingState';
import { ApiError } from '@/lib/api/types';
import { testIds } from '@/lib/testids';

import { CYCLE_TYPES, cycleTypeHelp, cycleTypeLabel, type CycleType } from './types';
import { useCyclePreferences, useUpdateCyclePreference } from './usePreferences';

/**
 * Cada cuanto presupuestas, desde Ajustes.
 *
 * <p>Se eligio en el alta, y hasta ahora no habia forma de cambiarlo sin entrar
 * a Swagger: una preferencia que gobierna las fechas de todo el producto y que
 * quedaba congelada en la primera decision.
 */
export function CyclePreferenceCard() {
  const { t } = useTranslation();
  const preferencias = useCyclePreferences();
  const guardar = useUpdateCyclePreference({ notify: true });

  const [tipo, setTipo] = useState<CycleType>('BIWEEKLY');
  const [dia, setDia] = useState('1');

  useEffect(() => {
    if (!preferencias.data) return;
    setTipo(preferencias.data.budgetCycleType);
    setDia(String(preferencias.data.cycleAnchorDay));
  }, [preferencias.data]);

  const apiError = guardar.error instanceof ApiError ? guardar.error : null;
  const fieldErrors = apiError?.fieldErrorMap ?? {};
  const generalMessage = apiError && apiError.fieldErrors.length === 0 ? apiError.message : null;

  const actual = preferencias.data;
  const sinCambios =
    actual !== undefined &&
    actual.budgetCycleType === tipo &&
    actual.cycleAnchorDay === Number(dia);

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    guardar.mutate({ cycleType: tipo, anchorDay: Number(dia) });
  }

  return (
    <Card data-testid={testIds.settings.cycleCard}>
      <CardContent>
        <Box component="form" onSubmit={handleSubmit} noValidate>
          <Stack spacing={4}>
            <Stack spacing={1.5}>
              <Typography variant="h3">{t('settings.cycle.title')}</Typography>
              <Typography variant="body2" color="text.secondary">
                {t('settings.cycle.description')}
              </Typography>
            </Stack>

            {preferencias.isPending && <LoadingState rows={2} />}

            {generalMessage && (
              <Alert severity="error" data-testid={testIds.settings.cycleError}>
                {generalMessage}
              </Alert>
            )}

            {actual && (
              <>
                <Stack direction={{ xs: 'column', sm: 'row' }} spacing={3}>
                  <TextField
                    select
                    label={t('settings.cycle.typeLabel')}
                    value={tipo}
                    onChange={(event) => setTipo(event.target.value as CycleType)}
                    helperText={cycleTypeHelp(tipo)}
                    disabled={guardar.isPending}
                    fullWidth
                    data-testid={testIds.settings.cycleType}
                  >
                    {CYCLE_TYPES.map((value) => (
                      <MenuItem key={value} value={value}>
                        {cycleTypeLabel(value)}
                      </MenuItem>
                    ))}
                  </TextField>

                  <TextField
                    label={t('settings.cycle.dayLabel')}
                    type="number"
                    value={dia}
                    onChange={(event) => setDia(event.target.value)}
                    error={Boolean(fieldErrors.anchorDay)}
                    helperText={fieldErrors.anchorDay ?? t('settings.cycle.dayHelp')}
                    disabled={guardar.isPending}
                    sx={{ width: { xs: '100%', sm: 200 } }}
                    slotProps={{
                      htmlInput: {
                        'data-testid': testIds.settings.cycleAnchor,
                        min: 1,
                        max: 31,
                      },
                    }}
                  />
                </Stack>

                <Box>
                  <Button
                    type="submit"
                    variant="contained"
                    disabled={guardar.isPending || sinCambios}
                    data-testid={testIds.settings.cycleSubmit}
                  >
                    {guardar.isPending ? t('common.oneMoment') : t('settings.cycle.submit')}
                  </Button>
                </Box>
              </>
            )}
          </Stack>
        </Box>
      </CardContent>
    </Card>
  );
}
