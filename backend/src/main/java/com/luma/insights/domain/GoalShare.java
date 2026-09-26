package com.luma.insights.domain;

import com.luma.common.model.Money;

/**
 * Cuanto del remanente le tocaria a una meta, si la persona sigue la
 * sugerencia.
 *
 * <p>Es una propuesta, no un movimiento: a diferencia del aporte por ciclo,
 * nada se registra en la meta con solo generarse este registro. La persona
 * decide desde Ahorros, igual que con los recortes de deficit.
 */
public record GoalShare(String goalId, String goalName, Money amount) {}
