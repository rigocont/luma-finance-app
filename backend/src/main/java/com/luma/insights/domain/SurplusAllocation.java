package com.luma.insights.domain;

import com.luma.common.model.Money;
import java.util.List;

/**
 * Que hacer con lo que sobra, cuando el ciclo cierra con remanente.
 *
 * <p>Simetrico a DeficitAdvisor.Advice pero en la direccion contraria: en vez
 * de proponer de donde recortar, propone a donde mandar lo que ya alcanza y de
 * sobra.
 *
 * @param cycleId el ciclo con remanente.
 * @param surplus lo que sobra, siempre positivo.
 * @param shares el reparto propuesto, en el orden en que se llenaron las
 *     metas -de la de mayor prioridad a la de menor-. Vacio si no hay ninguna
 *     meta activa a la que le falte algo.
 */
public record SurplusAllocation(String cycleId, Money surplus, List<GoalShare> shares) {}
