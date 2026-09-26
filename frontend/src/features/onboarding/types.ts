/*
 * Lo del ciclo vive en features/preferences: no es del asistente sino una
 * preferencia de la cuenta, y Ajustes la usa igual. Aqui solo queda lo que es
 * propio del alta.
 */

import i18n from '@/i18n';

/** Debe coincidir con com.luma.onboarding.application.OnboardingService.Step. */
export const STEPS = ['CYCLE', 'INCOMES', 'EXPENSES', 'SAVINGS', 'SUMMARY'] as const;
export type Step = (typeof STEPS)[number];

export function stepLabel(value: Step): string {
  return i18n.t(`onboarding.stepLabels.${value}`);
}

/** Solo los ingresos son obligatorios. El resto se puede saltar. */
export const OPTIONAL_STEPS: ReadonlySet<Step> = new Set<Step>(['EXPENSES', 'SAVINGS']);

export interface OnboardingState {
  completed: boolean;
  resumeStep: Step;
  incomeCount: number;
  expenseCount: number;
  goalCount: number;
  canFinish: boolean;
}

/** El orden en que se recorren los pasos. */
export function nextStep(current: Step): Step {
  const index = STEPS.indexOf(current);
  return STEPS[Math.min(index + 1, STEPS.length - 1)] as Step;
}

export function previousStep(current: Step): Step {
  const index = STEPS.indexOf(current);
  return STEPS[Math.max(index - 1, 0)] as Step;
}
