package com.luma.budget.domain;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Cuando ocurre algo que se repite, dentro de un periodo dado.
 *
 * <p>Responde la pregunta que hace posible el presupuesto: "de este gasto
 * mensual que vence el dia 20, cuantas veces me toca pagarlo en esta quincena".
 * La respuesta puede ser ninguna, una o varias: un gasto mensual cae dos veces
 * en un ciclo bimestral.
 *
 * @param frequency cada cuanto se repite
 * @param dayOfMonth dia del mes de referencia. Si es nulo se usa el dia de
 *     {@code startDate}. Se recorta al ultimo dia de los meses cortos.
 * @param startDate desde cuando aplica. Tambien define el dia y mes de las
 *     recurrencias anuales y la alineacion de las bimestrales.
 * @param endDate hasta cuando aplica, o nulo si no termina
 */
public record RecurrenceSchedule(
        Frequency frequency, Integer dayOfMonth, LocalDate startDate, LocalDate endDate) {

    public RecurrenceSchedule {
        Objects.requireNonNull(frequency, "frequency");
        Objects.requireNonNull(startDate, "startDate");

        if (dayOfMonth != null && (dayOfMonth < 1 || dayOfMonth > 31)) {
            throw new IllegalArgumentException("El dia del mes debe estar entre 1 y 31: " + dayOfMonth);
        }
        if (endDate != null && endDate.isBefore(startDate)) {
            throw new IllegalArgumentException(
                    "La fecha de fin no puede ser anterior al inicio: %s .. %s"
                            .formatted(startDate, endDate));
        }
    }

    public static RecurrenceSchedule monthly(int dayOfMonth, LocalDate startDate) {
        return new RecurrenceSchedule(Frequency.MONTHLY, dayOfMonth, startDate, null);
    }

    public static RecurrenceSchedule oneTime(LocalDate date) {
        return new RecurrenceSchedule(Frequency.ONE_TIME, null, date, date);
    }

    /** Las fechas en que esto ocurre dentro del periodo, en orden y sin repetidas. */
    public List<LocalDate> occurrencesIn(BudgetPeriod period) {
        List<LocalDate> candidates = switch (frequency) {
            case ONE_TIME -> List.of(startDate);
            case ANNUAL -> annualCandidates(period);
            case MONTHLY -> monthlyCandidates(period);
            case BIMONTHLY -> bimonthlyCandidates(period);
            case BIWEEKLY -> biweeklyCandidates(period);
        };

        List<LocalDate> result = new ArrayList<>();
        for (LocalDate candidate : candidates) {
            if (appliesOn(candidate) && period.contains(candidate) && !result.contains(candidate)) {
                result.add(candidate);
            }
        }

        Collections.sort(result);
        return List.copyOf(result);
    }

    /** Cuantas veces ocurre en el periodo. */
    public int occurrenceCountIn(BudgetPeriod period) {
        return occurrencesIn(period).size();
    }

    /** Dentro de la vigencia: ni antes del inicio ni despues del fin. */
    private boolean appliesOn(LocalDate date) {
        if (date.isBefore(startDate)) {
            return false;
        }
        return endDate == null || !date.isAfter(endDate);
    }

    private int referenceDay() {
        return dayOfMonth != null ? dayOfMonth : startDate.getDayOfMonth();
    }

    private List<LocalDate> annualCandidates(BudgetPeriod period) {
        List<LocalDate> candidates = new ArrayList<>();
        for (int year = period.start().getYear(); year <= period.end().getYear(); year++) {
            // withYear recorta el 29 de febrero al 28 en anos no bisiestos.
            candidates.add(startDate.withYear(year));
        }
        return candidates;
    }

    private List<LocalDate> monthlyCandidates(BudgetPeriod period) {
        List<LocalDate> candidates = new ArrayList<>();
        for (YearMonth month : monthsTouchedBy(period)) {
            candidates.add(CyclePlanner.clampToMonth(month, referenceDay()));
        }
        return candidates;
    }

    private List<LocalDate> bimonthlyCandidates(BudgetPeriod period) {
        // Se alinea con el mes de inicio: si empieza en enero, cae en los meses
        // impares; si empieza en febrero, en los pares.
        int startParity = startDate.getMonthValue() % 2;

        List<LocalDate> candidates = new ArrayList<>();
        for (YearMonth month : monthsTouchedBy(period)) {
            if (month.getMonthValue() % 2 == startParity) {
                candidates.add(CyclePlanner.clampToMonth(month, referenceDay()));
            }
        }
        return candidates;
    }

    private List<LocalDate> biweeklyCandidates(BudgetPeriod period) {
        int first = referenceDay();
        int second = first + 15;

        List<LocalDate> candidates = new ArrayList<>();
        for (YearMonth month : monthsTouchedBy(period)) {
            candidates.add(CyclePlanner.clampToMonth(month, first));
            candidates.add(CyclePlanner.clampToMonth(month, second));
        }
        return candidates;
    }

    private static List<YearMonth> monthsTouchedBy(BudgetPeriod period) {
        List<YearMonth> months = new ArrayList<>();
        YearMonth cursor = YearMonth.from(period.start());
        YearMonth last = YearMonth.from(period.end());

        while (!cursor.isAfter(last)) {
            months.add(cursor);
            cursor = cursor.plusMonths(1);
        }
        return months;
    }
}
