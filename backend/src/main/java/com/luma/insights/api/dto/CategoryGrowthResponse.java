package com.luma.insights.api.dto;

import com.luma.common.web.MoneyDto;
import com.luma.insights.domain.CategoryGrowth;

/** Una categoria que lleva una racha de ciclos seguidos al alza. */
public record CategoryGrowthResponse(String categoryName, MoneyDto firstAmount, MoneyDto lastAmount, int cycles) {

    public static CategoryGrowthResponse from(CategoryGrowth growth) {
        return new CategoryGrowthResponse(
                growth.categoryName(),
                MoneyDto.from(growth.firstAmount()),
                MoneyDto.from(growth.lastAmount()),
                growth.cycles());
    }
}
