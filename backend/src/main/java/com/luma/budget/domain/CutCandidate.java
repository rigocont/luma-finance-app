package com.luma.budget.domain;

import com.luma.common.model.Money;

/**
 * Un renglon del que se PODRIA recortar, visto por el asesor de deficit.
 *
 * <p>Es una copia inmutable, igual que {@link PlannedItem}: el asesor no conoce
 * JPA ni de donde vino el dato, y por eso se puede probar en milisegundos.
 *
 * <p>Lleva mas campos que {@link PlannedItem} porque responde otra pregunta. A
 * la calculadora solo le importa cuanto y de que tipo; al asesor le importa
 * ademas que tan movible es y como se llama, porque su respuesta es una lista
 * que una persona va a leer y decidir.
 *
 * @param flexibility nulo en los renglones de ahorro: la flexibilidad es un
 *     atributo de los gastos.
 * @param goalPriority nulo en los gastos. En los ahorros, el orden que la
 *     persona le dio a la meta; un numero mas alto es menos prioritario.
 */
public record CutCandidate(
        String itemPublicId,
        String name,
        CycleItemType type,
        ItemStatus status,
        Flexibility flexibility,
        Integer goalPriority,
        Money amount) {

    /** Un gasto ya pagado no se puede recortar: el dinero ya salio. */
    public boolean isStillAvoidable() {
        return status == ItemStatus.PENDING
                || status == ItemStatus.NEEDS_REVIEW
                || status == ItemStatus.OVERDUE;
    }

    public boolean isSaving() {
        return type == CycleItemType.SAVING;
    }

    /**
     * Un gasto que LUMA nunca va a sugerir mover.
     *
     * <p>Es una promesa del producto, no una heuristica: la pantalla de gastos
     * dice literalmente que un gasto critico no se va a sugerir retrasar aunque
     * falte dinero. Si esto cambiara, ese texto se vuelve mentira.
     */
    public boolean isUntouchable() {
        return flexibility == Flexibility.CRITICAL;
    }
}
