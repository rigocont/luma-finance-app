package com.luma.savings.api.dto;

import com.luma.common.model.Money;
import com.luma.common.web.MoneyDto;
import com.luma.savings.domain.SavingsContribution;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Un movimiento de la meta.
 *
 * <p>El monto de un retiro viaja NEGATIVO, tal como esta guardado. La interfaz
 * lo presenta con su signo; el transporte no lo esconde.
 */
public record SavingsContributionResponse(
        String id,
        MoneyDto amount,
        LocalDate date,
        String type,
        boolean fromCycle,
        String notes,
        Instant createdAt) {

    public static SavingsContributionResponse from(
            SavingsContribution contribution, String currency) {

        return new SavingsContributionResponse(
                contribution.getPublicId(),
                MoneyDto.from(Money.of(contribution.getAmount(), currency)),
                contribution.getContributionDate(),
                contribution.getType().name(),
                contribution.getCycleItemId() != null,
                contribution.getNotes(),
                contribution.getCreatedAt());
    }
}
