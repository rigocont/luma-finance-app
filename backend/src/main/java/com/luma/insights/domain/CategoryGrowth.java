package com.luma.insights.domain;

import com.luma.common.model.Money;

/**
 * Una categoria de gasto que crecio de forma sostenida.
 *
 * <p>No es una alerta ni algo que haya que resolver: es una observacion. El
 * gasto puede seguir siendo correcto, la persona solo no lo habia notado
 * todavia porque paso poco a poco, un ciclo a la vez.
 *
 * @param categoryName el nombre, resuelto una sola vez por el servicio -este
 *     registro no conoce el catalogo de categorias.
 * @param firstAmount lo que costo la categoria en el primero de los ciclos
 *     comparados.
 * @param lastAmount lo que costo en el mas reciente.
 * @param cycles cuantos ciclos seguidos crecio. Hoy siempre el mismo numero
 *     -la ventana que usa el detector- pero viaja explicito para que la
 *     interfaz no tenga que adivinarlo.
 */
public record CategoryGrowth(String categoryName, Money firstAmount, Money lastAmount, int cycles) {}
