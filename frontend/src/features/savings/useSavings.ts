import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import i18n from '@/i18n';
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
    (payload) => i18n.t('savings.toast.created', { name: payload.name }),
  );
}

export function useUpdateGoal() {
  return useSavingsMutation(
    ({ id, payload }: { id: string; payload: SavingsGoalPayload }) => updateGoal(id, payload),
    ({ payload }) => i18n.t('savings.toast.updated', { name: payload.name }),
  );
}

export function useRegisterMovement() {
  return useSavingsMutation(
    ({ goalId, payload }: { goalId: string; payload: MovementPayload }) =>
      registerMovement(goalId, payload),
    ({ payload }, result) => {
      if (payload.type === 'WITHDRAWAL') {
        return i18n.t('savings.toast.withdrawal', {
          saved: result.saved.amount,
          target: result.target.amount,
        });
      }
      // Alcanzar la meta merece decirse, no quedarse en un numero mas.
      return result.status === 'COMPLETED'
        ? i18n.t('savings.toast.contributionCompleted', { name: result.name })
        : i18n.t('savings.toast.contribution', { name: result.name });
    },
  );
}

export function useReorderGoals() {
  return useSavingsMutation(
    (goalIds: string[]) => reorderGoals(goalIds),
    () => i18n.t('savings.toast.reordered'),
  );
}

export function useSetGoalPaused() {
  return useSavingsMutation(
    ({ id, paused }: { id: string; paused: boolean }) => setGoalPaused(id, paused),
    ({ paused }, result) =>
      paused
        ? i18n.t('savings.toast.paused', { name: result.name })
        : i18n.t('savings.toast.resumed', { name: result.name }),
  );
}

export function useDeleteGoal() {
  return useSavingsMutation(
    ({ id }: { id: string; name: string }) => deleteGoal(id),
    ({ name }) => i18n.t('savings.toast.deleted', { name }),
  );
}
