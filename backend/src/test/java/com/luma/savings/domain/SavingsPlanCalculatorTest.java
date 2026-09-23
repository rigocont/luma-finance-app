package com.luma.savings.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.luma.budget.domain.BudgetPeriod;
import com.luma.budget.domain.CyclePlanner;
import com.luma.budget.domain.CycleType;
import com.luma.common.model.Money;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("SavingsPlanCalculator")
class SavingsPlanCalculatorTest {

    private final CyclePlanner mensual = CyclePlanner.of(CycleType.MONTHLY);
    private final BudgetPeriod septiembre =
            CyclePlanner.of(CycleType.MONTHLY).periodContaining(LocalDate.parse("2026-09-13"));

    @Nested
    @DisplayName("aporte por ciclo")
    class ContributionPerCycle {

        @Test
        void repartePorPartesIgualesLoQueFalta() {
            Money aporte = SavingsPlanCalculator.contributionPerCycle(
                    Money.of("12000.00"),
                    Money.of("0.00"),
                    LocalDate.parse("2026-12-31"),
                    septiembre,
                    mensual);

            // Septiembre, octubre, noviembre y diciembre: cuatro ciclos.
            assertThat(aporte.amount()).isEqualTo(new BigDecimal("3000.00"));
        }

        @Test
        void descuentaLoYaAhorrado() {
            Money aporte = SavingsPlanCalculator.contributionPerCycle(
                    Money.of("50000.00"),
                    Money.of("20000.00"),
                    LocalDate.parse("2027-07-31"),
                    septiembre,
                    mensual);

            // Faltan 30,000 repartidos en 11 ciclos: 2,727.28 el primero.
            assertThat(aporte.amount()).isEqualTo(new BigDecimal("2727.28"));
        }

        @Test
        void unaMetaYaAlcanzadaNoNecesitaAporte() {
            Money aporte = SavingsPlanCalculator.contributionPerCycle(
                    Money.of("10000.00"),
                    Money.of("10000.00"),
                    LocalDate.parse("2027-01-31"),
                    septiembre,
                    mensual);

            assertThat(aporte.isZero()).isTrue();
        }

        @Test
        void haberAhorradoDeMasTampocoNecesitaAporte() {
            Money aporte = SavingsPlanCalculator.contributionPerCycle(
                    Money.of("10000.00"),
                    Money.of("12000.00"),
                    LocalDate.parse("2027-01-31"),
                    septiembre,
                    mensual);

            assertThat(aporte.isZero()).isTrue();
        }

        @Test
        void siLaFechaObjetivoYaPasoTodoCaeEnElCicloActual() {
            Money aporte = SavingsPlanCalculator.contributionPerCycle(
                    Money.of("5000.00"),
                    Money.of("1000.00"),
                    LocalDate.parse("2026-05-01"),
                    septiembre,
                    mensual);

            // No se puede repartir en ciclos que ya no existen.
            assertThat(aporte.amount()).isEqualTo(new BigDecimal("4000.00"));
        }

        @Test
        void unaMetaSinFechaNoPuedeCalcularSuAporte() {
            assertThatThrownBy(() -> SavingsPlanCalculator.contributionPerCycle(
                            Money.of("5000.00"), Money.of("0.00"), null, septiembre, mensual))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("fecha objetivo");
        }

        @Test
        void enCiclosQuincenalesElAporteEsLaMitad() {
            CyclePlanner quincenal = CyclePlanner.of(CycleType.BIWEEKLY);
            BudgetPeriod primeraQuincena =
                    quincenal.periodContaining(LocalDate.parse("2026-09-05"));

            Money aporte = SavingsPlanCalculator.contributionPerCycle(
                    Money.of("12000.00"),
                    Money.of("0.00"),
                    LocalDate.parse("2026-12-31"),
                    primeraQuincena,
                    quincenal);

            // Ocho quincenas de septiembre a diciembre: 1,500 cada una.
            assertThat(aporte.amount()).isEqualTo(new BigDecimal("1500.00"));
        }
    }

    @Nested
    @DisplayName("ciclos restantes")
    class RemainingCycles {

        @Test
        void cuentaElCicloActualYLosQueFaltan() {
            int ciclos = SavingsPlanCalculator.remainingCycles(
                    LocalDate.parse("2026-12-31"), septiembre, mensual);

            assertThat(ciclos).isEqualTo(4);
        }
    }

    @Nested
    @DisplayName("proyeccion")
    class Projection {

        @Test
        void diceEnQueCicloSeAlcanzaLaMeta() {
            Optional<BudgetPeriod> ciclo = SavingsPlanCalculator.projectedCompletion(
                    Money.of("1000.00"),
                    Money.of("0.00"),
                    Money.of("250.00"),
                    septiembre,
                    mensual);

            assertThat(ciclo).isPresent();
            assertThat(ciclo.get().start()).isEqualTo(LocalDate.parse("2026-12-01"));
        }

        @Test
        void unaMetaYaAlcanzadaSeCompletaEnElCicloActual() {
            Optional<BudgetPeriod> ciclo = SavingsPlanCalculator.projectedCompletion(
                    Money.of("1000.00"),
                    Money.of("1000.00"),
                    Money.of("250.00"),
                    septiembre,
                    mensual);

            assertThat(ciclo).contains(septiembre);
        }

        @Test
        void sinAporteNoHaySegunLaProyeccionFechaDeLlegada() {
            Optional<BudgetPeriod> ciclo = SavingsPlanCalculator.projectedCompletion(
                    Money.of("1000.00"),
                    Money.of("0.00"),
                    Money.zero(),
                    septiembre,
                    mensual);

            // Devolver una fecha inventada seria peor que decir que no se llega.
            assertThat(ciclo).isEmpty();
        }
    }

    @Nested
    @DisplayName("progreso")
    class Progress {

        @Test
        void calculaElPorcentajeAlcanzado() {
            assertThat(SavingsPlanCalculator.progressPercentage(
                            Money.of("50000.00"), Money.of("20000.00")))
                    .isEqualTo(40);
        }

        @Test
        void noPasaDeCien() {
            assertThat(SavingsPlanCalculator.progressPercentage(
                            Money.of("1000.00"), Money.of("1500.00")))
                    .isEqualTo(100);
        }

        @Test
        void sinNadaAhorradoEsCero() {
            assertThat(SavingsPlanCalculator.progressPercentage(
                            Money.of("1000.00"), Money.zero()))
                    .isZero();
        }

        @Test
        void truncaHaciaAbajoParaNoPrometerDeMas() {
            // 999 de 1000 es 99.9%: se muestra 99, no 100.
            assertThat(SavingsPlanCalculator.progressPercentage(
                            Money.of("1000.00"), Money.of("999.00")))
                    .isEqualTo(99);
        }

        @Test
        void unaMetaSinObjetivoNoTieneProgreso() {
            assertThat(SavingsPlanCalculator.progressPercentage(Money.zero(), Money.of("100.00")))
                    .isZero();
        }
    }

    @Test
    void unaMetaManualNoAfectaElPresupuesto() {
        assertThat(ContributionMode.MANUAL.affectsBudget()).isFalse();
        assertThat(ContributionMode.AUTO_BY_TARGET_DATE.affectsBudget()).isTrue();
        assertThat(ContributionMode.FIXED_PER_CYCLE.affectsBudget()).isTrue();
    }
}
