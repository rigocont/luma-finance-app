import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { useEffect } from 'react';

import i18n, { type SupportedLanguage } from '@/i18n';
import { showToast } from '@/store/toastStore';

import { fetchPreferences, preferencesKeys, updateCyclePreference, updateLanguage } from './api';
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
        showToast(i18n.t('settings.cycle.saved'));
      }
    },
  });
}

/**
 * Cambia el idioma de la interfaz.
 *
 * <p>Aplica el idioma nuevo de inmediato -sin esperar a que la mutacion
 * termine- para que el boton que se acaba de presionar responda al instante:
 * si el servidor rechaza el cambio (sin red, por ejemplo), `useLanguageSync`
 * lo va a corregir en la siguiente sincronizacion.
 */
export function useUpdateLanguage() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (uiLanguage: SupportedLanguage) => updateLanguage(uiLanguage),
    onMutate: async (uiLanguage) => {
      await i18n.changeLanguage(uiLanguage);
    },
    onSuccess: async () => {
      await queryClient.invalidateQueries({ queryKey: preferencesKeys.all });
    },
  });
}

/**
 * Sincroniza el idioma de la interfaz con el que la cuenta tiene guardado.
 *
 * <p>Vive montado una sola vez, dentro de AppShell (Fase 15): ese componente
 * solo se renderiza con sesion iniciada y alta terminada (ver
 * routes/router.tsx), asi que aqui nunca se pide esto para una cuenta anonima.
 * Antes de que exista sesion, `detectSystemLanguage` (src/i18n) ya elige algo
 * razonable a partir del navegador.
 */
export function useLanguageSync(): void {
  const preferencias = useCyclePreferences();
  const uiLanguage = preferencias.data?.uiLanguage;

  useEffect(() => {
    if (uiLanguage && uiLanguage !== i18n.language) {
      void i18n.changeLanguage(uiLanguage);
    }
  }, [uiLanguage]);
}
