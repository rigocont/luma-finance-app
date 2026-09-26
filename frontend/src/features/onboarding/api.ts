import { api } from '@/lib/api/client';

import type { OnboardingState } from './types';

export const onboardingKeys = {
  all: ['onboarding'] as const,
  state: ['onboarding', 'state'] as const,
};

export async function fetchOnboardingState(): Promise<OnboardingState> {
  const { data } = await api.get<OnboardingState>('/onboarding/state');
  return data;
}

/** Termina el alta y abre el primer ciclo. Exige al menos un ingreso. */
export async function completeOnboarding(): Promise<void> {
  await api.post('/onboarding/complete');
}

/** Da el alta por terminada sin configurar nada. Conserva lo capturado. */
export async function skipOnboarding(): Promise<void> {
  await api.post('/onboarding/skip');
}
