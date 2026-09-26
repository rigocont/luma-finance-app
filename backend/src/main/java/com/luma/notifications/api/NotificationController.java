package com.luma.notifications.api;

import com.luma.budget.application.BudgetCycleService;
import com.luma.common.web.PageResponse;
import com.luma.notifications.api.dto.NotificationResponse;
import com.luma.notifications.application.NotificationService;
import com.luma.users.application.CurrentUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Las alertas internas: pagos que se acercan o se pasaron, y ciclos que no
 * alcanzan.
 *
 * <p>Sin pantalla propia en esta fase: la interfaz las muestra en un menu
 * desplegable desde la campana del encabezado, asi que esta clase solo
 * necesita listar, contar y marcar como leidas.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "Alertas internas de pagos y presupuesto")
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final NotificationService notifications;
    private final BudgetCycleService cycles;
    private final CurrentUserService currentUser;

    public NotificationController(
            NotificationService notifications, BudgetCycleService cycles, CurrentUserService currentUser) {
        this.notifications = notifications;
        this.cycles = cycles;
        this.currentUser = currentUser;
    }

    @GetMapping
    @Operation(
            summary = "Las alertas del usuario, de la mas reciente a la mas vieja",
            description = "Leidas y no leidas juntas: el menu desplegable las distingue por estilo, no por una segunda peticion.")
    public PageResponse<NotificationResponse> list(
            @AuthenticationPrincipal Jwt jwt,
            @PageableDefault(size = 20) Pageable pageable) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = cycles.currencyOf(userId);

        return PageResponse.from(
                notifications.list(userId, pageable),
                notification -> NotificationResponse.from(notification, currency));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Cuantas alertas sin leer tiene el usuario, para la insignia de la campana")
    public UnreadCountResponse unreadCount(@AuthenticationPrincipal Jwt jwt) {
        Long userId = currentUser.requireId(jwt.getSubject());
        return new UnreadCountResponse(notifications.unreadCount(userId));
    }

    @PostMapping("/{notificationId}/read")
    @Operation(summary = "Marca una alerta como leida")
    public NotificationResponse markRead(
            @AuthenticationPrincipal Jwt jwt, @PathVariable String notificationId) {

        Long userId = currentUser.requireId(jwt.getSubject());
        String currency = cycles.currencyOf(userId);

        return NotificationResponse.from(
                notifications.markRead(userId, notificationId), currency);
    }

    @PostMapping("/read-all")
    @Operation(summary = "Marca todas las alertas del usuario como leidas")
    public UnreadCountResponse markAllRead(@AuthenticationPrincipal Jwt jwt) {
        Long userId = currentUser.requireId(jwt.getSubject());
        notifications.markAllRead(userId);
        return new UnreadCountResponse(notifications.unreadCount(userId));
    }

    /** Envoltura minima: un solo numero no se manda suelto en el cuerpo de la respuesta. */
    public record UnreadCountResponse(long count) {}
}
