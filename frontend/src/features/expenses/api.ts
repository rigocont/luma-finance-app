import { api } from '@/lib/api/client';
import type { PageResponse } from '@/lib/api/types';

import type { Expense, ExpenseCategory, ExpenseFilters, ExpensePayload } from './types';

export const expenseKeys = {
  all: ['expenses'] as const,
  list: (filters: ExpenseFilters) => ['expenses', 'list', filters] as const,
  categories: ['expenses', 'categories'] as const,
};

export async function fetchCategories(): Promise<ExpenseCategory[]> {
  const { data } = await api.get<ExpenseCategory[]>('/expense-categories');
  return data;
}

export async function fetchExpenses(filters: ExpenseFilters): Promise<PageResponse<Expense>> {
  const { data } = await api.get<PageResponse<Expense>>('/expenses', {
    params: {
      kind: filters.kind,
      categoryId: filters.categoryId,
      active: filters.active,
      sort: filters.sort,
      page: filters.page,
      size: filters.size,
    },
  });
  return data;
}

export async function createExpense(payload: ExpensePayload): Promise<Expense> {
  const { data } = await api.post<Expense>('/expenses', payload);
  return data;
}

export async function updateExpense(id: string, payload: ExpensePayload): Promise<Expense> {
  const { data } = await api.patch<Expense>(`/expenses/${id}`, payload);
  return data;
}

export async function setExpenseActive(id: string, active: boolean): Promise<Expense> {
  const action = active ? 'activate' : 'deactivate';
  const { data } = await api.post<Expense>(`/expenses/${id}/${action}`);
  return data;
}

export async function deleteExpense(id: string): Promise<void> {
  await api.delete(`/expenses/${id}`);
}
