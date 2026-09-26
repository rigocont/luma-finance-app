package com.luma.savings.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.luma.common.error.BusinessRuleException;
import com.luma.common.model.Money;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SavingsGoal")
class SavingsGoalTest {

    private static final String MXN = "MXN";
    private static final LocalDate OBJETIVO = LocalDate.of(2027, 12, 31);

    private static SavingsGoal fondo() {
        return SavingsGoal.create(
                1L, "Fondo de emergencia", Money.of("30000.00"), OBJETIVO,
                ContributionMode.AUTO_BY_TARGET_DATE, null, 1);
    }

    private static SavingsGoal conAporteFijo(String porCiclo) {
        return SavingsGoal.create(
                1L, "Vacaciones", Money.of("18000.00"), null,
                ContributionMode.FIXED_PER_CYCLE, Money.of(porCiclo), 2);
    }

    @Nested
    @DisplayName("al crearse")
    class Creacion {

        @Test
        void naceActivaYEnCero() {
            SavingsGoal goal = fondo();

            assertThat(goal.isActive()).isTrue();
            assertThat(goal.saved(MXN)).isEqualTo(Money.of("0.00"));
            assertThat(goal.remaining(MXN)).isEqualTo(Money.of("30000.00"));
        }

        @Test
        void rechazaUnaMetaEnCero() {
            assertThatThrownBy(() -> SavingsGoal.create(
                            1L, "Meta", Money.of("0.00"), OBJETIVO,
                            ContributionMode.AUTO_BY_TARGET_DATE, null, 1))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void elAporteFijoExigeUnMonto() {
            assertThatThrownBy(() -> SavingsGoal.create(
                            1L, "Meta", Money.of("100.00"), null,
                            ContributionMode.FIXED_PER_CYCLE, null, 1))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("cuanto");
        }

        @Test
        void elCalculoAutomaticoExigeUnaFecha() {
            // Sin fecha no hay ciclos entre los cuales repartir lo que falta.
            assertThatThrownBy(() -> SavingsGoal.create(
                            1L, "Meta", Money.of("100.00"), null,
                            ContributionMode.AUTO_BY_TARGET_DATE, null, 1))
                    .isInstanceOf(BusinessRuleException.class)
                    .hasMessageContaining("fecha");
        }

        @Test
        void elModoManualNoEntraEnElPresupuesto() {
            SavingsGoal goal = SavingsGoal.create(
                    1L, "Algun dia", Money.of("5000.00"), null,
                    ContributionMode.MANUAL, null, 1);

            assertThat(goal.isActive()).isTrue();
            assertThat(goal.affectsBudget()).isFalse();
        }
    }

    @Nested
    @DisplayName("el progreso")
    class Progreso {

        @Test
        void unaAportacionSubeElProgreso() {
            SavingsGoal goal = fondo();

            goal.applyContribution(Money.of("1000.00"));

            assertThat(goal.saved(MXN)).isEqualTo(Money.of("1000.00"));
            assertThat(goal.remaining(MXN)).isEqualTo(Money.of("29000.00"));
        }

        @Test
        void unRetiroLoBaja() {
            SavingsGoal goal = fondo();
            goal.applyContribution(Money.of("1000.00"));

            goal.applyContribution(Money.of("-400.00"));

            assertThat(goal.saved(MXN)).isEqualTo(Money.of("600.00"));
        }

        @Test
        void noSePuedeRetirarMasDeLoQueHay() {
            SavingsGoal goal = fondo();
            goal.applyContribution(Money.of("500.00"));

            assertThatThrownBy(() -> goal.applyContribution(Money.of("-600.00")))
                    .isInstanceOf(BusinessRuleException.class);

            assertThat(goal.saved(MXN)).isEqualTo(Money.of("500.00"));
        }

        @Test
        void loQueFaltaNuncaEsNegativo() {
            // Pasarse de la meta es bueno; una cifra negativa en "te falta" no
            // significa nada.
            SavingsGoal goal = conAporteFijo("1000.00");

            goal.applyContribution(Money.of("20000.00"));

            assertThat(goal.remaining(MXN)).isEqualTo(Money.of("0.00"));
        }
    }

