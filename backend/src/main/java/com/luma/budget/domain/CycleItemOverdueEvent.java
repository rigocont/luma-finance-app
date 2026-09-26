package com.luma.budget.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Un renglon acaba de marcarse vencido.
 *
 * <p>Lo publica {@link com.luma.budget.application.OverdueItemsJob} SOLO para
 * los renglones que transicionan hoy, no para los que ya estaban vencidos: esa
 * es la garantia de que notifications genere la alerta una vez, no una vez por
 * dia mientras el pago siga sin ocurrir.
 *
 * <p>Vive en el modulo de presupuesto porque es quien tiene el renglon. El
 * monto viaja sin moneda, igual que en el resto de los eventos y consultas
 * entre modulos: la moneda es una preferencia del usuario y quien la necesite
 * la pide por separado.
 */
public record CycleItemOverdueEvent(
        Long userId, String itemPublicId, String itemName, BigDecimal plannedAmount, LocalDate dueDate) {}
