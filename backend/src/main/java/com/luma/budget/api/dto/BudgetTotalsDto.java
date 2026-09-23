package com.luma.budget.api.dto;

import com.luma.budget.domain.BudgetTotals;
import com.luma.common.web.MoneyDto;

/** Los cinco numeros de la formula del presupuesto. */
public record BudgetTotalsDto(
        MoneyDto income,
        MoneyDto fixedExpenses,
        MoneyDto variableExpenses,
        MoneyDto savings,
        MoneyDto balance,
        MoneyDto totalOutflow) {

    public static BudgetTotalsDto from(BudgetTotals totals) {
        return new BudgetTotalsDto(
                MoneyDto.from(totals.income()),
                MoneyDto.from(totals.fixedExpenses()),
                MoneyDto.from(totals.variableExpenses()),
                MoneyDto.from(totals.savings()),
                MoneyDto.from(totals.balance()),
                MoneyDto.from(totals.totalOutflow()));
    }
}
