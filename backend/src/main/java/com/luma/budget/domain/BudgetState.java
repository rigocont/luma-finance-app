package com.luma.budget.domain;

import com.luma.common.model.Money;

/**
 * Resultado del ciclo.
 *
 * <p>La interfaz NUNCA muestra estos nombres. Los traduce a lenguaje normal:
 * "te falta para cerrar", "justo", "te alcanza". Ver docs/design-system.md.
 */
public enum BudgetState {

    /** Las salidas superan las entradas. */
    DEFICIT,

    /** Entradas y salidas coinciden exactamente. */
    BALANCED,

    /** Queda dinero disponible. */
    SURPLUS;

    public static BudgetState of(Money balance) {
        if (balance.isNegative()) {
            return DEFICIT;
        }
        return balance.isZero() ? BALANCED : SURPLUS;
    }
}
