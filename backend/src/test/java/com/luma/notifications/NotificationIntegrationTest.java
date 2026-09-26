package com.luma.notifications;

import static org.assertj.core.api.Assertions.assertThat;

import com.luma.budget.application.BudgetCycleService;
import com.luma.budget.application.OverdueItemsJob;
import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.BudgetPeriod;
import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleItemType;
import com.luma.budget.domain.CycleType;
import com.luma.budget.domain.Flexibility;
import com.luma.budget.domain.ItemSource;
import com.luma.budget.domain.ItemStatus;
import com.luma.budget.infrastructure.BudgetCycleRepository;
import com.luma.budget.infrastructure.CycleItemRepository;
import com.luma.common.model.Money;
import com.luma.notifications.application.NotificationService;
import com.luma.notifications.application.PaymentAlertsJob;
import com.luma.notifications.domain.Notification;
import com.luma.notifications.domain.NotificationType;
import com.luma.notifications.infrastructure.NotificationRepository;
import com.luma.support.IntegrationTest;
import com.luma.users.domain.User;
import com.luma.users.infrastructure.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las tres alertas contra la base real: que se generen cuando corresponde, que
 * no se dupliquen, y que el servicio de consulta las liste, cuente y marque
 * bien.
 *
 * <p>Los trabajos programados se construyen a mano con un reloj fijo, como
 * {@code AbandonedOnboardingJobTest}: lo que hay que demostrar es COMO deciden,
 * y para eso el reloj es un dato de la prueba, no algo que dependa de cuando
 * corra la suite.
 */
@Transactional
@DisplayName("Las alertas internas contra la base real")
class NotificationIntegrationTest extends IntegrationTest {

    @Autowired
    UserRepository users;

    @Autowired
    BudgetCycleRepository cycles;

    @Autowired
    CycleItemRepository items;

    @Autowired
    NotificationRepository notificationRepository;

    @Autowired
    NotificationService notifications;

    @Autowired
    BudgetCycleService budgetCycles;

    @Autowired
    ApplicationEventPublisher events;

    private Long userId;

    @BeforeEach
    void crearUsuario() {
        User user = users.save(User.register(
                "qa+" + UUID.randomUUID() + "@luma.app", "Persona de prueba", "hash-irrelevante"));
        userId = user.getId();
    }

    /** Un ciclo activo que cubre la fecha dada, para no pelear con el planificador real. */
    private BudgetCycle cicloActivoQueCubre(LocalDate fecha) {
        BudgetPeriod periodo = BudgetPeriod.of(fecha.minusDays(15), fecha.plusDays(15));
        return cycles.save(BudgetCycle.open(userId, CycleType.MONTHLY, periodo, 1));
    }

    private CycleItem renglon(
            BudgetCycle cycle, CycleItemType tipo, String nombre, String monto, LocalDate vence) {
        return items.save(CycleItem.materialize(
                cycle.getId(),
                tipo,
                tipo == CycleItemType.INCOME ? ItemSource.INCOME : ItemSource.EXPENSE,
                null,
                nombre,
                null,
                Money.of(monto),
                vence,
                ItemStatus.PENDING,
                tipo == CycleItemType.INCOME ? null : Flexibility.IMPORTANT,
                0));
    }

    private Clock relojFijo(LocalDate hoy) {
        return Clock.fixed(hoy.atStartOfDay(ZoneOffset.UTC).toInstant(), ZoneOffset.UTC);
    }

    @Nested
    @DisplayName("el aviso de pago proximo")
    class PagoProximo {

        @Test
        @DisplayName("se genera para un renglon que vence en exactamente 3 dias")
        void seGeneraA3Dias() {
            LocalDate hoy = LocalDate.of(2026, 10, 1);
            BudgetCycle cycle = cicloActivoQueCubre(hoy);
            renglon(cycle, CycleItemType.INCOME, "Sueldo", "8000.00", hoy);
            renglon(cycle, CycleItemType.FIXED_EXPENSE, "Renta", "8000.00", hoy.plusDays(3));

            new PaymentAlertsJob(budgetCycles, notifications, relojFijo(hoy)).generate();

            List<Notification> generadas = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                    userId, PageRequest.of(0, 10)).getContent();

            assertThat(generadas).hasSize(1);
            assertThat(generadas.get(0).getType()).isEqualTo(NotificationType.PAYMENT_DUE_SOON);
            assertThat(generadas.get(0).getItemName()).isEqualTo("Renta");
            assertThat(generadas.get(0).getAmount()).isEqualByComparingTo("8000.00");
        }

        @Test
        @DisplayName("no se genera para un renglon que vence en 2 o en 4 dias")
        void noSeGeneraFueraDelUmbral() {
            LocalDate hoy = LocalDate.of(2026, 10, 1);
            BudgetCycle cycle = cicloActivoQueCubre(hoy);
            renglon(cycle, CycleItemType.INCOME, "Sueldo", "200.00", hoy);
            renglon(cycle, CycleItemType.FIXED_EXPENSE, "En 2 dias", "100.00", hoy.plusDays(2));
            renglon(cycle, CycleItemType.FIXED_EXPENSE, "En 4 dias", "100.00", hoy.plusDays(4));

            new PaymentAlertsJob(budgetCycles, notifications, relojFijo(hoy)).generate();

            assertThat(notifications.unreadCount(userId)).isZero();
        }

