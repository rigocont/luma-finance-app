package com.luma.savings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.luma.budget.application.BudgetCycleService;
import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.CycleItem;
import com.luma.budget.domain.CycleItemType;
import com.luma.budget.domain.ItemStatus;
import com.luma.budget.infrastructure.CycleItemRepository;
import com.luma.common.error.BusinessRuleException;
import com.luma.common.model.Money;
import com.luma.savings.application.SavingsGoalService;
import com.luma.savings.domain.ContributionMode;
import com.luma.savings.domain.ContributionType;
import com.luma.savings.domain.SavingsGoal;
import com.luma.support.IntegrationTest;
import com.luma.users.domain.User;
import com.luma.users.infrastructure.UserRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * Las metas de ahorro contra la base real.
 *
 * <p>Lo que de verdad importa aqui es el cruce entre el ciclo y la meta:
 * confirmar el renglon de ahorro registra el aporte, y confirmarlo dos veces no
 * puede sumar dos veces. Eso solo se puede demostrar de extremo a extremo.
 */
@Transactional
@DisplayName("Ahorros contra la base real")
class SavingsIntegrationTest extends IntegrationTest {

    @Autowired
    UserRepository users;

    @Autowired
    SavingsGoalService savings;

    @Autowired
    BudgetCycleService cycles;

    @Autowired
    CycleItemRepository items;

    private Long userId;

    @BeforeEach
    void crearUsuario() {
        User user = users.save(User.register(
                "qa+" + UUID.randomUUID() + "@luma.app", "Persona de prueba", "hash-irrelevante"));
        userId = user.getId();
    }

    private SavingsGoal metaFija(String nombre, String objetivo, String porCiclo) {
        return savings.create(
                userId, nombre, Money.of(objetivo), null,
                ContributionMode.FIXED_PER_CYCLE, Money.of(porCiclo), null, null);
    }

    private List<CycleItem> renglonesDeAhorro(BudgetCycle cycle) {
        return items.findByBudgetCycleIdOrderByDueDateAscDisplayOrderAscIdAsc(cycle.getId())
                .stream()
                .filter(item -> item.getItemType() == CycleItemType.SAVING)
                .toList();
    }

    @Nested
    @DisplayName("con un ciclo abierto")
    class ConCicloAbierto {

        @Test
        @DisplayName("una meta nueva entra al ciclo en curso")
        void laMetaNuevaEntra() {
            BudgetCycle cycle = cycles.openNextCycle(userId);
            assertThat(renglonesDeAhorro(cycle)).isEmpty();

            metaFija("Fondo de emergencia", "30000.00", "1000.00");

            List<CycleItem> renglones = renglonesDeAhorro(cycle);
            assertThat(renglones).hasSize(1);
            assertThat(renglones.getFirst().getPlannedAmount()).isEqualByComparingTo("1000.00");
        }

        @Test
        @DisplayName("el aporte se espera al cierre del ciclo")
        void venceAlCierre() {
            BudgetCycle cycle = cycles.openNextCycle(userId);

            metaFija("Fondo", "30000.00", "1000.00");

            assertThat(renglonesDeAhorro(cycle).getFirst().getDueDate())
                    .isEqualTo(cycle.period().end());
        }

        @Test
        @DisplayName("una meta en modo manual NO entra")
        void laManualNoEntra() {
            BudgetCycle cycle = cycles.openNextCycle(userId);

            savings.create(
                    userId, "Algun dia", Money.of("5000.00"), null,
                    ContributionMode.MANUAL, null, null, null);

            assertThat(renglonesDeAhorro(cycle)).isEmpty();
        }
    }

    @Nested
    @DisplayName("confirmar el renglon de ahorro")
    class ConfirmarRenglon {

        @Test
        @DisplayName("registra el aporte y sube el progreso de la meta")
        void registraElAporte() {
            BudgetCycle cycle = cycles.openNextCycle(userId);
            SavingsGoal meta = metaFija("Fondo", "30000.00", "1000.00");
            CycleItem renglon = renglonesDeAhorro(cycle).getFirst();

            cycles.settleItem(cycle, renglon.getPublicId(), Money.of("1000.00"), null, true);

            assertThat(savings.require(userId, meta.getPublicId()).saved("MXN"))
                    .isEqualTo(Money.of("1000.00"));
        }

        @Test
        @DisplayName("con el aporte desmarcado NO toca la meta")
        void puedeNoRegistrarse() {
            // Apartar el dinero y registrarlo en la meta son la misma accion
            // casi siempre, pero no siempre.
            BudgetCycle cycle = cycles.openNextCycle(userId);
            SavingsGoal meta = metaFija("Fondo", "30000.00", "1000.00");
            CycleItem renglon = renglonesDeAhorro(cycle).getFirst();

            cycles.settleItem(cycle, renglon.getPublicId(), Money.of("1000.00"), null, false);

            assertThat(savings.require(userId, meta.getPublicId()).saved("MXN"))
                    .isEqualTo(Money.of("0.00"));
            assertThat(renglonesDeAhorro(cycle).getFirst().getStatus())
                    .isEqualTo(ItemStatus.PAID);
        }

        @Test
        @DisplayName("confirmar dos veces NO suma dos veces")
        void noCuentaDosVeces() {
            // Es la defensa que justifica guardar el cycle_item_id en la
            // aportacion. Sin ella el progreso se separa de la realidad.
            BudgetCycle cycle = cycles.openNextCycle(userId);
            SavingsGoal meta = metaFija("Fondo", "30000.00", "1000.00");
            CycleItem renglon = renglonesDeAhorro(cycle).getFirst();

            cycles.settleItem(cycle, renglon.getPublicId(), Money.of("1000.00"), null, true);
            cycles.settleItem(cycle, renglon.getPublicId(), Money.of("1000.00"), null, true);

            assertThat(savings.require(userId, meta.getPublicId()).saved("MXN"))
                    .isEqualTo(Money.of("1000.00"));
        }

