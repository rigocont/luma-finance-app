package com.luma.insights;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.luma.expenses.application.ExpenseService;
import com.luma.insights.application.InsightsService;
import com.luma.insights.domain.FinancialInsights;
import com.luma.savings.application.SavingsGoalService;
import com.luma.savings.domain.ContributionMode;
import com.luma.support.IntegrationTest;
import com.luma.users.domain.User;
import com.luma.users.infrastructure.UserRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * El analisis financiero sin IA (Fase 13, parte determinista) contra la base
 * real: las tres senales -causa de deficit, reparto de remanente, crecimiento
 * sostenido- y, sobre todo, que cada una sepa quedarse callada cuando no hay
 * con que respaldarse.
 *
 * <p>La capa de redaccion con LLM (docs/00-arquitectura-fase-0.md, S8.4) no
 * existe todavia a proposito: esta prueba solo cubre las cifras.
 */
@Transactional
@DisplayName("El analisis financiero sin IA, contra la base real")
class InsightsIntegrationTest extends IntegrationTest {

    @Autowired
    UserRepository users;

    @Autowired
    ExpenseService expenses;

    @Autowired
    BudgetCycleRepository cycleRepository;

    @Autowired
    CycleItemRepository items;

    @Autowired
    SavingsGoalService savingsGoals;

    @Autowired
    InsightsService insights;

    private Long userId;

    @BeforeEach
    void crearUsuario() {
        User user = users.save(User.register(
                "qa+" + UUID.randomUUID() + "@luma.app", "Persona de prueba", "hash-irrelevante"));
        userId = user.getId();
    }

