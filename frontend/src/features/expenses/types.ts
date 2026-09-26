import type { MoneyValue } from '@/lib/money';

/** Debe coincidir con com.luma.expenses.domain.ExpenseKind. */
export const EXPENSE_KINDS = ['FIXED', 'VARIABLE'] as const;
export type ExpenseKind = (typeof EXPENSE_KINDS)[number];

/** Debe coincidir con com.luma.budget.domain.Flexibility. */
export const FLEXIBILITIES = ['CRITICAL', 'IMPORTANT', 'FLEXIBLE'] as const;
export type Flexibility = (typeof FLEXIBILITIES)[number];

export const FREQUENCIES = ['BIWEEKLY', 'MONTHLY', 'BIMONTHLY', 'ANNUAL', 'ONE_TIME'] as const;
export type Frequency = (typeof FREQUENCIES)[number];

export const EXPENSE_SORTS = ['NEWEST', 'NAME', 'AMOUNT_DESC', 'AMOUNT_ASC', 'DUE_DAY'] as const;
export type ExpenseSort = (typeof EXPENSE_SORTS)[number];

export interface ExpenseCategory {
  id: string;
  code: string;
  name: string;
  icon: string | null;
  color: string | null;
  defaultKind: ExpenseKind;
  system: boolean;
}

export interface Expense {
  id: string;
  name: string;
  kind: ExpenseKind;
  amount: MoneyValue;
  category: ExpenseCategory | null;
  flexibility: Flexibility;
  frequency: Frequency;
  dueDay: number | null;
  startDate: string;
  endDate: string | null;
  active: boolean;
  requiresReview: boolean;
  notes: string | null;
  createdAt: string;
}

export interface ExpensePayload {
  name: string;
  kind: ExpenseKind;
  amount: string;
  categoryId: string | null;
  clearCategory?: boolean;
  flexibility: Flexibility;
  frequency: Frequency;
  dueDay: number | null;
  startDate: string;
  endDate: string | null;
  notes: string | null;
}

export interface ExpenseFilters {
  kind?: ExpenseKind;
  categoryId?: string;
  active?: boolean;
  sort: ExpenseSort;
  page: number;
  size: number;
}

export const EXPENSE_KIND_LABELS: Record<ExpenseKind, string> = {
  FIXED: 'Monto estable',
  VARIABLE: 'Monto que cambia',
};

/**
 * La flexibilidad no es decorativa: es lo que impide que el analisis sugiera
 * retrasar la renta. Las etiquetas lo dicen en terminos de consecuencia, no de
 * categoria abstracta.
 */
export const FLEXIBILITY_LABELS: Record<Flexibility, string> = {
  CRITICAL: 'No se puede mover',
  IMPORTANT: 'Se puede mover con consecuencias',
  FLEXIBLE: 'Se puede posponer o recortar',
};

export const FLEXIBILITY_SHORT: Record<Flexibility, string> = {
  CRITICAL: 'Critico',
  IMPORTANT: 'Importante',
  FLEXIBLE: 'Flexible',
};

export const FREQUENCY_LABELS: Record<Frequency, string> = {
  BIWEEKLY: 'Cada quincena',
  MONTHLY: 'Cada mes',
  BIMONTHLY: 'Cada dos meses',
  ANNUAL: 'Una vez al ano',
  ONE_TIME: 'Una sola vez',
};

export const EXPENSE_SORT_LABELS: Record<ExpenseSort, string> = {
  NEWEST: 'Mas reciente',
  NAME: 'Nombre',
  AMOUNT_DESC: 'Mayor monto',
  AMOUNT_ASC: 'Menor monto',
  DUE_DAY: 'Dia de pago',
};

export function needsDueDay(frequency: Frequency): boolean {
  return frequency !== 'ANNUAL' && frequency !== 'ONE_TIME';
}
