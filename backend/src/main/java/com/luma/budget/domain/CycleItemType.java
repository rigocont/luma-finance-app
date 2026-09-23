package com.luma.budget.domain;

/** Que representa un renglon del ciclo dentro de la formula del presupuesto. */
public enum CycleItemType {

    /** Entra. */
    INCOME,

    /** Sale, y se repite con un monto estable. */
    FIXED_EXPENSE,

    /** Sale, y cambia de monto en cada ciclo. */
    VARIABLE_EXPENSE,

    /** Sale hacia una meta de ahorro. Es una salida planificada, no un sobrante. */
    SAVING;

    public boolean isOutflow() {
        return this != INCOME;
    }
}
