package com.luma.budget;

import static org.assertj.core.api.Assertions.assertThat;

import com.luma.budget.application.BudgetCycleService;
import com.luma.budget.application.DeficitAdviceService;
import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.BudgetPeriod;
import com.luma.budget.domain.BudgetState;
import com.luma.budget.domain.CutCandidate;
import com.luma.budget.domain.DeficitAdvisor;
import com.luma.budget.domain.Flexibility;
import com.luma.budget.domain.Frequency;
import com.luma.budget.infrastructure.BudgetCycleRepository;
import com.luma.common.model.Money;
import com.luma.expenses.application.ExpenseService;
import com.luma.expenses.domain.ExpenseKind;
import com.luma.income.application.IncomeService;
import com.luma.income.domain.IncomeType;
import com.luma.savings.application.SavingsGoalService;
import com.luma.savings.domain.ContributionMode;
import com.luma.support.IntegrationTest;
import com.luma.users.domain.User;
import com.luma.users.infrastructure.UserRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * El resumen financiero contra la base real.
 *
 * <p>Lo que se prueba aqui es el armado del snapshot: que el consejo de deficit
 * reciba la flexibilidad de cada gasto y la prioridad de cada meta desde la base,
 * y que la comparacion entre ciclos salga en el orden en que se va a leer. La
 * decision en si —a quien proponer y en que orden— vive en
 * {@code DeficitAdvisorTest}, sin base de datos.
 */
@Transactional
@DisplayName("El resumen financiero contra la base real")
class BudgetDashboardIntegrationTest extends IntegrationTest {

    private static final int DIA_SEGURO = 10;

    @Autowired
    UserRepository users;

    @Autowired
    IncomeService incomes;

    @Autowired
    ExpenseService expenses;

    @Autowired
    SavingsGoalService savings;

    @Autowired
    BudgetCycleService cycles;

    @Autowired
    DeficitAdviceService advice;

    @Autowired
    BudgetCycleRepository cycleRepository;

    private Long userId;

    @BeforeEach
    void crearUsuario() {
        User user = users.save(User.register(
                "qa+" + UUID.randomUUID() + "@luma.app", "Persona de prueba", "hash-irrelevante"));
        userId = user.getId();
    }

    private static LocalDate inicioLejano() {
        return LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusYears(1);
    }

    private void ingreso(String monto) {
        incomes.create(
                userId, "Sueldo", IncomeType.RECURRENT, Money.of(monto),
                Frequency.BIWEEKLY, DIA_SEGURO, inicioLejano(), null, null);
    }

    private void gasto(String nombre, String monto, Flexibility flexibility) {
        expenses.create(
                userId, null, nombre, ExpenseKind.FIXED, Money.of(monto),
                Frequency.BIWEEKLY, DIA_SEGURO, flexibility, inicioLejano(), null, null);
    }

    private void meta(String nombre, String porCiclo) {
        savings.create(
                userId, nombre, Money.of("50000.00"), null,
                ContributionMode.FIXED_PER_CYCLE, Money.of(porCiclo), null, null);
    }

    /** Un ciclo ya cerrado, tantos meses atras del de referencia. */
    private void cicloAnterior(BudgetCycle referencia, int mesesAtras) {
        LocalDate inicio = referencia.getStartDate().minusMonths(mesesAtras);
        BudgetCycle viejo = BudgetCycle.open(
                userId,
                referencia.getCycleType(),
                BudgetPeriod.of(inicio, inicio.plusDays(13)),
                referencia.getSequenceNumber() - mesesAtras);
        viejo.close();
        cycleRepository.save(viejo);
    }

    @Nested
    @DisplayName("el consejo de deficit")
    class Consejo {

        @Test
        @DisplayName("con remanente no propone nada")
        void conRemanente() {
            ingreso("20000.00");
            gasto("Luz", "500.00", Flexibility.IMPORTANT);
            BudgetCycle cycle = cycles.openNextCycle(userId);

            DeficitAdvisor.Advice resultado = advice.adviceFor(cycle, "MXN");

            assertThat(resultado.cuts()).isEmpty();
            assertThat(resultado.missing()).isEqualTo(Money.of("0.00"));
        }

