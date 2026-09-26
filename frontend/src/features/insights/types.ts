import type { MoneyValue } from '@/lib/money';

/**
 * El analisis financiero sin IA (Fase 13, parte determinista).
 *
 * La capa de redaccion/priorizacion con LLM (ver docs/00-arquitectura-fase-0.md,
 * S8.4) queda pendiente para una fase futura. Estos tres campos ya son utiles
 * sin ella, y ninguno se inventa: si no hay con que respaldarlo, no aparece.
 */

export interface GoalShare {
  goalId: string;
  goalName: string;
  amount: MoneyValue;
}

/** Nulo cuando el ciclo no esta en deficit, o cuando no hay ciclo anterior con que comparar. */
export interface DeficitCause {
  cycleId: string;
  missing: MoneyValue;
  categoryName: string;
  previousAmount: MoneyValue;
  currentAmount: MoneyValue;
  increase: MoneyValue;
}

/** Nulo cuando el ciclo no tiene remanente. `shares` viene vacia sin metas activas. */
export interface SurplusAllocation {
  cycleId: string;
  surplus: MoneyValue;
  shares: GoalShare[];
}

/** Una categoria con al menos 3 ciclos seguidos al alza. */
export interface CategoryGrowth {
  categoryName: string;
  firstAmount: MoneyValue;
  lastAmount: MoneyValue;
  cycles: number;
}

export interface FinancialInsights {
  deficitCause: DeficitCause | null;
  surplusAllocation: SurplusAllocation | null;
  categoryGrowth: CategoryGrowth[];
}
