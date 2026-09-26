package com.luma.savings.domain;

/**
 * Se creo una meta de ahorro.
 *
 * <p>Mismo motivo que en ingresos y gastos: el presupuesto conoce a ahorros
 * porque los materializa, y la dependencia tiene que seguir yendo en ese
 * sentido.
 */
public record SavingsGoalCreatedEvent(SavingsGoal goal) {}
