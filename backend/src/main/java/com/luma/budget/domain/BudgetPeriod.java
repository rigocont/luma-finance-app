package com.luma.budget.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

/**
 * Rango de fechas de un ciclo, con ambos extremos incluidos.
 *
 * <p>Fechas de calendario, sin hora ni zona horaria: el dia 1 es el dia 1 en
 * cualquier parte del mundo.
 */
public record BudgetPeriod(LocalDate start, LocalDate end) {

    public BudgetPeriod {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
        if (end.isBefore(start)) {
            throw new IllegalArgumentException(
                    "El fin del periodo no puede ser anterior al inicio: %s .. %s"
                            .formatted(start, end));
        }
    }

    public static BudgetPeriod of(LocalDate start, LocalDate end) {
        return new BudgetPeriod(start, end);
    }

    /** Extremos incluidos. */
    public boolean contains(LocalDate date) {
        return !date.isBefore(start) && !date.isAfter(end);
    }

    public boolean overlaps(BudgetPeriod other) {
        return !other.start.isAfter(end) && !other.end.isBefore(start);
    }

    /** Dias que abarca, contando ambos extremos. */
    public long lengthInDays() {
        return ChronoUnit.DAYS.between(start, end) + 1;
    }

    @Override
    public String toString() {
        return "%s..%s".formatted(start, end);
    }
}
