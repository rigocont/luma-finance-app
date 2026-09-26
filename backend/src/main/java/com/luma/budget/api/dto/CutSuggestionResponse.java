package com.luma.budget.api.dto;

import com.luma.budget.domain.CutCandidate;
import com.luma.budget.domain.ItemStatus;
import com.luma.common.web.MoneyDto;

/**
 * Un renglon del que se podria recortar.
 *
 * <p>Se manda el {@code itemId} para que la interfaz pueda ofrecer la accion
 * —omitir el renglon, pausar la meta— sin volver a buscar cual era.
 *
 * @param estimated el monto todavia es una estimacion sin confirmar. La interfaz
 *     tiene que decirlo: proponer recortar una cifra que puede cambiar sin
 *     avisarlo seria dar por firme lo que no lo es.
 */
public record CutSuggestionResponse(
        String itemId,
        String name,
        String itemType,
        String status,
        String flexibility,
        MoneyDto amount,
        boolean estimated) {

    public static CutSuggestionResponse from(CutCandidate candidate, String currency) {
        return new CutSuggestionResponse(
                candidate.itemPublicId(),
                candidate.name(),
                candidate.type().name(),
                candidate.status().name(),
                candidate.flexibility() != null ? candidate.flexibility().name() : null,
                MoneyDto.from(candidate.amount()),
                candidate.status() == ItemStatus.NEEDS_REVIEW);
    }
}
