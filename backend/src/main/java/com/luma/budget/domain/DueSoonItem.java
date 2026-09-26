package com.luma.budget.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un renglon que vence dentro de poco, para quien avisa.
 *
 * <p>Proyeccion de solo lectura entre {@code cycle_items} y {@code
 * budget_cycles}: trae el {@code userId} porque {@link CycleItem} no lo
 * conoce, solo el id de su ciclo, y el modulo de notificaciones lo necesita
 * para saber a quien avisarle sin una segunda consulta.
 */
public record DueSoonItem(
        Long userId, String itemPublicId, String itemName, BigDecimal plannedAmount, LocalDate dueDate) {}
