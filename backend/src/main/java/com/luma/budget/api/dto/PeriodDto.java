package com.luma.budget.api.dto;

import com.luma.budget.domain.BudgetPeriod;
import com.luma.budget.domain.CycleType;
import java.time.LocalDate;

/** El rango de fechas del ciclo, con ambos extremos incluidos. */
public record PeriodDto(String type, LocalDate start, LocalDate end) {

    public static PeriodDto from(CycleType type, BudgetPeriod period) {
        return new PeriodDto(type.name(), period.start(), period.end());
    }
}