        @Test
        @DisplayName("confirmar por un monto distinto registra lo que de verdad apartaste")
        void registraElMontoReal() {
            BudgetCycle cycle = cycles.openNextCycle(userId);
            SavingsGoal meta = metaFija("Fondo", "30000.00", "1000.00");
            CycleItem renglon = renglonesDeAhorro(cycle).getFirst();

            cycles.settleItem(cycle, renglon.getPublicId(), Money.of("600.00"), null, true);

            assertThat(savings.require(userId, meta.getPublicId()).saved("MXN"))
                    .isEqualTo(Money.of("600.00"));
            assertThat(renglonesDeAhorro(cycle).getFirst().getStatus())
                    .isEqualTo(ItemStatus.PARTIAL);
        }
    }

    @Nested
    @DisplayName("movimientos sueltos")
    class Movimientos {

        @Test
        @DisplayName("una aportacion extra sube el progreso")
        void aportacionExtra() {
            SavingsGoal meta = metaFija("Fondo", "30000.00", "1000.00");

            savings.registerMovement(
                    userId, meta.getPublicId(), Money.of("2500.00"), null,
                    ContributionType.EXTRA, null, "Aguinaldo");

            assertThat(savings.require(userId, meta.getPublicId()).saved("MXN"))
                    .isEqualTo(Money.of("2500.00"));
            assertThat(savings.movements(userId, meta.getPublicId())).hasSize(1);
        }

        @Test
        @DisplayName("un retiro se guarda en negativo y baja el progreso")
        void retiro() {
            SavingsGoal meta = metaFija("Fondo", "30000.00", "1000.00");
            savings.registerMovement(
                    userId, meta.getPublicId(), Money.of("2000.00"), null,
                    ContributionType.EXTRA, null, null);

            savings.registerMovement(
                    userId, meta.getPublicId(), Money.of("800.00"), null,
                    ContributionType.WITHDRAWAL, null, null);

            assertThat(savings.require(userId, meta.getPublicId()).saved("MXN"))
                    .isEqualTo(Money.of("1200.00"));
            assertThat(savings.movements(userId, meta.getPublicId()).getFirst().getAmount())
                    .isEqualByComparingTo("-800.00");
        }

        @Test
        @DisplayName("no se puede retirar mas de lo que hay")
        void retiroExcesivo() {
            SavingsGoal meta = metaFija("Fondo", "30000.00", "1000.00");

            assertThatThrownBy(() -> savings.registerMovement(
                            userId, meta.getPublicId(), Money.of("100.00"), null,
                            ContributionType.WITHDRAWAL, null, null))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    @DisplayName("prioridad")
    class Prioridad {

        @Test
        @DisplayName("la meta nueva queda al final")
        void laNuevaVaAlFinal() {
            SavingsGoal primera = metaFija("Fondo", "30000.00", "1000.00");
            SavingsGoal segunda = metaFija("Vacaciones", "18000.00", "500.00");

            assertThat(primera.getPriority()).isLessThan(segunda.getPriority());
        }

        @Test
        @DisplayName("reordenar reasigna las prioridades en el orden recibido")
        void reordenar() {
            SavingsGoal primera = metaFija("Fondo", "30000.00", "1000.00");
            SavingsGoal segunda = metaFija("Vacaciones", "18000.00", "500.00");

            List<SavingsGoal> resultado = savings.reorder(
                    userId, List.of(segunda.getPublicId(), primera.getPublicId()));

            assertThat(resultado).extracting(SavingsGoal::getName)
                    .containsExactly("Vacaciones", "Fondo");
        }

        @Test
        @DisplayName("un orden incompleto se rechaza")
        void ordenIncompleto() {
            // Aceptar una lista parcial dejaria metas con prioridad ambigua.
            SavingsGoal primera = metaFija("Fondo", "30000.00", "1000.00");
            metaFija("Vacaciones", "18000.00", "500.00");

            assertThatThrownBy(() -> savings.reorder(userId, List.of(primera.getPublicId())))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        @DisplayName("un orden con una meta repetida se rechaza")
        void ordenConRepetidos() {
            // Tiene el tamano correcto y aun asi es invalido: asignaria dos
            // prioridades a la misma meta y dejaria a la otra con la suya
            // vieja, duplicada. Comprobar solo el tamano no lo atrapa.
            SavingsGoal primera = metaFija("Fondo", "30000.00", "1000.00");
            metaFija("Vacaciones", "18000.00", "500.00");

            assertThatThrownBy(() -> savings.reorder(
                            userId, List.of(primera.getPublicId(), primera.getPublicId())))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    @DisplayName("aislamiento")
    class Aislamiento {

        @Test
        @DisplayName("no se ven las metas de otra persona")
        void noSeVenLasDeOtro() {
            SavingsGoal mia = metaFija("Fondo", "30000.00", "1000.00");

            User otro = users.save(User.register(
                    "qa+" + UUID.randomUUID() + "@luma.app", "Otra persona", "hash"));

            assertThatThrownBy(() -> savings.require(otro.getId(), mia.getPublicId()))
                    .isInstanceOf(com.luma.common.error.ResourceNotFoundException.class);
            assertThat(savings.search(otro.getId(), null)).isEmpty();
        }
    }
}
