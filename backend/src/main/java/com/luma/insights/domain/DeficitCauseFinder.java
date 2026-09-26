package com.luma.insights.domain;

import com.luma.common.model.Money;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Cual categoria explica mejor que un ciclo no haya alcanzado.
 *
 * <p>Codigo puro. Compara los totales por categoria de dos ciclos y elige la
 * que mas subio, en dinero. No dice "esto causo el deficit" en un sentido
 * estricto -el deficit puede venir de varias cosas a la vez, o de un ingreso
 * que bajo- pero es lo unico que se puede afirmar sin adivinar: una cifra que
 * de verdad crecio, y por cuanto.
 *
 * <p>Si ninguna categoria subio, no hay nada honesto que senalar y el metodo
 * devuelve vacio. Es preferible no explicar nada a explicar algo que las
 * cifras no sostienen.
 */
public final class DeficitCauseFinder {

    private DeficitCauseFinder() {}

    /** El cambio de una categoria entre dos ciclos, con su identificador crudo. */
    public record CategoryDelta(Long categoryId, Money previousAmount, Money currentAmount) {

        public Money increase() {
            return currentAmount.subtract(previousAmount);
        }
    }

    /**
     * @param previous total planeado por categoria en el ciclo anterior.
     * @param current total planeado por categoria en el ciclo actual. Una
     *     categoria ausente en cualquiera de los dos mapas cuenta como cero -no
     *     habia gasto de esa categoria ese ciclo.
     * @param currency la moneda de los montos que se devuelven.
     */
    public static Optional<CategoryDelta> biggestIncrease(
            Map<Long, BigDecimal> previous, Map<Long, BigDecimal> current, String currency) {

        Set<Long> categorias = new HashSet<>(previous.keySet());
        categorias.addAll(current.keySet());

        return categorias.stream()
                .map(categoryId -> new CategoryDelta(
                        categoryId,
                        Money.of(previous.getOrDefault(categoryId, BigDecimal.ZERO), currency),
                        Money.of(current.getOrDefault(categoryId, BigDecimal.ZERO), currency)))
                .filter(delta -> delta.increase().isPositive())
                // Desempate por categoryId: sin el, un empate exacto en el
                // monto dependeria del orden de iteracion del set, y la misma
                // pregunta podria responder distinto entre una llamada y otra.
                .max(Comparator.comparing((CategoryDelta delta) -> delta.increase().amount())
                        .thenComparing(CategoryDelta::categoryId));
    }
}