    /** El id de una categoria del catalogo del sistema, por su codigo. */
    private Long categoria(String code) {
        return expenses.catalog(userId).stream()
                .filter(categoria -> categoria.getCode().equals(code))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    private BudgetCycle cicloCerrado(LocalDate inicio, int secuencia) {
        BudgetPeriod periodo = BudgetPeriod.of(inicio, inicio.plusDays(29));
        BudgetCycle cycle = BudgetCycle.open(userId, CycleType.MONTHLY, periodo, secuencia);
        cycle.close();
        return cycleRepository.save(cycle);
    }

    private BudgetCycle cicloActivo(LocalDate inicio, int secuencia) {
        BudgetPeriod periodo = BudgetPeriod.of(inicio, inicio.plusDays(29));
        return cycleRepository.save(BudgetCycle.open(userId, CycleType.MONTHLY, periodo, secuencia));
    }

    private void renglon(BudgetCycle cycle, CycleItemType tipo, Long categoryId, String nombre, String monto) {
        items.save(CycleItem.materialize(
                cycle.getId(),
                tipo,
                tipo == CycleItemType.INCOME ? ItemSource.INCOME : ItemSource.EXPENSE,
                null,
                nombre,
                categoryId,
                Money.of(monto),
                cycle.getStartDate(),
                ItemStatus.PENDING,
                tipo == CycleItemType.INCOME ? null : Flexibility.IMPORTANT,
                0));
    }

    @Nested
    @DisplayName("la causa del deficit")
    class CausaDelDeficit {

        @Test
        @DisplayName("senala la categoria que mas subio respecto al ciclo anterior")
        void senalaLaCategoriaQueMasSubio() {
            Long despensa = categoria("GROCERIES");
            Long renta = categoria("HOUSING");

            BudgetCycle anterior = cicloCerrado(LocalDate.of(2026, 7, 1), 1);
            renglon(anterior, CycleItemType.INCOME, null, "Sueldo", "3000.00");
            renglon(anterior, CycleItemType.VARIABLE_EXPENSE, despensa, "Despensa", "1000.00");
            renglon(anterior, CycleItemType.FIXED_EXPENSE, renta, "Renta", "2000.00");

            BudgetCycle actual = cicloActivo(LocalDate.of(2026, 8, 1), 2);
            renglon(actual, CycleItemType.INCOME, null, "Sueldo", "3000.00");
            renglon(actual, CycleItemType.VARIABLE_EXPENSE, despensa, "Despensa", "1500.00");
            renglon(actual, CycleItemType.FIXED_EXPENSE, renta, "Renta", "2000.00");

            FinancialInsights resultado = insights.currentInsights(userId);

            assertThat(resultado.deficitCause()).isNotNull();
            assertThat(resultado.deficitCause().cycleId()).isEqualTo(actual.getPublicId());
            assertThat(resultado.deficitCause().categoryName()).isEqualTo("Despensa");
            assertThat(resultado.deficitCause().missing().amount()).isEqualByComparingTo("500.00");
            assertThat(resultado.deficitCause().previousAmount().amount()).isEqualByComparingTo("1000.00");
            assertThat(resultado.deficitCause().currentAmount().amount()).isEqualByComparingTo("1500.00");
            assertThat(resultado.deficitCause().increase().amount()).isEqualByComparingTo("500.00");
            assertThat(resultado.surplusAllocation()).isNull();
        }

        @Test
        @DisplayName("no se afirma nada sin un ciclo anterior con que comparar")
        void noSeAfirmaNadaSinCicloAnterior() {
            Long renta = categoria("HOUSING");

            BudgetCycle actual = cicloActivo(LocalDate.of(2026, 8, 1), 1);
            renglon(actual, CycleItemType.INCOME, null, "Sueldo", "1000.00");
            renglon(actual, CycleItemType.FIXED_EXPENSE, renta, "Renta", "2000.00");

            FinancialInsights resultado = insights.currentInsights(userId);

            assertThat(resultado.deficitCause()).isNull();
        }
    }

    @Nested
    @DisplayName("el reparto del remanente")
    class RepartoDelRemanente {

        @Test
        @DisplayName("llena la meta mas prioritaria primero, y sigue con la que resta")
        void llenaEnCascadaPorPrioridad() {
            Long renta = categoria("HOUSING");

            BudgetCycle actual = cicloActivo(LocalDate.of(2026, 8, 1), 1);
            renglon(actual, CycleItemType.INCOME, null, "Sueldo", "5000.00");
            renglon(actual, CycleItemType.FIXED_EXPENSE, renta, "Renta", "3000.00");

            savingsGoals.create(
                    userId, "Vacaciones", Money.of("1500.00"), null, ContributionMode.MANUAL, null, null, null);
            savingsGoals.create(
                    userId, "Fondo", Money.of("1000.00"), null, ContributionMode.MANUAL, null, null, null);

            FinancialInsights resultado = insights.currentInsights(userId);

            assertThat(resultado.deficitCause()).isNull();
            assertThat(resultado.surplusAllocation()).isNotNull();
            assertThat(resultado.surplusAllocation().cycleId()).isEqualTo(actual.getPublicId());
            assertThat(resultado.surplusAllocation().surplus().amount()).isEqualByComparingTo("2000.00");
            assertThat(resultado.surplusAllocation().shares()).hasSize(2);
            assertThat(resultado.surplusAllocation().shares().get(0).goalName()).isEqualTo("Vacaciones");
            assertThat(resultado.surplusAllocation().shares().get(0).amount().amount())
                    .isEqualByComparingTo("1500.00");
            assertThat(resultado.surplusAllocation().shares().get(1).goalName()).isEqualTo("Fondo");
            assertThat(resultado.surplusAllocation().shares().get(1).amount().amount())
                    .isEqualByComparingTo("500.00");
        }

        @Test
        @DisplayName("el remanente sigue apareciendo aunque no haya ninguna meta activa")
        void elRemanenteApareceSinMetas() {
            Long renta = categoria("HOUSING");

            BudgetCycle actual = cicloActivo(LocalDate.of(2026, 8, 1), 1);
            renglon(actual, CycleItemType.INCOME, null, "Sueldo", "5000.00");
            renglon(actual, CycleItemType.FIXED_EXPENSE, renta, "Renta", "3000.00");

            FinancialInsights resultado = insights.currentInsights(userId);

            assertThat(resultado.surplusAllocation()).isNotNull();
            assertThat(resultado.surplusAllocation().surplus().amount()).isEqualByComparingTo("2000.00");
            assertThat(resultado.surplusAllocation().shares()).isEmpty();
        }
    }

    @Nested
    @DisplayName("el crecimiento sostenido")
    class CrecimientoSostenido {

        @Test
        @DisplayName("aparece cuando una categoria sube 3 ciclos seguidos")
        void apareceConTresCiclosAlAlza() {
            Long salud = categoria("HEALTH");

            BudgetCycle c1 = cicloCerrado(LocalDate.of(2026, 6, 1), 1);
            renglon(c1, CycleItemType.VARIABLE_EXPENSE, salud, "Consulta", "100.00");

            BudgetCycle c2 = cicloCerrado(LocalDate.of(2026, 7, 1), 2);
            renglon(c2, CycleItemType.VARIABLE_EXPENSE, salud, "Consulta", "200.00");

            BudgetCycle c3 = cicloCerrado(LocalDate.of(2026, 8, 1), 3);
            renglon(c3, CycleItemType.VARIABLE_EXPENSE, salud, "Consulta", "300.00");

            FinancialInsights resultado = insights.currentInsights(userId);

            assertThat(resultado.categoryGrowth()).hasSize(1);
            assertThat(resultado.categoryGrowth().get(0).categoryName()).isEqualTo("Salud");
            assertThat(resultado.categoryGrowth().get(0).firstAmount().amount()).isEqualByComparingTo("100.00");
            assertThat(resultado.categoryGrowth().get(0).lastAmount().amount()).isEqualByComparingTo("300.00");
            assertThat(resultado.categoryGrowth().get(0).cycles()).isEqualTo(3);
        }

        @Test
        @DisplayName("no se afirma nada con menos de 3 ciclos de historia")
        void noSeAfirmaNadaConMenosDeTresCiclos() {
            Long salud = categoria("HEALTH");

            BudgetCycle c1 = cicloCerrado(LocalDate.of(2026, 7, 1), 1);
            renglon(c1, CycleItemType.VARIABLE_EXPENSE, salud, "Consulta", "100.00");

            BudgetCycle c2 = cicloCerrado(LocalDate.of(2026, 8, 1), 2);
            renglon(c2, CycleItemType.VARIABLE_EXPENSE, salud, "Consulta", "200.00");

            FinancialInsights resultado = insights.currentInsights(userId);

            assertThat(resultado.categoryGrowth()).isEmpty();
        }

        @Test
        @DisplayName("una caida rompe la racha")
        void unaCaidaRompeLaRacha() {
            Long salud = categoria("HEALTH");

            BudgetCycle c1 = cicloCerrado(LocalDate.of(2026, 6, 1), 1);
            renglon(c1, CycleItemType.VARIABLE_EXPENSE, salud, "Consulta", "300.00");

            BudgetCycle c2 = cicloCerrado(LocalDate.of(2026, 7, 1), 2);
            renglon(c2, CycleItemType.VARIABLE_EXPENSE, salud, "Consulta", "100.00");

            BudgetCycle c3 = cicloCerrado(LocalDate.of(2026, 8, 1), 3);
            renglon(c3, CycleItemType.VARIABLE_EXPENSE, salud, "Consulta", "400.00");

            FinancialInsights resultado = insights.currentInsights(userId);

            assertThat(resultado.categoryGrowth()).isEmpty();
        }
    }
}
