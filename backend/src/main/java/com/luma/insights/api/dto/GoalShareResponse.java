package com.luma.insights.api.dto;

import com.luma.common.web.MoneyDto;
import com.luma.insights.domain.GoalShare;

/** Cuanto del remanente se propone mandar a una meta especifica. */
public record GoalShareResponse(String goalId, String goalName, MoneyDto amount) {

    public static GoalShareResponse from(GoalShare share) {
        return new GoalShareResponse(share.goalId(), share.goalName(), MoneyDto.from(share.amount()));
    }
}
