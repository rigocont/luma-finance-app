package com.luma.budget.domain;

/** Estado de ejecucion de un renglon del ciclo. */
public enum ItemStatus {

    /** Gasto variable recien materializado: la persona tiene que confirmar el monto. */
    NEEDS_REVIEW,

    /** Confirmado y esperando su fecha. */
    PENDING,

    /** Ocurrio por completo. */
    PAID,

    /** Ocurrio en parte. */
    PARTIAL,

    /**
     * La persona lo saco de este ciclo.
     *
     * <p>No entra en ningun total: es la diferencia entre "no lo he pagado" y
     * "este ciclo no aplica".
     */
    SKIPPED,

    /** Paso su fecha sin ocurrir. */
    OVERDUE;

    /** Los renglones omitidos no cuentan en el presupuesto. */
    public boolean countsTowardBudget() {
        return this != SKIPPED;
    }

    public boolean isSettled() {
        return this == PAID || this == PARTIAL;
    }
}
