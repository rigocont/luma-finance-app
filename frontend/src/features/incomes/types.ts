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

/**
 * La API devuelve codigos estables; la pantalla habla espanol normal.
 *
 * La traduccion vive aqui y no repartida por los componentes: un enum nuevo en
 * el backend se traduce en un solo lugar.
 */
export const INCOME_TYPE_LABELS: Record<IncomeType, string> = {
  RECURRENT: 'Sueldo o ingreso fijo',
  VARIABLE: 'Monto variable',
  BONUS: 'Bono',
  AGUINALDO: 'Aguinaldo',
  SALE: 'Venta',
  OTHER: 'Otro',
};

export const FREQUENCY_LABELS: Record<Frequency, string> = {
  BIWEEKLY: 'Cada quincena',
  MONTHLY: 'Cada mes',
  BIMONTHLY: 'Cada dos meses',
  ANNUAL: 'Una vez al ano',
  ONE_TIME: 'Una sola vez',
};

export const INCOME_SORT_LABELS: Record<IncomeSort, string> = {
  NEWEST: 'Mas reciente',
  NAME: 'Nombre',
  AMOUNT_DESC: 'Mayor monto',
  AMOUNT_ASC: 'Menor monto',
  START_DATE: 'Fecha de inicio',
};

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
