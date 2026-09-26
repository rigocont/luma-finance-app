package com.luma.notifications.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Una alerta interna")
class NotificationTest {

    private static final Long USER_ID = 1L;

    @Nested
    @DisplayName("al generarse")
    class AlGenerarse {

        @Test
        @DisplayName("un pago proximo trae su renglon, monto y fecha, sin leer")
        void pagoProximo() {
            LocalDate vence = LocalDate.of(2026, 10, 1);
            Notification alerta = Notification.paymentDueSoon(
                    USER_ID, "item-1", "Renta", new BigDecimal("8000.00"), vence);

            assertThat(alerta.getType()).isEqualTo(NotificationType.PAYMENT_DUE_SOON);
            assertThat(alerta.getReferenceId()).isEqualTo("item-1");
            assertThat(alerta.getItemName()).isEqualTo("Renta");
            assertThat(alerta.getAmount()).isEqualByComparingTo("8000.00");
            assertThat(alerta.getDueDate()).isEqualTo(vence);
            assertThat(alerta.isRead()).isFalse();
            assertThat(alerta.getReadAt()).isNull();
            assertThat(alerta.getPublicId()).isNotBlank();
        }

        @Test
        @DisplayName("un pago vencido se ve igual que uno proximo, solo cambia el tipo")
        void pagoVencido() {
            Notification alerta = Notification.paymentOverdue(
                    USER_ID, "item-2", "Internet", new BigDecimal("599.00"), LocalDate.of(2026, 9, 20));

            assertThat(alerta.getType()).isEqualTo(NotificationType.PAYMENT_OVERDUE);
            assertThat(alerta.getItemName()).isEqualTo("Internet");
        }

        @Test
        @DisplayName("un deficit no trae nombre de renglon ni fecha, solo el faltante")
        void deficit() {
            Notification alerta =
                    Notification.cycleDeficit(USER_ID, "cycle-1", new BigDecimal("1250.00"));

            assertThat(alerta.getType()).isEqualTo(NotificationType.CYCLE_DEFICIT);
            assertThat(alerta.getReferenceId()).isEqualTo("cycle-1");
            assertThat(alerta.getItemName()).isNull();
            assertThat(alerta.getDueDate()).isNull();
            assertThat(alerta.getAmount()).isEqualByComparingTo("1250.00");
        }

        @Test
        @DisplayName("dos alertas nunca comparten public_id")
        void publicIdUnico() {
            Notification a = Notification.cycleDeficit(USER_ID, "cycle-1", BigDecimal.TEN);
            Notification b = Notification.cycleDeficit(USER_ID, "cycle-1", BigDecimal.TEN);

            assertThat(a.getPublicId()).isNotEqualTo(b.getPublicId());
        }
    }

    @Nested
    @DisplayName("al marcarse como leida")
    class AlMarcarseLeida {

        @Test
        @DisplayName("guarda el momento en que se leyo")
        void marcaLeida() {
            Notification alerta = Notification.cycleDeficit(USER_ID, "cycle-1", BigDecimal.TEN);
            Instant leidaEn = Instant.parse("2026-09-26T10:00:00Z");

            alerta.markRead(leidaEn);

            assertThat(alerta.isRead()).isTrue();
            assertThat(alerta.getReadAt()).isEqualTo(leidaEn);
        }

        @Test
        @DisplayName("marcarla otra vez no mueve el momento original")
        void esIdempotente() {
            Notification alerta = Notification.cycleDeficit(USER_ID, "cycle-1", BigDecimal.TEN);
            Instant primeraLectura = Instant.parse("2026-09-26T10:00:00Z");

            alerta.markRead(primeraLectura);
            alerta.markRead(Instant.parse("2026-09-27T10:00:00Z"));

            assertThat(alerta.getReadAt()).isEqualTo(primeraLectura);
        }
    }
}
