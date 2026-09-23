package com.luma.budget.domain;

import com.luma.common.model.Money;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * El motor presupuestal.
 *
 * <p>Codigo puro: entra una lista inmutable, sale un resultado inmutable. Sin
 * Spring, sin JPA, sin acceso a base de datos. Es la pieza de la que depende
 * todo el valor del producto, asi que es la que mas tests tiene y la que corre
 * en milisegundos.
 *
 * <p>Reglas:
 *
 * <ul>
 *   <li>Los renglones {@code SKIPPED} no entran en ningun total.
 *   <li>Los totales presupuestados usan el monto planificado.
 *   <li>Los totales reales usan el monto real, o cero si todavia no ocurre.
 *   <li>El estado del ciclo se deriva del balance PRESUPUESTADO: es la pregunta
 *       "me va a alcanzar", no "me alcanzo".
 * </ul>
 */
public final class BudgetCalculator {

    /** Escala de las proporciones: cuatro decimales bastan para mostrar porcentajes. */
    private static final int RATE_SCALE = 4;

    private BudgetCalculator() {}

    public static BudgetResult calculate(List<PlannedItem> items, String currency) {
        if (items == null || items.isEmpty()) {
            return empty(currency);
        }

        Money plannedIncome = Money.zero(currency);
        Money plannedFixed = Money.zero(currency);
        Money plannedVariable = Money.zero(currency);
        Money plannedSavings = Money.zero(currency);

        Money actualIncome = Money.zero(currency);
        Money actualFixed = Money.zero(currency);
        Money actualVariable = Money.zero(currency);
        Money actualSavings = Money.zero(currency);

        int settled = 0;
        int pending = 0;
        int overdue = 0;
        int needsReview = 0;
        int skipped = 0;

        for (PlannedItem item : items) {
            switch (item.status()) {
                case SKIPPED -> skipped++;
                case OVERDUE -> overdue++;
                case NEEDS_REVIEW -> needsReview++;
                case PENDING -> pending++;
                case PAID, PARTIAL -> settled++;
            }

            if (!item.status().countsTowardBudget()) {
                continue;
            }

            Money plannedAmount = item.plannedAmount();
            Money realizedAmount = item.realizedAmount();

            switch (item.type()) {
                case INCOME -> {
                    plannedIncome = plannedIncome.add(plannedAmount);
                    actualIncome = actualIncome.add(realizedAmount);
                }
                case FIXED_EXPENSE -> {
                    plannedFixed = plannedFixed.add(plannedAmount);
                    actualFixed = actualFixed.add(realizedAmount);
                }
                case VARIABLE_EXPENSE -> {
                    plannedVariable = plannedVariable.add(plannedAmount);
                    actualVariable = actualVariable.add(realizedAmount);
                }
                case SAVING -> {
                    plannedSavings = plannedSavings.add(plannedAmount);
                    actualSavings = actualSavings.add(realizedAmount);
                }
            }
        }

        BudgetTotals planned =
                BudgetTotals.from(plannedIncome, plannedFixed, plannedVariable, plannedSavings);
        BudgetTotals actual =
                BudgetTotals.from(actualIncome, actualFixed, actualVariable, actualSavings);

        return new BudgetResult(
                planned,
                actual,
                BudgetState.of(planned.balance()),
                ratio(planned.savings(), planned.income()),
                ratio(planned.fixedExpenses().add(planned.variableExpenses()), planned.income()),
                items.size(),
                settled,
                pending,
                overdue,
                needsReview,
                skipped);
    }

    /**
     * Proporcion de una parte respecto al ingreso.
     *
     * <p>Sin ingreso no hay proporcion que calcular: devuelve cero en lugar de
     * dividir entre cero. Un ciclo recien creado, sin ingresos capturados
     * todavia, es un caso normal y no debe reventar.
     */
    private static BigDecimal ratio(Money part, Money income) {
        if (income.isZero() || income.isNegative()) {
            return BigDecimal.ZERO.setScale(RATE_SCALE);
        }
        return part.amount().divide(income.amount(), RATE_SCALE, RoundingMode.HALF_UP);
    }

    private static BudgetResult empty(String currency) {
        BudgetTotals zero = BudgetTotals.zero(currency);
        return new BudgetResult(
                zero,
                zero,
                BudgetState.BALANCED,
                BigDecimal.ZERO.setScale(RATE_SCALE),
                BigDecimal.ZERO.setScale(RATE_SCALE),
                0,
                0,
                0,
                0,
                0,
                0);
    }
}
