package com.luma.expenses.domain;

/**
 * Se capturo un gasto nuevo.
 *
 * <p>Mismo motivo que {@code IncomeCreatedEvent}: el modulo de presupuesto ya
 * conoce al de gastos porque los materializa. Si gastos llamara de vuelta al
 * presupuesto, los dos quedarian apuntandose entre si.
 *
 * <p>Se atiende dentro de la MISMA transaccion: si materializar falla, tampoco
 * se guarda el gasto.
 */
public record ExpenseCreatedEvent(Expense expense) {}
