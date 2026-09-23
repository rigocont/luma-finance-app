package com.luma.income.domain;

/** Naturaleza de un ingreso. */
public enum IncomeType {

    /** Sueldo o cualquier entrada estable. */
    RECURRENT,

    /** Entra con regularidad pero cambia de monto. */
    VARIABLE,

    BONUS,

    /** Aguinaldo. Concepto propio de Mexico, no reducible a BONUS. */
    AGUINALDO,

    SALE,

    OTHER;

    /**
     * El monto configurado es una estimacion, no un dato confiable.
     *
     * <p>Estos ingresos se materializan esperando revision, igual que los gastos
     * variables: un presupuesto que da por seguras unas comisiones que todavia no
     * se conocen promete dinero que puede no llegar, y ese es justo el error que
     * la aplicacion existe para evitar.
     *
     * <p>La regla vive aqui y no repartida en la materializacion para que
     * cambiarla sea tocar un solo lugar.
     */
    public boolean requiresReview() {
        return this == VARIABLE || this == SALE;
    }
}
