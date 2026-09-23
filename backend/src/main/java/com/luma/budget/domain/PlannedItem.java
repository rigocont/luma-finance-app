package com.luma.budget.domain;

import com.luma.common.model.Money;
import java.util.Objects;

/**
 * Un renglon del ciclo, visto por la calculadora.
 *
 * <p>Es una copia inmutable, no una entidad: la calculadora no conoce JPA ni
 * necesita saber de donde vino el dato. Eso la hace probable en milisegundos.
 *
 * @param type que es, para la formula
 * @param status en que va
 * @param plannedAmount lo presupuestado
 * @param actualAmount lo que de verdad ocurrio, o nulo si aun no ocurre
 */
public record PlannedItem(
        CycleItemType type, ItemStatus status, Money plannedAmount, Money actualAmount) {

    public PlannedItem {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(plannedAmount, "plannedAmount");
    }

    public static PlannedItem planned(CycleItemType type, Money amount) {
        return new PlannedItem(type, ItemStatus.PENDING, amount, null);
    }

    public static PlannedItem settled(CycleItemType type, Money planned, Money actual) {
        return new PlannedItem(type, ItemStatus.PAID, planned, actual);
    }

    public static PlannedItem skipped(CycleItemType type, Money planned) {
        return new PlannedItem(type, ItemStatus.SKIPPED, planned, null);
    }

    /** Lo que de verdad ocurrio. Cero mientras no ocurra nada. */
    public Money realizedAmount() {
        return actualAmount != null ? actualAmount : Money.zero(plannedAmount.currency());
    }
}
