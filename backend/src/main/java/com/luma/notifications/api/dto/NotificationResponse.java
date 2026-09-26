package com.luma.notifications.api.dto;

import com.luma.common.model.Money;
import com.luma.common.web.MoneyDto;
import com.luma.notifications.domain.Notification;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Una alerta, tal como sale por la API.
 *
 * <p>{@code itemName}, {@code amount} y {@code dueDate} llegan nulos cuando no
 * aplican al tipo (un deficit no senala un renglon; un pago no trae un monto
 * distinto al de su propio renglon en el ciclo). El cliente arma la oracion
 * segun {@code type}, igual que ya hace con {@code BudgetState} en el
 * resumen financiero: el texto es un mapa fijo en la interfaz, no algo que
 * calcule.
 */
public record NotificationResponse(
        String id,
        String type,
        String referenceId,
        String itemName,
        MoneyDto amount,
        LocalDate dueDate,
        boolean read,
        Instant createdAt) {

    public static NotificationResponse from(Notification notification, String currency) {
        return new NotificationResponse(
                notification.getPublicId(),
                notification.getType().name(),
                notification.getReferenceId(),
                notification.getItemName(),
                notification.getAmount() != null
                        ? MoneyDto.from(Money.of(notification.getAmount(), currency))
                        : null,
                notification.getDueDate(),
                notification.isRead(),
                notification.getCreatedAt());
    }
}
