import Alert from '@mui/material/Alert';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useEffect, useState } from 'react';
import { useTranslation } from 'react-i18next';

import { LoadingState } from '@/components/ui/LoadingState';
import { testIds } from '@/lib/testids';

import {
  CYCLE_TYPES,
  cycleTypeHelp,
  cycleTypeLabel,
  type CycleType,
} from '../../preferences/types';
import { useCyclePreferences, useUpdateCyclePreference } from '../../preferences/usePreferences';

interface CycleStepProps {
  /** Se le entrega el guardado para que el pie del asistente lo dispare. */
  onRegisterSave: (save: (() => Promise<void>) | null) => void;
}

/**
 * Cada cuanto presupuestas y desde que dia.
 *
 * <p>Va primero porque decide las fechas de todo lo demas: capturar un sueldo
 * antes de saber si el ciclo es quincenal o mensual obliga a repensarlo despues.
 *
 * <p>Nunca bloquea: ya hay valores por omision, asi que este paso siempre se
 * puede pasar. Guardar solo llama al servidor si algo cambio.
 */
export function CycleStep({ onRegisterSave }: CycleStepProps) {
  const { t } = useTranslation();
  const preferencias = useCyclePreferences();
  const guardar = useUpdateCyclePreference();

  const [tipo, setTipo] = useState<CycleType>('BIWEEKLY');
  const [dia, setDia] = useState('1');

  useEffect(() => {
    if (!preferencias.data) return;
    setTipo(preferencias.data.budgetCycleType);
    setDia(String(preferencias.data.cycleAnchorDay));
  }, [preferencias.data]);

  useEffect(() => {
    onRegisterSave(async () => {
      const diaNumero = Number(dia);
      const actual = preferencias.data;

      // Si no cambio nada no se manda la peticion: avanzar por el asistente no
      // tiene por que escribir en la base.
      if (actual && actual.budgetCycleType === tipo && actual.cycleAnchorDay === diaNumero) {
        return;
      }

      await guardar.mutateAsync({ cycleType: tipo, anchorDay: diaNumero });
    });

    return () => onRegisterSave(null);
  }, [tipo, dia, preferencias.data, guardar, onRegisterSave]);

  if (preferencias.isPending) {
    return <LoadingState rows={2} />;
  }

  return (
    <Stack spacing={5} data-testid={testIds.onboarding.step('cycle')}>
      <Typography variant="body1" color="text.secondary">
        {t('onboarding.cycle.intro')}
      </Typography>

      <TextField
        select
        label={t('onboarding.cycle.typeLabel')}
        value={tipo}
        onChange={(event) => setTipo(event.target.value as CycleType)}
        helperText={cycleTypeHelp(tipo)}
        fullWidth
        data-testid={testIds.onboarding.cycleTypeInput}
      >
        {CYCLE_TYPES.map((value) => (
          <MenuItem key={value} value={value}>
            {cycleTypeLabel(value)}
          </MenuItem>
        ))}
      </TextField>

      <TextField
        label={t('onboarding.cycle.dayLabel')}
        type="number"
        value={dia}
        onChange={(event) => setDia(event.target.value)}
        helperText={t('onboarding.cycle.dayHelp')}
        fullWidth
        slotProps={{
          htmlInput: { 'data-testid': testIds.onboarding.cycleAnchorInput, min: 1, max: 31 },
        }}
      />

      <Alert severity="info" data-testid={testIds.onboarding.cyclePreview}>
        {t('onboarding.cycle.preview')}
      </Alert>
    </Stack>
  );
}
