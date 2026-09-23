package com.luma.budget.domain;

import com.luma.common.model.Money;

/**
 * Los cinco numeros de la formula del presupuesto.
 *
 * <pre>
 *   ingresos - gastos fijos - gastos variables - ahorro = balance
 * </pre>
 */
public record BudgetTotals(
        Money income,
        Money fixedExpenses,
        Money variableExpenses,
        Money savings,
        Money balance) {

    public static BudgetTotals zero(String currency) {
        Money zero = Money.zero(currency);
        return new BudgetTotals(zero, zero, zero, zero, zero);
    }

    static BudgetTotals from(Money income, Money fixed, Money variable, Money savings) {
        Money balance = income.subtract(fixed).subtract(variable).subtract(savings);
        return new BudgetTotals(income, fixed, variable, savings, balance);
    }

    /** Todo lo que sale, sin importar de que tipo. */
    public Money totalOutflow() {
        return fixedExpenses.add(variableExpenses).add(savings);
    }
}
