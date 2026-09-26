import { api } from '@/lib/api/client';

import type { CycleItem } from '../review/types';

import type { CycleSummary, CycleTrend, DeficitAdvice } from './types';

export const budgetKeys = {
  all: ['budget'] as const,
  current: ['budget', 'current'] as const,
  items: (cycleId: string) => ['budget', 'items', cycleId] as const,
  advice: (cycleId: string) => ['budget', 'advice', cycleId] as const,
  trends: (howMany: number) => ['budget', 'trends', howMany] as const,
};

export async function fetchCurrentSummary(): Promise<CycleSummary> {
  const { data } = await api.get<CycleSummary>('/budget-cycles/current');
  return data;
}

export async function fetchCycleItems(cycleId: string): Promise<CycleItem[]> {
  const { data } = await api.get<CycleItem[]>(`/budget-cycles/${cycleId}/items`);
  return data;
}

export async function fetchDeficitAdvice(cycleId: string): Promise<DeficitAdvice> {
  const { data } = await api.get<DeficitAdvice>(`/budget-cycles/${cycleId}/advice`);
  return data;
}

export async function fetchTrends(howMany: number): Promise<CycleTrend[]> {
  const { data } = await api.get<CycleTrend[]>('/budget-cycles/trends', {
    params: { cycles: howMany },
  });
  return data;
}

/** Abre el siguiente ciclo y materializa sus renglones. */
export async function openNextCycle(): Promise<CycleSummary> {
  const { data } = await api.post<CycleSummary>('/budget-cycles');
  return data;
}

/** Cierra un ciclo. Irreversible: queda inmutable. */
export async function closeCycle(cycleId: string): Promise<void> {
  await api.post(`/budget-cycles/${cycleId}/close`);
}

export async function skipItem(cycleId: string, itemId: string): Promise<CycleItem> {
  const { data } = await api.delete<CycleItem>(`/budget-cycles/${cycleId}/items/${itemId}`);
  return data;
}
