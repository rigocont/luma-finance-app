/** Debe coincidir con com.luma.budget.domain.CycleType. */
export const CYCLE_TYPES = ['BIWEEKLY', 'MONTHLY', 'BIMONTHLY'] as const;
export type CycleType = (typeof CYCLE_TYPES)[number];

export const CYCLE_TYPE_LABELS: Record<CycleType, string> = {
  BIWEEKLY: 'Cada quincena',
  MONTHLY: 'Cada mes',
  BIMONTHLY: 'Cada dos meses',
};

export const CYCLE_TYPE_HELP: Record<CycleType, string> = {
  BIWEEKLY: 'Lo mas comun si cobras los dias 15 y 30.',
  MONTHLY: 'Un presupuesto por mes, de principio a fin.',
  BIMONTHLY: 'Util si tus ingresos llegan cada dos meses.',
};

export interface CyclePreferences {
  currency: string;
  locale: string;
  timezone: string;
  budgetCycleType: CycleType;
  cycleAnchorDay: number;
  expenseAllocationPolicy: string;
}

export interface CyclePreferencePayload {
  cycleType: CycleType;
  anchorDay: number;
}
