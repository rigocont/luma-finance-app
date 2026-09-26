package com.luma.notifications.application;

import com.luma.common.error.ResourceNotFoundException;
import com.luma.notifications.domain.Notification;
import com.luma.notifications.domain.NotificationType;
import com.luma.notifications.infrastructure.NotificationRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Casos de uso de las alertas internas.
 *
 * <p>Los tres metodos {@code notifyX} son idempotentes a proposito: se llaman
 * desde un job programado que puede volver a correr, y desde un listener de
 * eventos, sin que ninguno de los dos tenga que saber si la alerta ya existe.
 * Quien avisa simplemente dice "esto paso"; decidir si hace falta una fila
 * nueva es trabajo de este servicio.
 */
@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final NotificationRepository notifications;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, Clock clock) {
        this.notifications = notifications;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public Page<Notification> list(Long userId, Pageable pageable) {
        return notifications.findByUserIdOrderByCreatedAtDesc(userId, pageable);
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notifications.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public Notification markRead(Long userId, String publicId) {
        Notification notification = notifications.findByPublicIdAndUserId(publicId, userId)
                .orElseThrow(() -> ResourceNotFoundException.of("Alerta", publicId));
        notification.markRead(clock.instant());
        return notifications.save(notification);
    }

    /** @return cuantas se marcaron. Cero es una respuesta valida, no un error. */
    @Transactional
    public int markAllRead(Long userId) {
        List<Notification> pendientes = notifications.findByUserIdAndReadFalse(userId);
        pendientes.forEach(n -> n.markRead(clock.instant()));
        notifications.saveAll(pendientes);
        return pendientes.size();
    }

    @Transactional
    public void notifyPaymentDueSoon(
            Long userId, String itemPublicId, String itemName, BigDecimal amount, LocalDate dueDate) {
        crearSiNoExiste(
                userId,
                NotificationType.PAYMENT_DUE_SOON,
                itemPublicId,
                () -> Notification.paymentDueSoon(userId, itemPublicId, itemName, amount, dueDate));
    }

    @Transactional
    public void notifyPaymentOverdue(
            Long userId, String itemPublicId, String itemName, BigDecimal amount, LocalDate dueDate) {
        crearSiNoExiste(
                userId,
                NotificationType.PAYMENT_OVERDUE,
                itemPublicId,
                () -> Notification.paymentOverdue(userId, itemPublicId, itemName, amount, dueDate));
    }

    @Transactional
    public void notifyCycleDeficit(Long userId, String cyclePublicId, BigDecimal missingAmount) {
        crearSiNoExiste(
                userId,
                NotificationType.CYCLE_DEFICIT,
                cyclePublicId,
                () -> Notification.cycleDeficit(userId, cyclePublicId, missingAmount));
    }

    /**
     * El candado del lado de la aplicacion. La llave unica de la base es el
     * segundo candado, para cuando dos ejecuciones coincidan en el mismo
     * instante; este evita el viaje a la base la mayoria de las veces.
     */
    private void crearSiNoExiste(
            Long userId, NotificationType type, String referenceId, Supplier<Notification> constructor) {

        if (notifications.existsByUserIdAndTypeAndReferenceId(userId, type, referenceId)) {
            return;
        }
        notifications.save(constructor.get());
        log.info("Alerta {} generada para el usuario {} ({})", type, userId, referenceId);
    }
}
