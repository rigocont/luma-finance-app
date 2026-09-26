import { api } from '@/lib/api/client';

import type { CyclePreferencePayload, CyclePreferences } from './types';
import type { SupportedLanguage } from '@/i18n';

export const preferencesKeys = {
  all: ['preferences'] as const,
  cycle: ['preferences', 'cycle'] as const,
};

export async function fetchPreferences(): Promise<CyclePreferences> {
  const { data } = await api.get<CyclePreferences>('/users/me/preferences');
  return data;
}

/** Aplica al SIGUIENTE ciclo. El que este abierto conserva su periodo. */
export async function updateCyclePreference(
  payload: CyclePreferencePayload,
): Promise<CyclePreferences> {
  const { data } = await api.patch<CyclePreferences>('/users/me/preferences/cycle', payload);
  return data;
}

/** Se guarda en la cuenta: el idioma elegido se recuerda tambien en otro dispositivo. */
export async function updateLanguage(uiLanguage: SupportedLanguage): Promise<CyclePreferences> {
  const { data } = await api.patch<CyclePreferences>('/users/me/preferences/language', {
    uiLanguage,
  });
  return data;
}
