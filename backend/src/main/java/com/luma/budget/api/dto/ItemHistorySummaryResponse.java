package com.luma.budget.api.dto;

import com.luma.budget.domain.ItemHistoryEntry;
import com.luma.common.model.Money;
import com.luma.common.web.MoneyDto;
import java.util.List;

/**
 * El historial de un gasto, con su promedio ya calculado.
 *
 * <p>El promedio viene del servidor y no se saca en la interfaz: es una cifra de
 * dinero, y en LUMA ninguna se deriva en el cliente. Que hoy solo sirva para
 * leer la lista de un vistazo no cambia la regla — asi no hay dos formas de
 * redondear el mismo numero.
 *
 * @param average nulo cuando no hay historia. La interfaz simplemente no lo
 *     muestra.
 */
public record ItemHistorySummaryResponse(
        int cycles, MoneyDto average, List<ItemHistoryResponse> entries) {

    public static ItemHistorySummaryResponse from(
            List<ItemHistoryEntry> history, String currency) {

        return new ItemHistorySummaryResponse(
                history.size(),
                ItemHistoryEntry.averageActual(history)
                        .map(amount -> MoneyDto.from(Money.of(amount, currency)))
                        .orElse(null),
                history.stream().map(entry -> ItemHistoryResponse.from(entry, currency)).toList());
    }
}