        @Test
        @DisplayName("propone el gasto flexible y trae su flexibilidad de la base")
        void proponeElFlexible() {
            ingreso("3000.00");
            gasto("Streaming", "2000.00", Flexibility.FLEXIBLE);
            gasto("Renta", "5000.00", Flexibility.CRITICAL);
            BudgetCycle cycle = cycles.openNextCycle(userId);

            DeficitAdvisor.Advice resultado = advice.adviceFor(cycle, "MXN");

            // El critico no aparece aunque sea el monto mas grande.
            assertThat(resultado.cuts()).extracting(CutCandidate::name)
                    .containsExactly("Streaming");
            assertThat(resultado.missing()).isEqualTo(Money.of("4000.00"));
            assertThat(resultado.coversTheGap()).isFalse();
        }

        @Test
        @DisplayName("el ahorro llega con la prioridad que tiene la meta")
        void elAhorroTraeSuPrioridad() {
            ingreso("3000.00");
            meta("Vacaciones", "1500.00");
            meta("Emergencias", "1500.00");
            gasto("Renta", "5000.00", Flexibility.CRITICAL);
            BudgetCycle cycle = cycles.openNextCycle(userId);

            DeficitAdvisor.Advice resultado = advice.adviceFor(cycle, "MXN");

            // "Vacaciones" se creo primero, asi que tiene prioridad 1 y
            // "Emergencias" la 2: se propone antes la MENOS prioritaria.
            assertThat(resultado.cuts()).extracting(CutCandidate::name)
                    .startsWith("Emergencias");
        }

        @Test
        @DisplayName("los ingresos nunca son candidatos")
        void losIngresosNoSeRecortan() {
            ingreso("1000.00");
            gasto("Renta", "5000.00", Flexibility.CRITICAL);
            BudgetCycle cycle = cycles.openNextCycle(userId);

            assertThat(advice.adviceFor(cycle, "MXN").cuts())
                    .extracting(CutCandidate::name)
                    .doesNotContain("Sueldo");
        }

        @Test
        @DisplayName("un ciclo vacio no rompe nada")
        void cicloVacio() {
            BudgetCycle cycle = cycles.openNextCycle(userId);

            DeficitAdvisor.Advice resultado = advice.adviceFor(cycle, "MXN");

            assertThat(resultado.cuts()).isEmpty();
            assertThat(resultado.coversTheGap()).isTrue();
        }
    }

    @Nested
    @DisplayName("la comparacion entre ciclos")
    class Tendencias {

        @Test
        @DisplayName("sin ciclos viene vacia")
        void sinCiclos() {
            assertThat(cycles.trends(userId, 6, "MXN")).isEmpty();
        }

        @Test
        @DisplayName("del mas antiguo al mas reciente")
        void delMasAntiguoAlMasReciente() {
            // Al contrario del historial: una comparacion se lee de izquierda a
            // derecha, y si llegara invertida la grafica saldria al reves.
            ingreso("20000.00");
            BudgetCycle actual = cycles.openNextCycle(userId);
            cicloAnterior(actual, 2);
            cicloAnterior(actual, 1);

            List<BudgetCycleService.CycleTrend> tendencia = cycles.trends(userId, 6, "MXN");

            assertThat(tendencia).hasSize(3);
            assertThat(tendencia)
                    .extracting(trend -> trend.cycle().getStartDate())
                    .isSorted();
            assertThat(tendencia.getLast().cycle().getPublicId())
                    .isEqualTo(actual.getPublicId());
        }

        @Test
        @DisplayName("respeta cuantos se piden")
        void respetaElTope() {
            ingreso("20000.00");
            BudgetCycle actual = cycles.openNextCycle(userId);
            cicloAnterior(actual, 3);
            cicloAnterior(actual, 2);
            cicloAnterior(actual, 1);

            assertThat(cycles.trends(userId, 2, "MXN")).hasSize(2);
        }

        @Test
        @DisplayName("cada ciclo trae su balance ya calculado")
        void cadaCicloTraeSuBalance() {
            ingreso("20000.00");
            gasto("Renta", "6000.00", Flexibility.CRITICAL);
            cycles.openNextCycle(userId);

            BudgetCycleService.CycleTrend actual = cycles.trends(userId, 6, "MXN").getLast();

            assertThat(actual.result().planned().income()).isEqualTo(Money.of("20000.00"));
            assertThat(actual.result().state()).isEqualTo(BudgetState.SURPLUS);
        }
    }
}
