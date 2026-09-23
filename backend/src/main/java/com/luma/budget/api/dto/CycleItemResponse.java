package com.luma.budget.api.dto;

import com.luma.budget.domain.CycleItem;
import com.luma.common.model.Money;
import com.luma.common.web.MoneyDto;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Un renglon del ciclo.
 *
 * <p>{@code name} es el que tenia la plantilla cuando se genero el ciclo, no el
 * actual. Es intencional: la historia no cambia porque hoy se renombre un gasto.
 */
public record CycleItemResponse(
        String id,
        String itemType,
        String sourceType,
        String name,
        MoneyDto plannedAmount,
        MoneyDto actualAmount,
        LocalDate dueDate,
        String status,
        String flexibility,
        LocalDate settledOn,
        Instant settledAt,
        int displayOrder,
        String notes) {

    public static CycleItemResponse from(CycleItem item, String currency) {
        return new CycleItemResponse(
                item.getPublicId(),
                item.getItemType().name(),
                item.getSourceType().name(),
                item.getName(),
                MoneyDto.from(Money.of(item.getPlannedAmount(), currency)),
                item.getActualAmount() != null
                        ? MoneyDto.from(Money.of(item.getActualAmount(), currency))
                        : null,
                item.getDueDate(),
                item.getStatus().name(),
                item.getFlexibility() != null ? item.getFlexibility().name() : null,
                item.getSettledOn(),
                item.getSettledAt(),
                item.getDisplayOrder(),
                item.getNotes());
    }
}
