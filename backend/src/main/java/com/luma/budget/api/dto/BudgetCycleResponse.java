package com.luma.budget.api.dto;

import com.luma.budget.domain.BudgetCycle;
import java.time.Instant;

/** Un ciclo presupuestal. */
public record BudgetCycleResponse(
        String id,
        PeriodDto period,
        String status,
        int sequenceNumber,
        Instant closedAt) {

    public static BudgetCycleResponse from(BudgetCycle cycle) {
        return new BudgetCycleResponse(
                cycle.getPublicId(),
                PeriodDto.from(cycle.getCycleType(), cycle.period()),
                cycle.getStatus().name(),
                cycle.getSequenceNumber(),
                cycle.getClosedAt());
    }
}
