import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import { showToast } from '@/store/toastStore';

import {
  createGoal,
  deleteGoal,
  fetchGoals,
  fetchMovements,
  registerMovement,
  reorderGoals,
  savingsKeys,
  setGoalPaused,
  updateGoal,
} from './api';
import type { GoalStatus, MovementPayload, SavingsGoalPayload } from './types';

export function useSavingsGoals(status?: GoalStatus) {
  return useQuery({
    queryKey: savingsKeys.list(status),
    queryFn: () => fetchGoals(status),
    placeholderData: (previous) => previous,
  });
}

export function useGoalMovements(goalId: string | null) {
  return useQuery({
    queryKey: savingsKeys.movements(goalId ?? ''),
    queryFn: () => fetchMovements(goalId as string),
    // Solo se piden cuando hay una meta abierta: el historial es una vista de
    // detalle, no algo que la lista necesite.
    enabled: goalId !== null,
  });
}

function useSavingsMutation<TVariables, TResult>(
  mutationFn: (variables: TVariables) => Promise<TResult>,
  successMessage: (variables: TVariables, result: TResult) => string,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn,
    onSuccess: async (result, variables) => {
      await queryClient.invalidateQueries({ queryKey: savingsKeys.all });
      showToast(successMessage(variables, result));
    },
  });
}

export function useCreateGoal() {
  return useSavingsMutation(
    (payload: SavingsGoalPayload) => createGoal(payload),
    (payload) => `Meta "${payload.name}" creada.`,
  );
}

export function useUpdateGoal() {
  return useSavingsMutation(
    ({ id, payload }: { id: string; payload: SavingsGoalPayload }) => updateGoal(id, payload),
    ({ payload }) => `Meta "${payload.name}" actualizada.`,
  );
}

export function useRegisterMovement() {
  return useSavingsMutation(
    ({ goalId, payload }: { goalId: string; payload: MovementPayload }) =>
      registerMovement(goalId, payload),
    ({ payload }, result) => {
      if (payload.type === 'WITHDRAWAL') {
        return `Retiro registrado. Llevas ${result.saved.amount} de ${result.target.amount}.`;
      }
      // Alcanzar la meta merece decirse, no quedarse en un numero mas.
      return result.status === 'COMPLETED'
        ? `Aportacion registrada. Alcanzaste "${result.name}".`
        : `Aportacion registrada en "${result.name}".`;
    },
  );
}

export function useReorderGoals() {
  return useSavingsMutation(
    (goalIds: string[]) => reorderGoals(goalIds),
    () => 'Orden de prioridad guardado.',
  );
}

export function useSetGoalPaused() {
  return useSavingsMutation(
    ({ id, paused }: { id: string; paused: boolean }) => setGoalPaused(id, paused),
    ({ paused }, result) =>
      paused
        ? `"${result.name}" deja de restar de tus ciclos.`
        : `"${result.name}" vuelve a contar en tus ciclos.`,
  );
}

export function useDeleteGoal() {
  return useSavingsMutation(
    ({ id }: { id: string; name: string }) => deleteGoal(id),
    ({ name }) => `Meta "${name}" eliminada.`,
  );
}
