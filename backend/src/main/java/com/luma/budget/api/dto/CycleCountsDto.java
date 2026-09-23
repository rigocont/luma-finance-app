package com.luma.budget.api.dto;

import com.luma.budget.domain.BudgetResult;

/** Cuantos renglones hay en cada estado. */
public record CycleCountsDto(
        int total, int settled, int pending, int overdue, int needsReview, int skipped) {

    public static CycleCountsDto from(BudgetResult result) {
        return new CycleCountsDto(
                result.itemCount(),
                result.settledCount(),
                result.pendingCount(),
                result.overdueCount(),
                result.needsReviewCount(),
                result.skippedCount());
    }
}
