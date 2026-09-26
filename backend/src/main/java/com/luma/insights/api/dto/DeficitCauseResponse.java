package com.luma.insights.api.dto;

import com.luma.common.web.MoneyDto;
import com.luma.insights.domain.DeficitCause;

/**
 * Por que no alcanzo el ciclo: la categoria que mas subio respecto al ciclo
 * anterior, con las cifras que respaldan la afirmacion.
 *
 * <p>Solo aparece cuando de verdad hay con que compararse; ver
 * {@code InsightsService}.
 */
public record DeficitCauseResponse(
        String cycleId,
        MoneyDto missing,
        String categoryName,
        MoneyDto previousAmount,
        MoneyDto currentAmount,
        MoneyDto increase) {

    public static DeficitCauseResponse from(DeficitCause cause) {
        return new DeficitCauseResponse(
                cause.cycleId(),
                MoneyDto.from(cause.missing()),
                cause.categoryName(),
                MoneyDto.from(cause.previousAmount()),
                MoneyDto.from(cause.currentAmount()),
                MoneyDto.from(cause.increase()));
    }
}
