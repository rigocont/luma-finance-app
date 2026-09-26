import type { MoneyValue } from '@/lib/money';

/** Debe coincidir con com.luma.savings.domain.ContributionMode. */
export const CONTRIBUTION_MODES = ['AUTO_BY_TARGET_DATE', 'FIXED_PER_CYCLE', 'MANUAL'] as const;
export type ContributionMode = (typeof CONTRIBUTION_MODES)[number];

export const GOAL_STATUSES = ['ACTIVE', 'COMPLETED', 'PAUSED', 'CANCELED'] as const;
export type GoalStatus = (typeof GOAL_STATUSES)[number];

export const CONTRIBUTION_TYPES = ['PLANNED', 'EXTRA', 'WITHDRAWAL'] as const;
export type ContributionType = (typeof CONTRIBUTION_TYPES)[number];

export interface SavingsGoal {
  id: string;
  name: string;
  target: MoneyValue;
  saved: MoneyValue;
  remaining: MoneyValue;
  /**
   * De 0 a 1, ya recortado. El servidor lo calcula; aqui solo se presenta.
   *
   * Viaja como NUMERO, no como cadena: a diferencia del dinero, una proporcion
   * de cuatro decimales no pierde precision en el tipo numerico de JavaScript.
   */
  progress: number;
  targetDate: string | null;
  contributionMode: ContributionMode;
  plannedPerCycle: MoneyValue;
  priority: number;
  status: GoalStatus;
  affectsBudget: boolean;
  icon: string | null;
  color: string | null;
  createdAt: string;
}

export interface SavingsMovement {
  id: string;
  /** Negativo en los retiros. */
  amount: MoneyValue;
  date: string;
  type: ContributionType;
  fromCycle: boolean;
  notes: string | null;
  createdAt: string;
}

export interface SavingsGoalPayload {
  name: string;
  target: string;
  targetDate: string | null;
  clearTargetDate?: boolean;
  mode: ContributionMode;
  plannedPerCycle: string | null;
}

export interface MovementPayload {
  amount: string;
  date: string | null;
  type: Extract<ContributionType, 'EXTRA' | 'WITHDRAWAL'>;
  notes: string | null;
}

export const CONTRIBUTION_MODE_LABELS: Record<ContributionMode, string> = {
  AUTO_BY_TARGET_DATE: 'Calcularlo por mi, segun la fecha',
  FIXED_PER_CYCLE: 'Un monto fijo cada ciclo',
  MANUAL: 'Aporto cuando puedo',
};

export const CONTRIBUTION_MODE_HELP: Record<ContributionMode, string> = {
  AUTO_BY_TARGET_DATE:
    'LUMA reparte lo que falta entre los ciclos que quedan hasta tu fecha objetivo.',
  FIXED_PER_CYCLE: 'Tu decides cuanto apartar en cada ciclo.',
  MANUAL: 'No resta de tu presupuesto. Registras los aportes cuando los hagas.',
};

export const GOAL_STATUS_LABELS: Record<GoalStatus, string> = {
  ACTIVE: 'En marcha',
  COMPLETED: 'Alcanzada',
  PAUSED: 'Pausada',
  CANCELED: 'Cancelada',
};

export const MOVEMENT_TYPE_LABELS: Record<ContributionType, string> = {
  PLANNED: 'Del ciclo',
  EXTRA: 'Aportacion extra',
  WITHDRAWAL: 'Retiro',
};

/** El porcentaje entero que se muestra junto a la barra. */
export function progressPercent(goal: SavingsGoal): number {
  return Math.round(goal.progress * 100);
}
