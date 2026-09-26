package com.luma.insights.domain;

import java.util.List;

/**
 * Lo que el analisis financiero tiene que decir hoy, para un usuario.
 *
 * <p>Los tres campos son independientes y todos pueden faltar: un ciclo en
 * equilibrio no tiene ni causa de deficit ni reparto de remanente, y una
 * cuenta con poco historial no tiene crecimiento sostenido que reportar. Nunca
 * es un error -es exactamente lo que hay que mostrar cuando no hay nada que
 * senalar.
 *
 * @param deficitCause nulo si el ciclo en curso no esta en deficit, o si no
 *     hay con que explicarlo.
 * @param surplusAllocation nulo si el ciclo en curso no tiene remanente, o si
 *     no hay ninguna meta activa a la que asignarselo.
 * @param categoryGrowth las categorias con crecimiento sostenido. Vacia, no
 *     nula, cuando no hay ninguna -es una lista, no una condicion binaria.
 */
public record FinancialInsights(
        DeficitCause deficitCause, SurplusAllocation surplusAllocation, List<CategoryGrowth> categoryGrowth) {}
