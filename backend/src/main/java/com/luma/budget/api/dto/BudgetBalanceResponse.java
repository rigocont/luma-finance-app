package com.luma.budget.api.dto;

import com.luma.budget.domain.BudgetResult;
import java.math.BigDecimal;

/**
 * El resultado del motor presupuestal.
 *
 * <p>Trae {@code planned} y {@code actual} por separado: lo presupuestado
 * responde "me va a alcanzar", lo real responde "que ha pasado". El
 * {@code state} se deriva de lo presupuestado.
 *
 * <p>{@code state} es un codigo estable (DEFICIT, BALANCED, SURPLUS). La
 * traduccion a lenguaje normal la hace la interfaz, no la API.
 */
public record BudgetBalanceResponse(
        BudgetTotalsDto planned,
        BudgetTotalsDto actual,
        String state,
        BigDecimal savingsRate,
        BigDecimal expenseRate,
        CycleCountsDto counts,
        boolean requiresReview,
        boolean hasOverduePayments) {

    public static BudgetBalanceResponse from(BudgetResult result) {
        return new BudgetBalanceResponse(
                BudgetTotalsDto.from(result.planned()),
                BudgetTotalsDto.from(result.actual()),
                result.state().name(),
                result.savingsRate(),
                result.expenseRate(),
                CycleCountsDto.from(result),
                result.requiresReview(),
                result.hasOverduePayments());
    }
}
