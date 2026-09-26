package com.luma.insights.domain;

import com.luma.common.model.Money;

/**
 * Por que un ciclo no alcanza, cuando hay algo concreto que senalar.
 *
 * <p>No siempre existe: si no hay un ciclo anterior con que comparar, o si
 * ninguna categoria crecio frente a el, este registro simplemente no se
 * genera. Es preferible no explicar nada a inventar una causa que las cifras
 * no respaldan.
 *
 * @param cycleId el ciclo en deficit.
 * @param missing lo que falta, en positivo -el mismo numero que ya muestra
 *     DeficitAdviceCard, para que ambas tarjetas cuenten la misma historia.
 * @param categoryName la categoria que mas subio respecto al ciclo anterior.
 * @param previousAmount lo que costo esa categoria en el ciclo anterior.
 * @param currentAmount lo que cuesta en este.
 */
public record DeficitCause(
        String cycleId, Money missing, String categoryName, Money previousAmount, Money currentAmount) {

    /** Cuanto subio. Siempre positiva: es la razon por la que se eligio esta categoria. */
    public Money increase() {
        return currentAmount.subtract(previousAmount);
    }
}
