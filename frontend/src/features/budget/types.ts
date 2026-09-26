import i18n from '@/i18n';
import type { MoneyValue } from '@/lib/money';

import type { CycleItem, CyclePeriod } from '../review/types';

/** Debe coincidir con com.luma.budget.domain.BudgetState. */
export const BUDGET_STATES = ['DEFICIT', 'BALANCED', 'SURPLUS'] as const;
export type BudgetState = (typeof BUDGET_STATES)[number];

export interface BudgetTotals {
  income: MoneyValue;
  fixedExpenses: MoneyValue;
  variableExpenses: MoneyValue;
  savings: MoneyValue;
  balance: MoneyValue;
  totalOutflow: MoneyValue;
}

export interface CycleCounts {
  total: number;
  settled: number;
  pending: number;
  overdue: number;
  needsReview: number;
  skipped: number;
}

export interface BudgetBalance {
  planned: BudgetTotals;
  actual: BudgetTotals;
  state: BudgetState;
  /** De 0 a 1. El servidor lo calcula; aqui solo se presenta. */
  savingsRate: number;
  expenseRate: number;
  counts: CycleCounts;
  requiresReview: boolean;
  hasOverduePayments: boolean;
}

export interface BudgetCycleSummary {
  id: string;
  period: CyclePeriod;
  status: string;
  sequenceNumber: number;
  closedAt: string | null;
}

export interface CycleSummary {
  cycle: BudgetCycleSummary;
  balance: BudgetBalance;
}

/**
 * Como cambio una cifra respecto al ciclo anterior.
 *
 * Llega con la direccion aparte y la magnitud en positivo: asi la interfaz elige
 * la frase sin restar ni sacar valores absolutos. Ninguna cifra de dinero se
 * deriva aqui.
 */
export interface Change {
  direction: 'UP' | 'DOWN' | 'SAME';
  amount: MoneyValue;
}

export interface CycleTrend {
  cycleId: string;
  period: CyclePeriod;
  status: string;
  sequenceNumber: number;
  state: BudgetState;
  planned: BudgetTotals;
  actual: BudgetTotals;
  /** Nulo en el ciclo mas antiguo de la lista: no tiene con que compararse. */
  outflowChange: Change | null;
}

export interface CutSuggestion {
  itemId: string;
  name: string;
  itemType: CycleItem['itemType'];
  status: CycleItem['status'];
  flexibility: string | null;
  amount: MoneyValue;
  /** El monto es una estimacion sin confirmar. */
  estimated: boolean;
}

export interface DeficitAdvice {
  missing: MoneyValue;
  covered: MoneyValue;
  /** Si los recortes propuestos alcanzan. Cuando es falso hay que decirlo. */
  coversTheGap: boolean;
  cuts: CutSuggestion[];
}

/**
 * El estado del ciclo en lenguaje normal.
 *
 * La API devuelve un codigo estable —DEFICIT, BALANCED, SURPLUS— y la traduccion
 * vive aqui a proposito: lo que una persona necesita leer cambia con el producto,
 * el contrato de la API no. Sigue el idioma de la interfaz desde la Fase 15.
 */
export function stateHeadline(state: BudgetState): string {
  return i18n.t(`dashboard.hero.state.${state}`);
}

export const STATE_TONE: Record<BudgetState, 'positive' | 'neutral' | 'negative'> = {
  SURPLUS: 'positive',
  BALANCED: 'neutral',
  DEFICIT: 'negative',
};

/** El porcentaje entero que se muestra junto a una proporcion. */
export function ratePercent(rate: number): number {
  return Math.round(rate * 100);
}
