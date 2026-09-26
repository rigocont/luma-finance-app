package com.luma.budget.domain;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * Lo que costo un mismo gasto en un ciclo anterior.
 *
 * <p>Es una proyeccion de consulta, no una entidad: la construye JPQL cruzando
 * el renglon con su ciclo. Existe porque la pregunta "cuanto fue la luz las
 * ultimas veces" cruza dos tablas y devolver entidades completas para
 * responderla traeria mucho mas de lo que la pantalla usa.
 *
 * <p>Solo aparecen renglones CONFIRMADOS: un monto planeado que nadie confirmo
 * no es historia, es un plan, y mezclarlos daria un promedio que no ocurrio.
 */
public record ItemHistoryEntry(
        Long sourceId,
        String cyclePublicId,
        LocalDate cycleStart,
        LocalDate cycleEnd,
        BigDecimal plannedAmount,
        BigDecimal actualAmount,
        LocalDate settledOn) {

    /**
     * El promedio de lo que costo. Vacio si no hay historia.
     *
     * <p>Se calcula aqui y no en la interfaz porque es una cifra de dinero, y
     * ninguna cifra de dinero se deriva en el cliente: dos decimales con el
     * mismo redondeo que el resto del producto, una sola vez y en un solo
     * lugar.
     */
    public static Optional<BigDecimal> averageActual(List<ItemHistoryEntry> entries) {
        if (entries.isEmpty()) {
            return Optional.empty();
        }

        BigDecimal total = entries.stream()
                .map(ItemHistoryEntry::actualAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return Optional.of(
                total.divide(BigDecimal.valueOf(entries.size()), 2, RoundingMode.HALF_UP));
    }
}
