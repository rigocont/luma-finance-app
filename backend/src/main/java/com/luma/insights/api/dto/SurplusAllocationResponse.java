package com.luma.insights.api.dto;

import com.luma.common.web.MoneyDto;
import com.luma.insights.domain.SurplusAllocation;
import java.util.List;

/**
 * Como repartir el remanente del ciclo entre las metas activas.
 *
 * <p>{@code shares} viene vacia cuando no hay metas activas: el remanente
 * sigue siendo real, solo no hay a donde proponer que vaya.
 */
public record SurplusAllocationResponse(String cycleId, MoneyDto surplus, List<GoalShareResponse> shares) {

    public static SurplusAllocationResponse from(SurplusAllocation allocation) {
        return new SurplusAllocationResponse(
                allocation.cycleId(),
                MoneyDto.from(allocation.surplus()),
                allocation.shares().stream().map(GoalShareResponse::from).toList());
    }
}
