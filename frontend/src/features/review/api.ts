import { api } from '@/lib/api/client';

import type {
  BudgetCycleSummary,
  ConfirmAmountsPayload,
  CycleItem,
  ItemHistory,
  ReviewItem,
  SettlePayload,
} from './types';

export const reviewKeys = {
  all: ['review'] as const,
  cycle: ['review', 'cycle'] as const,
  pending: ['review', 'pending'] as const,
  savings: (cycleId: string) => ['review', 'savings', cycleId] as const,
  history: (cycleId: string, itemId: string) => ['review', 'history', cycleId, itemId] as const,
};

/**
 * El ciclo en curso.
 *
 * Vive aqui y no en un modulo de ciclos propio porque esta es, por ahora, la
 * unica pantalla que lo necesita. Cuando llegue el resumen del presupuesto
 * (Fase 10) se mudara con el resto.
 */
export async function fetchCurrentCycle(): Promise<BudgetCycleSummary> {
  const { data } = await api.get<{ cycle: BudgetCycleSummary }>('/budget-cycles/current');
  return data.cycle;
}

export async function fetchPendingReview(): Promise<ReviewItem[]> {
  const { data } = await api.get<ReviewItem[]>('/budget-cycles/current/review');
  return data;
}

export async function fetchItemHistory(cycleId: string, itemId: string): Promise<ItemHistory> {
  const { data } = await api.get<ItemHistory>(`/budget-cycles/${cycleId}/items/${itemId}/history`);
  return data;
}

export async function fetchSavingItems(cycleId: string): Promise<CycleItem[]> {
  const { data } = await api.get<CycleItem[]>(`/budget-cycles/${cycleId}/items`, {
    params: { type: 'SAVING' },
  });
  return data;
}

/** Manda el lote completo: el servidor lo aplica entero o no lo aplica. */
export async function confirmAmounts(
  cycleId: string,
  payload: ConfirmAmountsPayload,
): Promise<CycleItem[]> {
  const { data } = await api.post<CycleItem[]>(
    `/budget-cycles/${cycleId}/items/confirm-amounts`,
    payload,
  );
  return data;
}

export async function settleItem(
  cycleId: string,
  itemId: string,
  payload: SettlePayload,
): Promise<CycleItem> {
  const { data } = await api.post<CycleItem>(
    `/budget-cycles/${cycleId}/items/${itemId}/settle`,
    payload,
  );
  return data;
}