    @Nested
    @DisplayName("alcanzar la meta")
    class Completar {

        @Test
        void llegarAlObjetivoLaMarcaComoAlcanzada() {
            SavingsGoal goal = conAporteFijo("1000.00");

            goal.applyContribution(Money.of("18000.00"));

            assertThat(goal.isCompleted()).isTrue();
        }

        @Test
        void unaMetaAlcanzadaYaNoRestaDelPresupuesto() {
            SavingsGoal goal = conAporteFijo("1000.00");
            goal.applyContribution(Money.of("18000.00"));

            assertThat(goal.affectsBudget()).isFalse();
        }

        @Test
        void subirLaMetaDespuesDeAlcanzarlaLaVuelveAPonerEnMarcha() {
            // Subir la meta es justo pedir seguir ahorrando. Dejarla marcada
            // como alcanzada obligaria a recrearla.
            SavingsGoal goal = conAporteFijo("1000.00");
            goal.applyContribution(Money.of("18000.00"));

            goal.changeTarget(Money.of("25000.00"));

            assertThat(goal.isCompleted()).isFalse();
            assertThat(goal.isActive()).isTrue();
        }

        @Test
        void retirarDespuesDeAlcanzarlaLaVuelveAPonerEnMarcha() {
            SavingsGoal goal = conAporteFijo("1000.00");
            goal.applyContribution(Money.of("18000.00"));

            goal.applyContribution(Money.of("-500.00"));

            assertThat(goal.isCompleted()).isFalse();
        }
    }

    @Nested
    @DisplayName("pausar y reanudar")
    class Pausa {

        @Test
        void pausarLaSacaDelPresupuestoSinPerderProgreso() {
            SavingsGoal goal = conAporteFijo("1000.00");
            goal.applyContribution(Money.of("2000.00"));

            goal.pause();

            assertThat(goal.affectsBudget()).isFalse();
            assertThat(goal.saved(MXN)).isEqualTo(Money.of("2000.00"));
        }

        @Test
        void reanudarLaDevuelveAlPresupuesto() {
            SavingsGoal goal = conAporteFijo("1000.00");
            goal.pause();

            goal.resume();

            assertThat(goal.affectsBudget()).isTrue();
        }

        @Test
        void cambiarAAporteFijoSinMontoSeRechaza() {
            SavingsGoal goal = fondo();

            assertThatThrownBy(() -> goal.changePlan(ContributionMode.FIXED_PER_CYCLE, null))
                    .isInstanceOf(BusinessRuleException.class);
        }

        @Test
        void cambiarACalculoAutomaticoSinFechaSeRechaza() {
            SavingsGoal goal = conAporteFijo("1000.00");

            assertThatThrownBy(() -> goal.changePlan(ContributionMode.AUTO_BY_TARGET_DATE, null))
                    .isInstanceOf(BusinessRuleException.class);
        }
    }

    @Nested
    @DisplayName("edicion parcial")
    class EdicionParcial {

        /**
         * Una edicion parcial manda solo lo que cambio. Si cambiar el modo sin
         * mandar el monto lo borrara, editar el nombre desde la pantalla
         * dejaria la meta sin aporte sin que nadie lo pidiera.
         */
        @Test
        void cambiarDeModoSinMontoConservaElQueYaHabia() {
            SavingsGoal goal = conAporteFijo("1500.00");

            goal.changeTargetDate(OBJETIVO);
            goal.changePlan(ContributionMode.AUTO_BY_TARGET_DATE, null);
            goal.changePlan(ContributionMode.FIXED_PER_CYCLE, null);

            assertThat(goal.plannedContribution(MXN)).isEqualTo(Money.of("1500.00"));
        }

        @Test
        void mandarSoloElIconoNoBorraElColor() {
            SavingsGoal goal = fondo();
            goal.changeLook("piggy-bank", "#F2B705");

            goal.changeLook("umbrella", null);

            assertThat(goal.getIcon()).isEqualTo("umbrella");
            assertThat(goal.getColor()).isEqualTo("#F2B705");
        }
    }
}
