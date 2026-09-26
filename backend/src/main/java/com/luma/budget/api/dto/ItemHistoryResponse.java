package com.luma.budget.api.dto;

import com.luma.budget.domain.ItemHistoryEntry;
import com.luma.common.model.Money;
import com.luma.common.web.MoneyDto;
import java.time.LocalDate;

/**
 * Lo que costo un mismo gasto en un ciclo anterior.
 *
 * <p>Viajan los dos montos, el planeado y el real, porque la diferencia entre
 * ambos es justo lo que la persona quiere ver: si un gasto lleva tres ciclos
 * costando mas de lo presupuestado, eso no se ve mirando solo uno.
 */
public record ItemHistoryResponse(
        String cycleId,
        LocalDate cycleStart,
        LocalDate cycleEnd,
        MoneyDto plannedAmount,
        MoneyDto actualAmount,
        LocalDate settledOn) {

    public static ItemHistoryResponse from(ItemHistoryEntry entry, String currency) {
        return new ItemHistoryResponse(
                entry.cyclePublicId(),
                entry.cycleStart(),
                entry.cycleEnd(),
                MoneyDto.from(Money.of(entry.plannedAmount(), currency)),
                MoneyDto.from(Money.of(entry.actualAmount(), currency)),
                entry.settledOn());
    }
}
