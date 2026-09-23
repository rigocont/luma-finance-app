package com.luma.savings.domain;

import com.luma.budget.domain.BudgetPeriod;
import com.luma.budget.domain.CyclePlanner;
import com.luma.budget.domain.Proration;
import com.luma.common.model.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Optional;

/**
 * Cuanto hay que apartar por ciclo para alcanzar una meta.
 *
 * <p>Codigo puro, como el motor presupuestal. Responde dos preguntas del
 * producto: "cuanto necesito ahorrar para alcanzar esta meta" y "cuanto tardare
 * al ritmo actual".
 */
public final class SavingsPlanCalculator {

    private SavingsPlanCalculator() {}

    /**
     * Aporte necesario en cada ciclo para llegar a la meta en su fecha objetivo.
     *
     * <p>Reparte lo que falta entre los ciclos restantes, incluido el actual. La
     * suma de todos los aportes es exactamente lo que falta: no sobra ni falta
     * un centavo al final.
     *
     * <p>Casos que devuelven cero: la meta ya se alcanzo, o lo ahorrado supera
     * el objetivo.
     *
     * <p>Si la fecha objetivo ya paso, lo que falta se concentra en el ciclo
     * actual. Es lo honesto: no se puede repartir en ciclos que no existen.
     */
    public static Money contributionPerCycle(
            Money targetAmount,
            Money currentAmount,
            LocalDate targetDate,
            BudgetPeriod currentPeriod,
            CyclePlanner planner) {

        Money remaining = targetAmount.subtract(currentAmount);
        if (!remaining.isPositive()) {
            return Money.zero(targetAmount.currency());
        }

        if (targetDate == null) {
            throw new IllegalArgumentException(
                    "Una meta sin fecha objetivo no puede calcular su aporte por ciclo; "
                            + "usa FIXED_PER_CYCLE o MANUAL");
        }

        int remainingCycles = planner.countPeriodsUntil(currentPeriod, targetDate);
        return Proration.share(remaining, remainingCycles, 0);
    }

    /** Cuantos ciclos faltan para la fecha objetivo, contando el actual. */
    public static int remainingCycles(
            LocalDate targetDate, BudgetPeriod currentPeriod, CyclePlanner planner) {
        return planner.countPeriodsUntil(currentPeriod, targetDate);
    }

    /**
     * En que ciclo se alcanzaria la meta aportando {@code perCycle} cada vez.
     *
     * <p>Vacio si el aporte es cero o negativo: a ese ritmo no se llega nunca, y
     * decirlo es mas util que devolver una fecha inventada.
     */
    public static Optional<BudgetPeriod> projectedCompletion(
            Money targetAmount,
            Money currentAmount,
            Money perCycle,
            BudgetPeriod currentPeriod,
            CyclePlanner planner) {

        Money remaining = targetAmount.subtract(currentAmount);
        if (!remaining.isPositive()) {
            return Optional.of(currentPeriod);
        }
        if (!perCycle.isPositive()) {
            return Optional.empty();
        }

        Money accumulated = Money.zero(targetAmount.currency());
        BudgetPeriod cursor = currentPeriod;

        for (int i = 0; i < CyclePlannerLimits.MAX_PROJECTED_CYCLES; i++) {
            accumulated = accumulated.add(perCycle);
            if (!accumulated.subtract(remaining).isNegative()) {
                return Optional.of(cursor);
            }
            cursor = planner.next(cursor);
        }
        return Optional.empty();
    }

    /** Porcentaje alcanzado, de 0 a 100, redondeado a entero. */
    public static int progressPercentage(Money targetAmount, Money currentAmount) {
        if (!targetAmount.isPositive()) {
            return 0;
        }
        if (!currentAmount.subtract(targetAmount).isNegative()) {
            return 100;
        }
        return currentAmount
                .amount()
                .multiply(BigDecimal.valueOf(100))
                .divide(targetAmount.amount(), 0, RoundingMode.DOWN)
                .intValue();
    }

    /** Tope de las proyecciones: 50 anos de ciclos quincenales. */
    private static final class CyclePlannerLimits {
        private static final int MAX_PROJECTED_CYCLES = 1_200;

        private CyclePlannerLimits() {}
    }
}
