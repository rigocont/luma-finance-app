import i18n from '@/i18n';

/** Debe coincidir con com.luma.budget.domain.CycleType. */
export const CYCLE_TYPES = ['BIWEEKLY', 'MONTHLY', 'BIMONTHLY'] as const;
export type CycleType = (typeof CYCLE_TYPES)[number];

/**
 * La API devuelve codigos estables; la pantalla habla en el idioma elegido
 * (Fase 15). Se usa `i18n.t` directamente -no el hook- porque este archivo
 * no es un componente; el componente que llama a esta funcion ya se
 * resuscribe al idioma con su propio `useTranslation`.
 */
export function cycleTypeLabel(value: CycleType): string {
  return i18n.t(`settings.cycle.types.${value}`);
}

export function cycleTypeHelp(value: CycleType): string {
  return i18n.t(`settings.cycle.help.${value}`);
}

export interface CyclePreferences {
  currency: string;
  locale: string;
  timezone: string;
  budgetCycleType: CycleType;
  cycleAnchorDay: number;
  expenseAllocationPolicy: string;
  /**
   * "es" o "en" (Fase 15). Vive en la cuenta, no en este navegador: es lo que
   * permite que el idioma elegido se recuerde tambien en otro dispositivo.
   * Distinto de `locale`, que gobierna el formato de numeros y fechas.
   */
  uiLanguage: string;
}

export interface CyclePreferencePayload {
  cycleType: CycleType;
  anchorDay: number;
}
