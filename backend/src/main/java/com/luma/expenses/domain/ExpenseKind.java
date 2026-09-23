package com.luma.expenses.domain;

/** Clasificacion de un gasto. */
public enum ExpenseKind {

    /** Monto estable ciclo con ciclo. Se materializa listo. */
    FIXED,

    /**
     * Cambia de monto en cada ciclo.
     *
     * <p>Al materializarse queda en NEEDS_REVIEW: la persona tiene que confirmar
     * cuanto es esta vez antes de que el presupuesto sea real.
     */
    VARIABLE
}
