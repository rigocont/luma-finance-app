package com.luma.budget.domain;

/**
 * Cada cuanto se repite un ingreso o un gasto.
 *
 * <p>Es independiente del tipo de ciclo: un gasto mensual puede caer en un
 * presupuesto quincenal, y uno quincenal en un presupuesto bimestral. Resolver
 * ese cruce es el trabajo de {@link RecurrenceSchedule}.
 */
public enum Frequency {

    /** Dos veces por mes: el dia indicado y ese mismo dia mas 15, ambos recortados. */
    BIWEEKLY,

    /** Una vez por mes, el dia indicado. */
    MONTHLY,

    /** Una vez cada dos meses, el dia indicado, alineado con el mes de inicio. */
    BIMONTHLY,

    /** Una vez al ano, en el mismo dia y mes que la fecha de inicio. */
    ANNUAL,

    /** Exactamente una vez, en la fecha de inicio. */
    ONE_TIME
}
