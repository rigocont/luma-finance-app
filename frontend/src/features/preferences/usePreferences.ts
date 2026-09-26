import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { showToast } from '@/store/toastStore';

import { fetchPreferences, preferencesKeys, updateCyclePreference } from './api';
import type { CyclePreferencePayload } from './types';

export function useCyclePreferences() {
  return useQuery({
    queryKey: preferencesKeys.cycle,
    queryFn: fetchPreferences,
  });
}

/**
 * Cambia el tipo de ciclo.
 *
 * <p>El aviso es opcional porque hay dos usos con expectativas distintas: en
 * Ajustes la persona presiono "Guardar" y espera confirmacion; en el asistente
 * esto ocurre al avanzar de paso, y un aviso ahi seria ruido sobre una accion
 * que nadie pidio por separado.
 */
export function useUpdateCyclePreference(options: { notify?: boolean } = {}) {
  const { notify = false } = options;
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (payload: CyclePreferencePayload) => updateCyclePreference(payload),
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: preferencesKeys.all });

      if (notify) {
        showToast('Listo. El cambio aplica a tu siguiente ciclo.');
      }
    },
  });
}
