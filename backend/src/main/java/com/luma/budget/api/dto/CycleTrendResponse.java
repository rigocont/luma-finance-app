package com.luma.budget.api.dto;

import com.luma.budget.application.BudgetCycleService;
import com.luma.common.web.MoneyDto;

/**
 * Un ciclo anterior con sus totales, para comparar.
 *
 * <p>Llegan los dos juegos de totales, el presupuestado y el real. La diferencia
 * entre ambos es lo que dice si el ciclo se cumplio o se desvio, y quedarse con
 * uno solo esconde justo eso.
 */
public record CycleTrendResponse(
        String cycleId,
        PeriodDto period,
        String status,
        int sequenceNumber,
        String state,
        BudgetTotalsDto planned,
        BudgetTotalsDto actual,
        ChangeDto outflowChange) {

    /**
     * Como cambio una cifra respecto al ciclo anterior.
     *
     * @param direction UP, DOWN o SAME.
     * @param amount la magnitud, siempre positiva.
     */
    public record ChangeDto(String direction, MoneyDto amount) {

        static ChangeDto from(BudgetCycleService.Change change) {
            return change == null
                    ? null
                    : new ChangeDto(change.direction(), MoneyDto.from(change.amount()));
        }
    }

    public static CycleTrendResponse from(BudgetCycleService.CycleTrend trend) {
        return new CycleTrendResponse(
                trend.cycle().getPublicId(),
                PeriodDto.from(trend.cycle().getCycleType(), trend.cycle().period()),
                trend.cycle().getStatus().name(),
                trend.cycle().getSequenceNumber(),
                trend.result().state().name(),
                BudgetTotalsDto.from(trend.result().planned()),
                BudgetTotalsDto.from(trend.result().actual()),
                ChangeDto.from(trend.outflowChange()));
    }
}
