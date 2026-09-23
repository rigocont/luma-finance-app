package com.luma.savings.domain;

/** Como se decide cuanto aportar a una meta en cada ciclo. */
public enum ContributionMode {

    /**
     * Lo calcula el sistema: lo que falta, repartido entre los ciclos que quedan
     * hasta la fecha objetivo.
     *
     * <p>Es lo que responde la pregunta "cuanto necesito ahorrar para llegar".
     */
    AUTO_BY_TARGET_DATE,

    /** Un monto fijo por ciclo que decide la persona. */
    FIXED_PER_CYCLE,

    /**
     * Sin plan: la persona aporta cuando quiere.
     *
     * <p>No entra en la formula del presupuesto, porque no hay nada
     * comprometido que restar.
     */
    MANUAL;

    public boolean affectsBudget() {
        return this != MANUAL;
    }
}
