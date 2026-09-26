package com.luma.notifications.application;

import com.luma.budget.domain.CycleItemOverdueEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Reacciona a lo que pasa en otros modulos generando la alerta que corresponde.
 *
 * <p>Vive en notifications, no en budget, para que la dependencia siga yendo
 * en un solo sentido: budget no sabe que existen las alertas, solo publica que
 * un renglon se vencio. Mismo patron que {@code CycleSyncListener}.
 */
@Component
public class NotificationEventListener {

    private final NotificationService notifications;

    public NotificationEventListener(NotificationService notifications) {
        this.notifications = notifications;
    }

    @EventListener
    @Transactional
    public void onCycleItemOverdue(CycleItemOverdueEvent event) {
        notifications.notifyPaymentOverdue(
                event.userId(), event.itemPublicId(), event.itemName(), event.plannedAmount(), event.dueDate());
    }
}
