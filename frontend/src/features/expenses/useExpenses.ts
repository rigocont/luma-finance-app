import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import i18n from '@/i18n';
import { showToast } from '@/store/toastStore';

import {
  createExpense,
  deleteExpense,
  expenseKeys,
  fetchCategories,
  fetchExpenses,
  setExpenseActive,
  updateExpense,
} from './api';
import type { ExpenseFilters, ExpensePayload } from './types';

/**
 * El catalogo casi nunca cambia: hoy es de solo lectura y sale de una semilla.
 * Una hora de frescura evita pedirlo en cada visita a la pantalla.
 */
export function useExpenseCategories() {
  return useQuery({
    queryKey: expenseKeys.categories,
    queryFn: fetchCategories,
    staleTime: 60 * 60 * 1000,
  });
}

export function useExpenses(filters: ExpenseFilters) {
  return useQuery({
    queryKey: expenseKeys.list(filters),
    queryFn: () => fetchExpenses(filters),
    placeholderData: (previous) => previous,
  });
}

function useExpenseMutation<TVariables, TResult>(
  mutationFn: (variables: TVariables) => Promise<TResult>,
  successMessage: (variables: TVariables, result: TResult) => string,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn,
    onSuccess: async (result, variables) => {
      // Se invalidan las LISTAS, no el catalogo: capturar un gasto no cambia
      // las categorias, y volver a pedirlas seria una peticion de mas en cada
      // captura.
      await queryClient.invalidateQueries({ queryKey: expenseKeys.all, exact: false });
      showToast(successMessage(variables, result));
    },
  });
}

export function useCreateExpense() {
  return useExpenseMutation(
    (payload: ExpensePayload) => createExpense(payload),
    (payload) => i18n.t('expenses.toast.created', { name: payload.name }),
  );
}

export function useUpdateExpense() {
  return useExpenseMutation(
    ({ id, payload }: { id: string; payload: ExpensePayload }) => updateExpense(id, payload),
    ({ payload }) => i18n.t('expenses.toast.updated', { name: payload.name }),
  );
}

export function useSetExpenseActive() {
  return useExpenseMutation(
    ({ id, active }: { id: string; active: boolean }) => setExpenseActive(id, active),
    ({ active }, result) =>
      active
        ? i18n.t('expenses.toast.activated', { name: result.name })
        : i18n.t('expenses.toast.deactivated', { name: result.name }),
  );
}

export function useDeleteExpense() {
  return useExpenseMutation(
    ({ id }: { id: string; name: string }) => deleteExpense(id),
    ({ name }) => i18n.t('expenses.toast.deleted', { name }),
  );
}
