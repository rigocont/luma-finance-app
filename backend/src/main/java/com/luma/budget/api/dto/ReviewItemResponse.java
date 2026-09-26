package com.luma.budget.api.dto;

import com.luma.budget.application.CycleReviewService;
import com.luma.common.model.Money;
import com.luma.common.web.MoneyDto;
import java.time.LocalDate;

/**
 * Un renglon que pide revision.
 *
 * @param suggestedAmount lo que se confirmo del mismo gasto en el ciclo
 *     anterior. Nulo cuando no hay ciclo anterior, cuando ese ciclo no tuvo este
 *     gasto, o cuando lo tuvo y nadie lo confirmo. La interfaz propone este
 *     monto ya escrito en el campo; si es nulo propone el planeado.
 * @param suggestedFromStart el inicio del ciclo de donde sale la sugerencia, para
 *     poder decir DE CUANDO es. Una cifra sugerida sin fecha invita a aceptarla
 *     sin mirarla.
 */
public record ReviewItemResponse(
        CycleItemResponse item, MoneyDto suggestedAmount, LocalDate suggestedFromStart) {

    public static ReviewItemResponse from(CycleReviewService.ReviewItem review, String currency) {
        return new ReviewItemResponse(
                CycleItemResponse.from(review.item(), currency),
                review.suggestion() != null
                        ? MoneyDto.from(Money.of(review.suggestion(), currency))
                        : null,
                review.suggestedFrom() != null ? review.suggestedFrom().getStartDate() : null);
    }
}
