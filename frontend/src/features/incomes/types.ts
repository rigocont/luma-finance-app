import type { MoneyValue } from '@/lib/money';

/** Debe coincidir con com.luma.income.domain.IncomeType. */
export const INCOME_TYPES = [
  'RECURRENT',
  'VARIABLE',
  'BONUS',
  'AGUINALDO',
  'SALE',
  'OTHER',
] as const;

export type IncomeType = (typeof INCOME_TYPES)[number];

/** Debe coincidir con com.luma.budget.domain.Frequency. */
export const FREQUENCIES = ['BIWEEKLY', 'MONTHLY', 'BIMONTHLY', 'ANNUAL', 'ONE_TIME'] as const;

export type Frequency = (typeof FREQUENCIES)[number];

export const INCOME_SORTS = ['NEWEST', 'NAME', 'AMOUNT_DESC', 'AMOUNT_ASC', 'START_DATE'] as const;

export type IncomeSort = (typeof INCOME_SORTS)[number];

export interface Income {
  id: string;
  name: string;
  type: IncomeType;
  amount: MoneyValue;
  frequency: Frequency;
  expectedDay: number | null;
  startDate: string;
  endDate: string | null;
  active: boolean;
  /** El monto es una estimacion y el ciclo pedira confirmarlo. */
  requiresReview: boolean;
  notes: string | null;
  createdAt: string;
}

export interface IncomePayload {
  name: string;
  type: IncomeType;
  amount: string;
  frequency: Frequency;
  expectedDay: number | null;
  startDate: string;
  endDate: string | null;
  notes: string | null;
}

export interface IncomeFilters {
  type?: IncomeType;
  active?: boolean;
  sort: IncomeSort;
  page: number;
  size: number;
}

import i18n from '@/i18n';

/**
 * La API devuelve codigos estables; la pantalla habla en el idioma elegido
 * (Fase 15).
 *
 * La traduccion vive aqui y no repartida por los componentes: un enum nuevo en
 * el backend se traduce en un solo lugar.
 */
export function incomeTypeLabel(value: IncomeType): string {
  return i18n.t(`incomes.types.${value}`);
}

export function frequencyLabel(value: Frequency): string {
  return i18n.t(`common.frequency.${value}`);
}

export function incomeSortLabel(value: IncomeSort): string {
  return i18n.t(`incomes.sort.${value}`);
}

/**
 * Las frecuencias anual y de una sola vez se resuelven con la fecha de inicio,
 * asi que pedir un dia del mes solo confundiria.
 */
export function needsExpectedDay(frequency: Frequency): boolean {
  return frequency !== 'ANNUAL' && frequency !== 'ONE_TIME';
}

/** Misma regla que IncomeType.requiresReview() en el backend. */
export function typeRequiresReview(type: IncomeType): boolean {
  return type === 'VARIABLE' || type === 'SALE';
}
