package com.luma.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.luma.budget.application.BudgetCycleService;
import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.Frequency;
import com.luma.common.error.BusinessRuleException;
import com.luma.common.model.Money;
import com.luma.income.application.IncomeService;
import com.luma.income.domain.IncomeType;
import com.luma.onboarding.application.OnboardingService;
import com.luma.support.IntegrationTest;
import com.luma.users.domain.User;
import com.luma.users.infrastructure.UserRepository;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

/**
 * El alta guiada contra la base real.
 *
 * <p>Lo que importa demostrar es que el asistente NO guarda un borrador: lo que
 * se captura en el son ingresos y gastos de verdad, y el estado que devuelve
 * sale de contarlos, no de un avance anotado en algun lado.
 */
@Transactional
@DisplayName("El alta guiada contra la base real")
class OnboardingIntegrationTest extends IntegrationTest {

    @Autowired
    UserRepository users;

    @Autowired
    IncomeService incomes;

    @Autowired
    OnboardingService onboarding;

    @Autowired
    BudgetCycleService cycles;

    private Long userId;

    @BeforeEach
    void crearUsuario() {
        User user = users.save(User.register(
                "qa+" + UUID.randomUUID() + "@luma.app", "Persona de prueba", "hash-irrelevante"));
        userId = user.getId();
    }

    /** Quincenal y con dia 10 para que caiga en cualquier periodo. */
    private void capturarIngreso() {
        incomes.create(
                userId,
                "Sueldo",
                IncomeType.RECURRENT,
                Money.of("18000.00"),
                Frequency.BIWEEKLY,
                10,
                LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).minusYears(1),
                null,
                null);
    }

    @Nested
    @DisplayName("el estado")
    class Estado {

        @Test
        @DisplayName("una cuenta nueva no ha terminado y no puede terminar")
        void cuentaNueva() {
            OnboardingService.State estado = onboarding.stateOf(userId);

            assertThat(estado.completed()).isFalse();
            assertThat(estado.canFinish()).isFalse();
            assertThat(estado.incomeCount()).isZero();
        }

        @Test
        @DisplayName("sin ingresos manda al paso de ingresos")
        void sinIngresos() {
            assertThat(onboarding.stateOf(userId).resumeStep())
                    .isEqualTo(OnboardingService.Step.INCOMES);
        }

        @Test
        @DisplayName("con un ingreso ya se puede terminar y se vuelve al resumen")
        void conUnIngreso() {
            capturarIngreso();

            OnboardingService.State estado = onboarding.stateOf(userId);

            assertThat(estado.incomeCount()).isEqualTo(1);
            assertThat(estado.canFinish()).isTrue();
            assertThat(estado.resumeStep()).isEqualTo(OnboardingService.Step.SUMMARY);
        }

        @Test
        @DisplayName("el conteo sale de los datos reales, no de un avance guardado")
        void cuentaLoQueHay() {
            // Es la prueba de que no hay borrador: el ingreso se creo por el
            // endpoint normal y el asistente lo ve igual.
            capturarIngreso();
            capturarIngreso();

            assertThat(onboarding.stateOf(userId).incomeCount()).isEqualTo(2);
        }
    }

    @Nested
    @DisplayName("terminar")
    class Terminar {

        @Test
        @DisplayName("marca la cuenta y abre el primer ciclo")
        void terminaYAbreCiclo() {
            capturarIngreso();

            BudgetCycle cycle = onboarding.complete(userId);

            assertThat(cycle).isNotNull();
            assertThat(users.findById(userId).orElseThrow().hasCompletedOnboarding()).isTrue();
            assertThat(cycles.currentCycle(userId)).isPresent();
        }

        @Test
        @DisplayName("el primer ciclo ya trae el ingreso materializado")
        void elCicloTraeLoCapturado() {
            capturarIngreso();

            BudgetCycle cycle = onboarding.complete(userId);

            assertThat(cycles.itemsOf(cycle, null, null)).isNotEmpty();
        }

        @Test
        @DisplayName("sin ingresos se rechaza")
        void sinIngresosSeRechaza() {
            assertThatThrownBy(() -> onboarding.complete(userId))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("ingreso");

            assertThat(users.findById(userId).orElseThrow().hasCompletedOnboarding()).isFalse();
        }

        @Test
        @DisplayName("terminar dos veces se rechaza")
        void dosVecesSeRechaza() {
            // Sin esto, el segundo intento abriria un ciclo de mas.
            capturarIngreso();
            onboarding.complete(userId);

            assertThatThrownBy(() -> onboarding.complete(userId))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    @DisplayName("hacerlo despues")
    class HacerloDespues {

        @Test
        @DisplayName("marca la cuenta sin abrir ciclo")
        void marcaSinCiclo() {
            onboarding.skip(userId);

            assertThat(users.findById(userId).orElseThrow().hasCompletedOnboarding()).isTrue();
            assertThat(cycles.currentCycle(userId)).isEmpty();
        }

        @Test
        @DisplayName("conserva lo que ya se habia capturado")
        void conservaLoCapturado() {
            capturarIngreso();

            onboarding.skip(userId);

            assertThat(onboarding.stateOf(userId).incomeCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("repetirlo no falla ni cambia nada")
        void esIdempotente() {
            onboarding.skip(userId);
            onboarding.skip(userId);

            assertThat(users.findById(userId).orElseThrow().hasCompletedOnboarding()).isTrue();
        }
    }
}
