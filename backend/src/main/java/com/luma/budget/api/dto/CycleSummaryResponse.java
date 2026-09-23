package com.luma.budget.api.dto;

import com.luma.budget.domain.BudgetCycle;
import com.luma.budget.domain.BudgetResult;

/**
 * El ciclo con su balance ya calculado.
 *
 * <p>Va junto a proposito: el cliente no debe hacer dos peticiones para pintar
 * el resumen, y menos aun sumar nada por su cuenta.
 */
public record CycleSummaryResponse(BudgetCycleResponse cycle, BudgetBalanceResponse balance) {

    public static CycleSummaryResponse from(BudgetCycle cycle, BudgetResult result) {
        return new CycleSummaryResponse(
                BudgetCycleResponse.from(cycle), BudgetBalanceResponse.from(result));
    }
}