        @Test
        @DisplayName("correr el trabajo dos veces no duplica la alerta")
        void noSeDuplicaEntreCorridas() {
            LocalDate hoy = LocalDate.of(2026, 10, 1);
            BudgetCycle cycle = cicloActivoQueCubre(hoy);
            renglon(cycle, CycleItemType.INCOME, "Sueldo", "8000.00", hoy);
            renglon(cycle, CycleItemType.FIXED_EXPENSE, "Renta", "8000.00", hoy.plusDays(3));

            PaymentAlertsJob job = new PaymentAlertsJob(budgetCycles, notifications, relojFijo(hoy));
            job.generate();
            job.generate();

            assertThat(notifications.unreadCount(userId)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("el aviso de deficit")
    class Deficit {

        @Test
        @DisplayName("se genera cuando el ciclo no alcanza, con el faltante exacto")
        void seGeneraConElFaltante() {
            LocalDate hoy = LocalDate.of(2026, 10, 1);
            BudgetCycle cycle = cicloActivoQueCubre(hoy);
            renglon(cycle, CycleItemType.INCOME, "Sueldo", "5000.00", hoy);
            renglon(cycle, CycleItemType.FIXED_EXPENSE, "Renta", "6500.00", hoy.plusDays(10));

            new PaymentAlertsJob(budgetCycles, notifications, relojFijo(hoy)).generate();

            List<Notification> generadas = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                    userId, PageRequest.of(0, 10)).getContent();

            assertThat(generadas).hasSize(1);
            assertThat(generadas.get(0).getType()).isEqualTo(NotificationType.CYCLE_DEFICIT);
            assertThat(generadas.get(0).getReferenceId()).isEqualTo(cycle.getPublicId());
            assertThat(generadas.get(0).getAmount()).isEqualByComparingTo("1500.00");
        }

        @Test
        @DisplayName("no se genera cuando el ciclo alcanza")
        void noSeGeneraSiAlcanza() {
            LocalDate hoy = LocalDate.of(2026, 10, 1);
            BudgetCycle cycle = cicloActivoQueCubre(hoy);
            renglon(cycle, CycleItemType.INCOME, "Sueldo", "5000.00", hoy);
            renglon(cycle, CycleItemType.FIXED_EXPENSE, "Renta", "3000.00", hoy.plusDays(10));

            new PaymentAlertsJob(budgetCycles, notifications, relojFijo(hoy)).generate();

            assertThat(notifications.unreadCount(userId)).isZero();
        }
    }

    @Nested
    @DisplayName("el aviso de pago vencido")
    class PagoVencido {

        @Test
        @DisplayName("se genera en cuanto OverdueItemsJob marca el renglon, no antes")
        void seGeneraAlMarcarseVencido() {
            LocalDate hoy = LocalDate.of(2026, 10, 1);
            BudgetCycle cycle = cicloActivoQueCubre(hoy);
            renglon(cycle, CycleItemType.FIXED_EXPENSE, "Internet", "599.00", hoy.minusDays(1));

            assertThat(notifications.unreadCount(userId)).isZero();

            new OverdueItemsJob(cycles, items, events, relojFijo(hoy)).markOverdueItems();

            List<Notification> generadas = notificationRepository.findByUserIdOrderByCreatedAtDesc(
                    userId, PageRequest.of(0, 10)).getContent();

            assertThat(generadas).hasSize(1);
            assertThat(generadas.get(0).getType()).isEqualTo(NotificationType.PAYMENT_OVERDUE);
            assertThat(generadas.get(0).getItemName()).isEqualTo("Internet");
        }

        @Test
        @DisplayName("correr el trabajo dos veces no duplica la alerta")
        void noSeDuplicaEntreCorridas() {
            LocalDate hoy = LocalDate.of(2026, 10, 1);
            BudgetCycle cycle = cicloActivoQueCubre(hoy);
            renglon(cycle, CycleItemType.FIXED_EXPENSE, "Internet", "599.00", hoy.minusDays(1));

            OverdueItemsJob job = new OverdueItemsJob(cycles, items, events, relojFijo(hoy));
            job.markOverdueItems();
            job.markOverdueItems();

            assertThat(notifications.unreadCount(userId)).isEqualTo(1);
        }
    }

    @Nested
    @DisplayName("la consulta de alertas")
    class Consulta {

        @Test
        @DisplayName("lista de la mas reciente a la mas vieja")
        void listaEnOrden() {
            notifications.notifyCycleDeficit(userId, "cycle-a", new BigDecimal("100.00"));
            notifications.notifyCycleDeficit(userId, "cycle-b", new BigDecimal("200.00"));

            List<Notification> lista =
                    notifications.list(userId, PageRequest.of(0, 10)).getContent();

            assertThat(lista).hasSize(2);
            assertThat(lista.get(0).getReferenceId()).isEqualTo("cycle-b");
        }

        @Test
        @DisplayName("marcar una como leida baja el contador de no leidas")
        void marcarLeidaBajaElContador() {
            notifications.notifyCycleDeficit(userId, "cycle-a", new BigDecimal("100.00"));
            String publicId = notifications.list(userId, PageRequest.of(0, 10))
                    .getContent().get(0).getPublicId();

            assertThat(notifications.unreadCount(userId)).isEqualTo(1);

            notifications.markRead(userId, publicId);

            assertThat(notifications.unreadCount(userId)).isZero();
        }

        @Test
        @DisplayName("marcar todas como leidas deja el contador en cero")
        void marcarTodasLasDejaEnCero() {
            notifications.notifyCycleDeficit(userId, "cycle-a", new BigDecimal("100.00"));
            notifications.notifyCycleDeficit(userId, "cycle-b", new BigDecimal("200.00"));

            int marcadas = notifications.markAllRead(userId);

            assertThat(marcadas).isEqualTo(2);
            assertThat(notifications.unreadCount(userId)).isZero();
        }
    }
}
