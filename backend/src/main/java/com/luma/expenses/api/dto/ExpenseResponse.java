package com.luma.expenses.api.dto;

import com.luma.common.model.Money;
import com.luma.common.web.MoneyDto;
import com.luma.expenses.domain.Expense;
import java.time.Instant;
import java.time.LocalDate;

/**
 * Un gasto.
 *
 * @param category la categoria completa y no solo su identificador: la lista la
 *     necesita para pintar cada fila, y devolverla evita que el cliente tenga
 *     que cruzarla contra el catalogo en cada render.
 * @param requiresReview el monto es una estimacion y el ciclo pedira
 *     confirmarlo. Es exactamente {@code kind == VARIABLE}, pero se expone
 *     calculado para que la interfaz no reimplemente la regla.
 */
public record ExpenseResponse(
        String id,
        String name,
        String kind,
        MoneyDto amount,
        ExpenseCategoryResponse category,
        String flexibility,
        String frequency,
        Integer dueDay,
        LocalDate startDate,
        LocalDate endDate,
        boolean active,
        boolean requiresReview,
        String notes,
        Instant createdAt) {

    public static ExpenseResponse from(
            Expense expense, ExpenseCategoryResponse category, String currency) {

        return new ExpenseResponse(
                expense.getPublicId(),
                expense.getName(),
                expense.getExpenseKind().name(),
                MoneyDto.from(Money.of(expense.getAmount(), currency)),
                category,
                expense.getFlexibility().name(),
                expense.getFrequency().name(),
                expense.getDueDay(),
                expense.getStartDate(),
                expense.getEndDate(),
                expense.isActive(),
                expense.isVariable(),
                expense.getNotes(),
                expense.getCreatedAt());
    }
}
