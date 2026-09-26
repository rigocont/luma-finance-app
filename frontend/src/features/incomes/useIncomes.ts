import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';

import i18n from '@/i18n';
import { showToast } from '@/store/toastStore';

import {
  createIncome,
  deleteIncome,
  fetchIncomes,
  incomeKeys,
  setIncomeActive,
  updateIncome,
} from './api';
import type { IncomeFilters, IncomePayload } from './types';

export function useIncomes(filters: IncomeFilters) {
  return useQuery({
    queryKey: incomeKeys.list(filters),
    queryFn: () => fetchIncomes(filters),
    // Mantiene la pagina anterior visible mientras llega la siguiente, en lugar
    // de vaciar la tabla en cada cambio de filtro.
    placeholderData: (previous) => previous,
  });
}

/**
 * Las mutaciones invalidan TODA la rama de ingresos, no solo la pagina actual.
 *
 * Es deliberado: crear un ingreso puede cambiar en que pagina cae cada uno
 * segun el orden elegido, y cualquier filtro guardado en cache queda obsoleto.
 * Invalidar de mas cuesta una peticion; invalidar de menos deja a la persona
 * viendo datos que ya no son ciertos.
 */
function useIncomeMutation<TVariables, TResult>(
  mutationFn: (variables: TVariables) => Promise<TResult>,
  successMessage: (variables: TVariables, result: TResult) => string,
) {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn,
    onSuccess: async (result, variables) => {
      await queryClient.invalidateQueries({ queryKey: incomeKeys.all });
      showToast(successMessage(variables, result));
    },
  });
}

export function useCreateIncome() {
  return useIncomeMutation(
    (payload: IncomePayload) => createIncome(payload),
    // El aviso confirma en los mismos terminos que el boton, y avisa del
    // efecto que la persona no pidio explicitamente pero si ocurrio.
    (payload) => i18n.t('incomes.toast.created', { name: payload.name }),
  );
}

export function useUpdateIncome() {
  return useIncomeMutation(
    ({ id, payload }: { id: string; payload: IncomePayload }) => updateIncome(id, payload),
    ({ payload }) => i18n.t('incomes.toast.updated', { name: payload.name }),
  );
}

export function useSetIncomeActive() {
  return useIncomeMutation(
    ({ id, active }: { id: string; active: boolean }) => setIncomeActive(id, active),
    ({ active }, result) =>
      active
        ? i18n.t('incomes.toast.activated', { name: result.name })
        : i18n.t('incomes.toast.deactivated', { name: result.name }),
  );
}

export function useDeleteIncome() {
  return useIncomeMutation(
    ({ id }: { id: string; name: string }) => deleteIncome(id),
    ({ name }) => i18n.t('incomes.toast.deleted', { name }),
  );
}
