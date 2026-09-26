import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

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
    (payload) => `Gasto "${payload.name}" capturado.`,
  );
}

export function useUpdateExpense() {
  return useExpenseMutation(
    ({ id, payload }: { id: string; payload: ExpensePayload }) => updateExpense(id, payload),
    ({ payload }) => `Gasto "${payload.name}" actualizado. Aplica desde el siguiente ciclo.`,
  );
}

export function useSetExpenseActive() {
  return useExpenseMutation(
    ({ id, active }: { id: string; active: boolean }) => setExpenseActive(id, active),
    ({ active }, result) =>
      active
        ? `"${result.name}" vuelve a contar en tus ciclos.`
        : `"${result.name}" deja de contar en los ciclos siguientes.`,
  );
}

export function useDeleteExpense() {
  return useExpenseMutation(
    ({ id }: { id: string; name: string }) => deleteExpense(id),
    ({ name }) => `Gasto "${name}" eliminado.`,
  );
}
