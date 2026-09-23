package com.luma.budget.domain;

import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Calcula los periodos de los ciclos presupuestales.
 *
 * <p>Es la pieza mas delicada del producto y la razon por la que existe como
 * codigo puro: los casos borde de fechas (dia 31 en meses de 30, febrero
 * bisiesto, cruces de ano) se prueban con tests instantaneos y sin base de
 * datos.
 *
 * <p>Garantia de diseno: los periodos consecutivos son contiguos y no se
 * traslapan. {@code next(p).start()} siempre es {@code p.end() + 1 dia}.
 */
public final class CyclePlanner {

    public static final int DEFAULT_ANCHOR_DAY = 1;

    /** Tope de seguridad para las iteraciones: 50 anos de ciclos quincenales. */
    static final int MAX_ITERATIONS = 1_200;

    private final CycleType type;
    private final int anchorDay;

    private CyclePlanner(CycleType type, int anchorDay) {
        if (type == null) {
            throw new IllegalArgumentException("El tipo de ciclo es obligatorio");
        }
        if (anchorDay < 1 || anchorDay > 31) {
            throw new IllegalArgumentException(
                    "El dia de anclaje debe estar entre 1 y 31: " + anchorDay);
        }
        this.type = type;
        this.anchorDay = anchorDay;
    }

    public static CyclePlanner of(CycleType type, int anchorDay) {
        return new CyclePlanner(type, anchorDay);
    }

    public static CyclePlanner of(CycleType type) {
        return new CyclePlanner(type, DEFAULT_ANCHOR_DAY);
    }

    public CycleType type() {
        return type;
    }

    public int anchorDay() {
        return anchorDay;
    }

    /** El periodo que contiene la fecha dada. */
    public BudgetPeriod periodContaining(LocalDate date) {
        return switch (type) {
            case BIWEEKLY -> biweekly(date);
            case MONTHLY -> monthly(date);
            case BIMONTHLY -> bimonthly(date);
        };
    }

    /**
     * El periodo siguiente.
     *
     * <p>Se calcula pidiendo el periodo que contiene el dia posterior al fin del
     * actual. Asi la contiguidad no depende de una segunda implementacion que
     * pueda desincronizarse de {@link #periodContaining(LocalDate)}.
     */
    public BudgetPeriod next(BudgetPeriod period) {
        return periodContaining(period.end().plusDays(1));
    }

    public BudgetPeriod previous(BudgetPeriod period) {
        return periodContaining(period.start().minusDays(1));
    }

    /** Cuenta cuantos periodos hay desde {@code from} hasta contener {@code until}. */
    public int countPeriodsUntil(BudgetPeriod from, LocalDate until) {
        if (!until.isAfter(from.end())) {
            return 1;
        }

        int count = 1;
        BudgetPeriod cursor = from;
        while (cursor.end().isBefore(until) && count < MAX_ITERATIONS) {
            cursor = next(cursor);
            count++;
        }
        return count;
    }

    private BudgetPeriod biweekly(LocalDate date) {
        YearMonth month = YearMonth.from(date);
        if (date.getDayOfMonth() <= 15) {
            return new BudgetPeriod(month.atDay(1), month.atDay(15));
        }
        return new BudgetPeriod(month.atDay(16), month.atEndOfMonth());
    }

    private BudgetPeriod monthly(LocalDate date) {
        YearMonth month = YearMonth.from(date);
        LocalDate anchorThisMonth = clampToMonth(month, anchorDay);

        if (!date.isBefore(anchorThisMonth)) {
            LocalDate end = clampToMonth(month.plusMonths(1), anchorDay).minusDays(1);
            return new BudgetPeriod(anchorThisMonth, end);
        }

        LocalDate start = clampToMonth(month.minusMonths(1), anchorDay);
        return new BudgetPeriod(start, anchorThisMonth.minusDays(1));
    }

    private BudgetPeriod bimonthly(LocalDate date) {
        YearMonth month = YearMonth.from(date);
        // Los bimestres se alinean en meses impares: ene-feb, mar-abr, y asi.
        YearMonth startMonth = month.getMonthValue() % 2 == 1 ? month : month.minusMonths(1);
        LocalDate anchor = clampToMonth(startMonth, anchorDay);

        if (date.isBefore(anchor)) {
            startMonth = startMonth.minusMonths(2);
            anchor = clampToMonth(startMonth, anchorDay);
        }

        LocalDate end = clampToMonth(startMonth.plusMonths(2), anchorDay).minusDays(1);
        return new BudgetPeriod(anchor, end);
    }

    /**
     * El dia pedido dentro del mes, recortado al ultimo dia si no existe.
     *
     * <p>El dia 31 en un mes de 30 cae el 30; en febrero, el 28 o el 29 segun el
     * ano. Sin este recorte, un vencimiento el 31 desapareceria de medio
     * calendario.
     */
    public static LocalDate clampToMonth(YearMonth month, int day) {
        return month.atDay(Math.min(day, month.lengthOfMonth()));
    }
}
