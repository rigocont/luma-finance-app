package com.luma.budget.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.luma.common.model.Money;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("BudgetCalculator")
class BudgetCalculatorTest {

    private static final String MXN = "MXN";

    private static PlannedItem ingreso(String monto) {
        return PlannedItem.planned(CycleItemType.INCOME, Money.of(monto));
    }

    private static PlannedItem fijo(String monto) {
        return PlannedItem.planned(CycleItemType.FIXED_EXPENSE, Money.of(monto));
    }

    private static PlannedItem variable(String monto) {
        return PlannedItem.planned(CycleItemType.VARIABLE_EXPENSE, Money.of(monto));
    }

    private static PlannedItem ahorro(String monto) {
        return PlannedItem.planned(CycleItemType.SAVING, Money.of(monto));
    }

    @Nested
    @DisplayName("la formula del presupuesto")
    class Formula {

        @Test
        void calculaElBalanceDeUnaQuincenaCompleta() {
            // El ejemplo del documento de arquitectura:
            //   12,500 - 7,800 - 2,100 - 1,500 = 1,100
            List<PlannedItem> renglones = List.of(
                    ingreso("12500.00"),
                    fijo("4500.00"),
                    fijo("890.00"),
                    fijo("610.00"),
                    fijo("1800.00"),
                    variable("1400.00"),
                    variable("700.00"),
                    ahorro("900.00"),
                    ahorro("600.00"));

            BudgetResult resultado = BudgetCalculator.calculate(renglones, MXN);

            assertThat(resultado.planned().income().amount()).isEqualTo(new BigDecimal("12500.00"));
            assertThat(resultado.planned().fixedExpenses().amount()).isEqualTo(new BigDecimal("7800.00"));
            assertThat(resultado.planned().variableExpenses().amount()).isEqualTo(new BigDecimal("2100.00"));
            assertThat(resultado.planned().savings().amount()).isEqualTo(new BigDecimal("1500.00"));
            assertThat(resultado.planned().balance().amount()).isEqualTo(new BigDecimal("1100.00"));
            assertThat(resultado.state()).isEqualTo(BudgetState.SURPLUS);
        }

        @Test
        void detectaUnDeficit() {
            List<PlannedItem> renglones = List.of(
                    ingreso("12500.00"), fijo("7800.00"), variable("3940.00"), ahorro("1500.00"));

            BudgetResult resultado = BudgetCalculator.calculate(renglones, MXN);

            assertThat(resultado.planned().balance().amount()).isEqualTo(new BigDecimal("-740.00"));
            assertThat(resultado.state()).isEqualTo(BudgetState.DEFICIT);
        }

        @Test
        void reconoceUnPresupuestoJusto() {
            List<PlannedItem> renglones =
                    List.of(ingreso("10000.00"), fijo("8000.00"), ahorro("2000.00"));

            BudgetResult resultado = BudgetCalculator.calculate(renglones, MXN);

            assertThat(resultado.planned().balance().isZero()).isTrue();
            assertThat(resultado.state()).isEqualTo(BudgetState.BALANCED);
        }

        @Test
        void elTotalDeSalidasSumaGastosYAhorro() {
            BudgetResult resultado = BudgetCalculator.calculate(
                    List.of(ingreso("10000.00"), fijo("5000.00"), variable("1000.00"), ahorro("2000.00")),
                    MXN);

            assertThat(resultado.planned().totalOutflow().amount()).isEqualTo(new BigDecimal("8000.00"));
        }

        @Test
        void unCicloSinRenglonesEstaBalanceadoYNoRevienta() {
            BudgetResult resultado = BudgetCalculator.calculate(List.of(), MXN);

            assertThat(resultado.state()).isEqualTo(BudgetState.BALANCED);
            assertThat(resultado.planned().balance().isZero()).isTrue();
            assertThat(resultado.itemCount()).isZero();
        }
    }

    @Nested
    @DisplayName("renglones omitidos")
    class Skipped {

        @Test
        void unRenglonOmitidoNoEntraEnNingunTotal() {
            List<PlannedItem> renglones = List.of(
                    ingreso("10000.00"),
                    fijo("3000.00"),
                    PlannedItem.skipped(CycleItemType.FIXED_EXPENSE, Money.of("5000.00")));

            BudgetResult resultado = BudgetCalculator.calculate(renglones, MXN);

            assertThat(resultado.planned().fixedExpenses().amount()).isEqualTo(new BigDecimal("3000.00"));
            assertThat(resultado.planned().balance().amount()).isEqualTo(new BigDecimal("7000.00"));
            assertThat(resultado.skippedCount()).isEqualTo(1);
            assertThat(resultado.itemCount()).isEqualTo(3);
        }

        @Test
        void omitirEsDistintoDeNoHaberPagado() {
            // Mismo monto, distinto estado: pendiente SI cuenta en el
            // presupuesto, omitido NO.
            BudgetResult conPendiente = BudgetCalculator.calculate(
                    List.of(ingreso("1000.00"), fijo("400.00")), MXN);
            BudgetResult conOmitido = BudgetCalculator.calculate(
                    List.of(
                            ingreso("1000.00"),
                            PlannedItem.skipped(CycleItemType.FIXED_EXPENSE, Money.of("400.00"))),
                    MXN);

            assertThat(conPendiente.planned().balance().amount()).isEqualTo(new BigDecimal("600.00"));
            assertThat(conOmitido.planned().balance().amount()).isEqualTo(new BigDecimal("1000.00"));
        }
    }

    @Nested
    @DisplayName("presupuestado frente a real")
    class PlannedVersusActual {

