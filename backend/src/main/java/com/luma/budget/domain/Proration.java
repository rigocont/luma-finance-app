package com.luma.budget.domain;

import com.luma.common.model.Money;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** Reparte un importe en partes que suman exactamente el total. */
public final class Proration {

    private Proration() {}

    /**
     * Divide un importe en {@code parts} partes.
     *
     * <p>La suma de las partes es SIEMPRE igual al total. Dividir 1000 entre 3
     * no da tres veces 333.33 (que suman 999.99): da 333.34, 333.33 y 333.33.
     *
     * <p>Los centavos sobrantes se reparten en las primeras partes. Asi ninguna
     * se desvia mas de un centavo, y si el plan se interrumpe a la mitad lo
     * pagado va ligeramente por delante y no por detras.
     *
     * <p>Toda la aritmetica se hace en centavos con enteros: no hay punto
     * flotante en ningun paso.
     */
    public static List<Money> split(Money total, int parts) {
        if (parts <= 0) {
            throw new IllegalArgumentException("El numero de partes debe ser positivo: " + parts);
        }

        long cents = total.amount().movePointRight(2).longValueExact();
        long sign = cents < 0 ? -1L : 1L;
        long absoluteCents = Math.abs(cents);

        long base = absoluteCents / parts;
        long remainder = absoluteCents % parts;

        List<Money> shares = new ArrayList<>(parts);
        for (int i = 0; i < parts; i++) {
            long share = base + (i < remainder ? 1L : 0L);
            shares.add(Money.of(BigDecimal.valueOf(sign * share, 2), total.currency()));
        }
        return List.copyOf(shares);
    }

    /** La parte numero {@code index} de la division, contando desde cero. */
    public static Money share(Money total, int parts, int index) {
        if (index < 0 || index >= parts) {
            throw new IllegalArgumentException(
                    "Indice fuera de rango: %d de %d partes".formatted(index, parts));
        }
        return split(total, parts).get(index);
    }
}
