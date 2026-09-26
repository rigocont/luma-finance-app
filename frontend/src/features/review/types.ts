import type { MoneyValue } from '@/lib/money';

/** Debe coincidir con com.luma.budget.domain.ItemStatus. */
export type ItemStatus = 'NEEDS_REVIEW' | 'PENDING' | 'PAID' | 'PARTIAL' | 'SKIPPED' | 'OVERDUE';

export type CycleItemType = 'INCOME' | 'FIXED_EXPENSE' | 'VARIABLE_EXPENSE' | 'SAVING';

export interface CycleItem {
  id: string;
  itemType: CycleItemType;
  sourceType: 'INCOME' | 'EXPENSE' | 'SAVINGS_GOAL';
  name: string;
  plannedAmount: MoneyValue;
  actualAmount: MoneyValue | null;
  dueDate: string | null;
  status: ItemStatus;
  flexibility: string | null;
  settledOn: string | null;
  settledAt: string | null;
  displayOrder: number;
  notes: string | null;
}

export interface ReviewItem {
  item: CycleItem;
  /** Lo confirmado del mismo gasto en el ciclo anterior. Nulo si no se sabe. */
  suggestedAmount: MoneyValue | null;
  suggestedFromStart: string | null;
}

export interface ItemHistoryEntry {
  cycleId: string;
  cycleStart: string;
  cycleEnd: string;
  plannedAmount: MoneyValue;
  actualAmount: MoneyValue;
  settledOn: string | null;
}

/**
 * El historial de un gasto con su promedio.
 *
 * El promedio llega calculado del servidor: es una cifra de dinero, y aqui no
 * se deriva ninguna. Es nulo cuando todavia no hay historia.
 */
export interface ItemHistory {
  cycles: number;
  average: MoneyValue | null;
  entries: ItemHistoryEntry[];
}

export interface CyclePeriod {
  /** BIWEEKLY, MONTHLY o BIMONTHLY. */
  type: string;
  start: string;
  end: string;
}

export interface BudgetCycleSummary {
  id: string;
  period: CyclePeriod;
  status: string;
  sequenceNumber: number;
  closedAt: string | null;
}

export interface ConfirmAmountsPayload {
  items: Array<{ itemId: string; amount: string }>;
}

export interface SettlePayload {
  actualAmount: string;
  settledOn: string | null;
  registerInGoal?: boolean;
}

/**
 * El monto que la interfaz propone escrito en el campo.
 *
 * <p>Se prefiere lo que costo la vez pasada sobre la estimacion, porque un dato
 * real vale mas que un plan. Sin historial queda la estimacion, que al menos
 * viene de la persona.
 */
export function proposedAmount(review: ReviewItem): string {
  return (review.suggestedAmount ?? review.item.plannedAmount).amount;
}
