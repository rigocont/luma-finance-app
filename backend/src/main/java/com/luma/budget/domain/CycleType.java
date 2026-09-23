package com.luma.budget.domain;

/** Duracion de un ciclo presupuestal. */
public enum CycleType {

    /**
     * Quincenal: del 1 al 15 y del 16 al ultimo dia del mes.
     *
     * <p>No usa dia de anclaje. Es la forma en que se pagan los sueldos
     * quincenales, y permitir anclajes arbitrarios multiplicaria los casos borde
     * sin resolver ningun problema real.
     */
    BIWEEKLY,

    /** Mensual, desde el dia de anclaje hasta el dia anterior al del mes siguiente. */
    MONTHLY,

    /**
     * Bimestral, desde el dia de anclaje de un mes impar hasta el dia anterior
     * al del mes impar siguiente.
     */
    BIMONTHLY
}
