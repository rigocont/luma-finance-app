package com.luma.notifications.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Una alerta interna: LUMA vio algo que la persona deberia saber sin tener que
 * ir a buscarlo.
 *
 * <p>Se crea UNA vez por condicion, no una vez por dia mientras siga activa.
 * Lo garantiza la llave unica de {@code (user_id, type, reference_id)} en la
 * base, y del lado de la aplicacion {@link
 * com.luma.notifications.application.NotificationService}, que revisa antes de
 * insertar. Doble candado a proposito: el job programado corre una vez al dia y
 * no deberia poder duplicar nada, pero un candado solo en la aplicacion no
 * sobrevive a una segunda instancia corriendo el mismo minuto.
 *
 * <p>{@code itemName}, {@code amount} y {@code dueDate} son COPIAS tomadas al
 * momento de generarse, igual que los renglones de un ciclo: si el gasto
 * cambia de nombre despues, la alerta sigue contando lo que paso cuando se
 * genero.
 */
@Entity
@Table(name = "notifications")
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private NotificationType type;

    @Column(name = "reference_id", nullable = false, length = 36)
    private String referenceId;

    @Column(name = "item_name", length = 120)
    private String itemName;

    @Column(precision = 15, scale = 2)
    private BigDecimal amount;

    @Column(name = "due_date")
    private LocalDate dueDate;

    @Column(name = "is_read", nullable = false)
    private boolean read;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected Notification() {
        // Requerido por JPA.
    }

    private static Notification of(
            Long userId,
            NotificationType type,
            String referenceId,
            String itemName,
            BigDecimal amount,
            LocalDate dueDate) {

        Notification notification = new Notification();
        notification.publicId = UUID.randomUUID().toString();
        notification.userId = userId;
        notification.type = type;
        notification.referenceId = referenceId;
        notification.itemName = itemName;
        notification.amount = amount;
        notification.dueDate = dueDate;
        notification.read = false;
        return notification;
    }

    public static Notification paymentDueSoon(
            Long userId, String itemPublicId, String itemName, BigDecimal amount, LocalDate dueDate) {
        return of(userId, NotificationType.PAYMENT_DUE_SOON, itemPublicId, itemName, amount, dueDate);
    }

    public static Notification paymentOverdue(
            Long userId, String itemPublicId, String itemName, BigDecimal amount, LocalDate dueDate) {
        return of(userId, NotificationType.PAYMENT_OVERDUE, itemPublicId, itemName, amount, dueDate);
    }

    public static Notification cycleDeficit(Long userId, String cyclePublicId, BigDecimal missingAmount) {
        return of(userId, NotificationType.CYCLE_DEFICIT, cyclePublicId, null, missingAmount, null);
    }

    public void markRead(Instant when) {
        if (read) {
            return;
        }
        this.read = true;
        this.readAt = when;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public Long getUserId() {
        return userId;
    }

    public NotificationType getType() {
        return type;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public String getItemName() {
        return itemName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public boolean isRead() {
        return read;
    }

    public Instant getReadAt() {
        return readAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