        @Test
        void loRealEsCeroMientrasNadaOcurre() {
            BudgetResult resultado =
                    BudgetCalculator.calculate(List.of(ingreso("1000.00"), fijo("400.00")), MXN);

            assertThat(resultado.planned().balance().amount()).isEqualTo(new BigDecimal("600.00"));
            assertThat(resultado.actual().income().isZero()).isTrue();
            assertThat(resultado.actual().fixedExpenses().isZero()).isTrue();
        }

        @Test
        void loRealUsaElMontoConfirmado() {
            List<PlannedItem> renglones = List.of(
                    PlannedItem.settled(CycleItemType.INCOME, Money.of("1000.00"), Money.of("1000.00")),
                    PlannedItem.settled(
                            CycleItemType.FIXED_EXPENSE, Money.of("400.00"), Money.of("450.00")));

            BudgetResult resultado = BudgetCalculator.calculate(renglones, MXN);

            // El presupuesto decia 600 de sobra; en la realidad quedaron 550.
            assertThat(resultado.planned().balance().amount()).isEqualTo(new BigDecimal("600.00"));
            assertThat(resultado.actual().balance().amount()).isEqualTo(new BigDecimal("550.00"));
        }

        @Test
        void elEstadoDelCicloSeDerivaDeLoPresupuestado() {
            // Nada se ha pagado todavia, asi que lo real seria un superavit
            // enorme. El estado debe responder "me va a alcanzar", no "me
            // alcanzo hasta ahora".
            List<PlannedItem> renglones = List.of(ingreso("1000.00"), fijo("1500.00"));

            BudgetResult resultado = BudgetCalculator.calculate(renglones, MXN);

            assertThat(resultado.state()).isEqualTo(BudgetState.DEFICIT);
        }
    }

    @Nested
    @DisplayName("proporciones")
    class Ratios {

        @Test
        void calculaLaProporcionDeAhorroYDeGasto() {
            List<PlannedItem> renglones = List.of(
                    ingreso("12500.00"), fijo("7800.00"), variable("2100.00"), ahorro("1500.00"));

            BudgetResult resultado = BudgetCalculator.calculate(renglones, MXN);

            assertThat(resultado.savingsRate()).isEqualTo(new BigDecimal("0.1200"));
            assertThat(resultado.expenseRate()).isEqualTo(new BigDecimal("0.7920"));
        }

        @Test
        void sinIngresoLasProporcionesSonCeroYNoHayDivisionPorCero() {
            BudgetResult resultado = BudgetCalculator.calculate(List.of(fijo("500.00")), MXN);

            assertThat(resultado.savingsRate()).isEqualTo(new BigDecimal("0.0000"));
            assertThat(resultado.expenseRate()).isEqualTo(new BigDecimal("0.0000"));
            assertThat(resultado.state()).isEqualTo(BudgetState.DEFICIT);
        }
    }

    @Nested
    @DisplayName("conteos por estado")
    class Counts {

        @Test
        void cuentaCadaEstadoPorSeparado() {
            List<PlannedItem> renglones = List.of(
                    ingreso("1000.00"),
                    new PlannedItem(CycleItemType.FIXED_EXPENSE, ItemStatus.PAID, Money.of("100.00"), Money.of("100.00")),
                    new PlannedItem(CycleItemType.FIXED_EXPENSE, ItemStatus.OVERDUE, Money.of("200.00"), null),
                    new PlannedItem(CycleItemType.VARIABLE_EXPENSE, ItemStatus.NEEDS_REVIEW, Money.of("300.00"), null),
                    PlannedItem.skipped(CycleItemType.VARIABLE_EXPENSE, Money.of("400.00")));

            BudgetResult resultado = BudgetCalculator.calculate(renglones, MXN);

            assertThat(resultado.itemCount()).isEqualTo(5);
            assertThat(resultado.pendingCount()).isEqualTo(1);
            assertThat(resultado.settledCount()).isEqualTo(1);
            assertThat(resultado.overdueCount()).isEqualTo(1);
            assertThat(resultado.needsReviewCount()).isEqualTo(1);
            assertThat(resultado.skippedCount()).isEqualTo(1);
            assertThat(resultado.requiresReview()).isTrue();
            assertThat(resultado.hasOverduePayments()).isTrue();
        }

        @Test
        void unRenglonVencidoSiCuentaEnElPresupuesto() {
            // Que un pago este vencido no significa que desaparezca: sigue
            // debiendose y sigue restando.
            List<PlannedItem> renglones = List.of(
                    ingreso("1000.00"),
                    new PlannedItem(CycleItemType.FIXED_EXPENSE, ItemStatus.OVERDUE, Money.of("400.00"), null));

            BudgetResult resultado = BudgetCalculator.calculate(renglones, MXN);

            assertThat(resultado.planned().balance().amount()).isEqualTo(new BigDecimal("600.00"));
        }
    }

    @Nested
    @DisplayName("estados")
    class States {

        @Test
        void seDerivanDelSignoDelBalance() {
            assertThat(BudgetState.of(Money.of("-0.01"))).isEqualTo(BudgetState.DEFICIT);
            assertThat(BudgetState.of(Money.zero())).isEqualTo(BudgetState.BALANCED);
            assertThat(BudgetState.of(Money.of("0.01"))).isEqualTo(BudgetState.SURPLUS);
        }

        @Test
        void unPagoCriticoNuncaSePuedePosponer() {
            assertThat(Flexibility.CRITICAL.canBeDeferred()).isFalse();
            assertThat(Flexibility.IMPORTANT.canBeDeferred()).isTrue();
            assertThat(Flexibility.FLEXIBLE.canBeDeferred()).isTrue();
        }
    }
}
