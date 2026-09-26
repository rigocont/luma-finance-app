import type { MoneyValue } from '@/lib/money';

/** Debe coincidir con com.luma.notifications.domain.NotificationType. */
export const NOTIFICATION_TYPES = ['PAYMENT_DUE_SOON', 'PAYMENT_OVERDUE', 'CYCLE_DEFICIT'] as const;
export type NotificationType = (typeof NOTIFICATION_TYPES)[number];

/**
 * Una alerta interna.
 *
 * `itemName`, `amount` y `dueDate` llegan nulos cuando el tipo no los usa: un
 * deficit no senala un renglon. El texto que se muestra depende de `type`, ver
 * `NOTIFICATION_COPY` — el mismo patron que `STATE_HEADLINE` en el resumen
 * financiero: la oracion es un mapa fijo en el cliente, no algo calculado.
 */
export interface AppNotification {
  id: string;
  type: NotificationType;
  referenceId: string;
  itemName: string | null;
  amount: MoneyValue | null;
  dueDate: string | null;
  read: boolean;
  createdAt: string;
}
