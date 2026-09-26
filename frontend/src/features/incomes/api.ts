import { api } from '@/lib/api/client';
import type { PageResponse } from '@/lib/api/types';

import type { Income, IncomeFilters, IncomePayload } from './types';

/**
 * Claves de cache de TanStack Query.
 *
 * Se construyen a partir de los filtros para que cada combinacion tenga su
 * propia entrada, y para poder invalidar todo el modulo con la clave raiz
 * despues de crear, editar o eliminar.
 */
export const incomeKeys = {
  all: ['incomes'] as const,
  list: (filters: IncomeFilters) => ['incomes', 'list', filters] as const,
};

export async function fetchIncomes(filters: IncomeFilters): Promise<PageResponse<Income>> {
  const { data } = await api.get<PageResponse<Income>>('/incomes', {
    params: {
      type: filters.type,
      active: filters.active,
      sort: filters.sort,
      page: filters.page,
      size: filters.size,
    },
  });
  return data;
}

export async function createIncome(payload: IncomePayload): Promise<Income> {
  const { data } = await api.post<Income>('/incomes', payload);
  return data;
}

export async function updateIncome(id: string, payload: IncomePayload): Promise<Income> {
  const { data } = await api.patch<Income>(`/incomes/${id}`, payload);
  return data;
}

export async function setIncomeActive(id: string, active: boolean): Promise<Income> {
  const action = active ? 'activate' : 'deactivate';
  const { data } = await api.post<Income>(`/incomes/${id}/${action}`);
  return data;
}

export async function deleteIncome(id: string): Promise<void> {
  await api.delete(`/incomes/${id}`);
}
