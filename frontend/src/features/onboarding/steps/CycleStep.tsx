import Alert from '@mui/material/Alert';
import MenuItem from '@mui/material/MenuItem';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import Typography from '@mui/material/Typography';
import { useEffect, useState } from 'react';

import { LoadingState } from '@/components/ui/LoadingState';
import { testIds } from '@/lib/testids';

import {
  CYCLE_TYPES,
  CYCLE_TYPE_HELP,
  CYCLE_TYPE_LABELS,
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
        Un ciclo es el periodo que presupuestas de una vez. Casi siempre coincide con cada cuanto te
        pagan.
      </Typography>

      <TextField
        select
        label="Cada cuanto presupuestas"
        value={tipo}
        onChange={(event) => setTipo(event.target.value as CycleType)}
        helperText={CYCLE_TYPE_HELP[tipo]}
        fullWidth
        data-testid={testIds.onboarding.cycleTypeInput}
      >
        {CYCLE_TYPES.map((value) => (
          <MenuItem key={value} value={value}>
            {CYCLE_TYPE_LABELS[value]}
          </MenuItem>
        ))}
      </TextField>

      <TextField
        label="Dia en que empieza"
        type="number"
        value={dia}
        onChange={(event) => setDia(event.target.value)}
        helperText="Si pones 31, en los meses cortos cae el ultimo dia."
        fullWidth
        slotProps={{
          htmlInput: { 'data-testid': testIds.onboarding.cycleAnchorInput, min: 1, max: 31 },
        }}
      />

      <Alert severity="info" data-testid={testIds.onboarding.cyclePreview}>
        Esto lo puedes cambiar despues. El ciclo que este abierto conserva sus fechas y el cambio
        aplica al siguiente.
      </Alert>
    </Stack>
  );
}
