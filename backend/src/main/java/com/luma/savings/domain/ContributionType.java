package com.luma.savings.domain;

/** De donde vino un movimiento de la meta. */
public enum ContributionType {

    /** Nacio de confirmar el renglon de ahorro de un ciclo. */
    PLANNED,

    /** Aportacion suelta, fuera del plan del ciclo. */
    EXTRA,

    /**
     * Retiro. Lleva monto NEGATIVO.
     *
     * <p>Modela "saque de la meta para cubrir un deficit", que es algo que pasa
     * y que el historial tiene que poder explicar. Borrar la aportacion en su
     * lugar dejaria un progreso que baja sin motivo visible.
     */
    WITHDRAWAL
}
