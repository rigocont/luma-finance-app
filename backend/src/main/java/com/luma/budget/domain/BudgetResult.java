package com.luma.budget.domain;

import java.math.BigDecimal;

/**
 * Lo que devuelve el motor presupuestal.
 *
 * <p>Trae dos juegos de totales a proposito. {@code planned} responde "como
 * quedo mi presupuesto"; {@code actual} responde "que ha pasado de verdad". Sin
 * esa distincion no hay pagos vencidos, ni historial, ni analisis posible.
 *
 * <p>Este objeto es tambien lo que consumira el modulo de analisis. La IA NUNCA
 * calcula cifras: recibe estas, ya calculadas, y solo las interpreta.
 *
 * @param savingsRate proporcion del ingreso destinada al ahorro, de 0 a 1
 * @param expenseRate proporcion del ingreso destinada a gastos, de 0 a 1
 */
public record BudgetResult(
        BudgetTotals planned,
        BudgetTotals actual,
        BudgetState state,
        BigDecimal savingsRate,
        BigDecimal expenseRate,
        int itemCount,
        int settledCount,
        int pendingCount,
        int overdueCount,
        int needsReviewCount,
        int skippedCount) {

    /** Hay renglones esperando que la persona confirme su monto. */
    public boolean requiresReview() {
        return needsReviewCount > 0;
    }

    public boolean hasOverduePayments() {
        return overdueCount > 0;
    }
}
