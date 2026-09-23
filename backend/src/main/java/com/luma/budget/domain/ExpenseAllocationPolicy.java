package com.luma.budget.domain;

/**
 * Como carga un gasto cuyo periodo natural es mas largo que el ciclo.
 *
 * <p>Es la decision de negocio numero uno del producto. Con ciclo quincenal y
 * una renta mensual de 8,000 que vence el dia 1:
 *
 * <ul>
 *   <li>{@code BY_DUE_DATE}: la primera quincena carga 8,000 y la segunda 0.
 *   <li>{@code PRORATE}: cada quincena carga 4,000.
 * </ul>
 *
 * <p>El producto usa {@code BY_DUE_DATE} por omision porque refleja el flujo de
 * caja real: es como la gente razona ("esta quincena me toca pagar la renta").
 * Prorratear suaviza el balance, pero esconde deficits de caja que si existen.
 */
public enum ExpenseAllocationPolicy {

    /** El gasto carga completo en el ciclo donde vence. */
    BY_DUE_DATE,

    /**
     * El gasto se reparte entre los ciclos que cubren su periodo natural.
     *
     * <p>Todavia no implementado en la materializacion: ver
     * {@code CycleMaterializer}. La columna existe y la preferencia se puede
     * guardar, pero abrir un ciclo con esta politica falla de forma explicita en
     * lugar de caer en silencio a la otra.
     */
    PRORATE
}
