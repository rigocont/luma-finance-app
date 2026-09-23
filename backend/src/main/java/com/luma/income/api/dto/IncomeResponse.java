package com.luma.income.api.dto;

import com.luma.common.model.Money;
import com.luma.common.web.MoneyDto;
import com.luma.income.domain.Income;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Un ingreso.
 *
 * @param requiresReview el monto es una estimacion y el ciclo pedira
 *     confirmarlo. Se expone calculado para que la interfaz pueda advertirlo al
 *     capturar, sin tener que reimplementar la regla.
 */
public record IncomeResponse(
        String id,
        String name,
        String type,
        MoneyDto amount,
        String frequency,
        Integer expectedDay,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        boolean requiresReview,
        String notes,
        Instant createdAt) {

    public static IncomeResponse from(Income income, String currency) {
        return new IncomeResponse(
                income.getPublicId(),
                income.getName(),
                income.getIncomeType().name(),
                MoneyDto.from(Money.of(income.getAmount(), currency)),
                income.getFrequency().name(),
                income.getExpectedDay(),
                income.getStartDate(),
                income.getEndDate(),
                income.isActive(),
                income.requiresReview(),
                income.getNotes(),
                income.getCreatedAt());
    }
}
