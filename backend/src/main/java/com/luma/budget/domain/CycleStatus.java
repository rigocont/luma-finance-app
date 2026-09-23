package com.luma.budget.domain;

/** Ciclo de vida de un ciclo presupuestal. */
public enum CycleStatus {

    /** Recien generado. Los gastos variables esperan revision. */
    DRAFT,

    /** En curso. Es donde la persona opera. */
    ACTIVE,

    /**
     * Terminado e inmutable.
     *
     * <p>Hallazgo 1.2 del analisis: si los ciclos cerrados se pudieran tocar, un
     * aumento de renta hoy cambiaria las cifras de meses pasados.
     */
    CLOSED;

    public boolean isMutable() {
        return this != CLOSED;
    }
}
