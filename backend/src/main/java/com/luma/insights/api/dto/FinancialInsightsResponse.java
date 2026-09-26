package com.luma.insights.api.dto;

import com.luma.insights.domain.FinancialInsights;
import java.util.List;

/**
 * El analisis financiero sin IA, completo.
 *
 * <p>{@code deficitCause} y {@code surplusAllocation} son mutuamente
 * excluyentes: un ciclo no puede estar en deficit y en remanente a la vez.
 * Ambos pueden venir nulos (ciclo en equilibrio, o sin ciclo abierto, o sin
 * datos suficientes para afirmar una causa).
 */
public record FinancialInsightsResponse(
        DeficitCauseResponse deficitCause,
        SurplusAllocationResponse surplusAllocation,
        List<CategoryGrowthResponse> categoryGrowth) {

    public static FinancialInsightsResponse from(FinancialInsights insights) {
        return new FinancialInsightsResponse(
                insights.deficitCause() != null ? DeficitCauseResponse.from(insights.deficitCause()) : null,
                insights.surplusAllocation() != null
                        ? SurplusAllocationResponse.from(insights.surplusAllocation())
                        : null,
                insights.categoryGrowth().stream().map(CategoryGrowthResponse::from).toList());
    }
}
