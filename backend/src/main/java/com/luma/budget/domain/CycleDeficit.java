package com.luma.budget.domain;

import java.math.BigDecimal;

/**
 * Un ciclo activo cuyo balance PRESUPUESTADO es negativo.
 *
 * <p>{@code missingAmount} ya viene en positivo: es cuanto falta, no el
 * balance con signo. Sacarle el valor absoluto en otra capa seria aritmetica
 * de dinero en el lugar equivocado.
 */
public record CycleDeficit(Long userId, String cyclePublicId, BigDecimal missingAmount) {}
