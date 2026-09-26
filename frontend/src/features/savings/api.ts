import { api } from '@/lib/api/client';

import type {
  GoalStatus,
  MovementPayload,
  SavingsGoal,
  SavingsGoalPayload,
  SavingsMovement,
} from './types';

export const savingsKeys = {
  all: ['savings'] as const,
  list: (status?: GoalStatus) => ['savings', 'list', status ?? 'all'] as const,
  movements: (goalId: string) => ['savings', 'movements', goalId] as const,
};

export async function fetchGoals(status?: GoalStatus): Promise<SavingsGoal[]> {
  const { data } = await api.get<SavingsGoal[]>('/savings-goals', { params: { status } });
  return data;
}

export async function fetchMovements(goalId: string): Promise<SavingsMovement[]> {
  const { data } = await api.get<SavingsMovement[]>(`/savings-goals/${goalId}/movements`);
  return data;
}

export async function createGoal(payload: SavingsGoalPayload): Promise<SavingsGoal> {
  const { data } = await api.post<SavingsGoal>('/savings-goals', payload);
  return data;
}

export async function updateGoal(id: string, payload: SavingsGoalPayload): Promise<SavingsGoal> {
  const { data } = await api.patch<SavingsGoal>(`/savings-goals/${id}`, payload);
  return data;
}

export async function registerMovement(
  goalId: string,
  payload: MovementPayload,
): Promise<SavingsGoal> {
  const { data } = await api.post<SavingsGoal>(`/savings-goals/${goalId}/movements`, payload);
  return data;
}

/** Manda el orden COMPLETO: el servidor reasigna todas las prioridades. */
export async function reorderGoals(goalIds: string[]): Promise<SavingsGoal[]> {
  const { data } = await api.put<SavingsGoal[]>('/savings-goals/order', { goalIds });
  return data;
}

export async function setGoalPaused(id: string, paused: boolean): Promise<SavingsGoal> {
  const action = paused ? 'pause' : 'resume';
  const { data } = await api.post<SavingsGoal>(`/savings-goals/${id}/${action}`);
  return data;
}

export async function deleteGoal(id: string): Promise<void> {
  await api.delete(`/savings-goals/${id}`);
}
