package com.luma.budget.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("El promedio del historial")
class ItemHistoryEntryTest {

    private static ItemHistoryEntry de(String real) {
        return new ItemHistoryEntry(
                1L,
                "ciclo",
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2026, 1, 15),
                new BigDecimal("800.00"),
                new BigDecimal(real),
                LocalDate.of(2026, 1, 15));
    }

    @Test
    @DisplayName("sin historia viene vacio")
    void sinHistoria() {
        assertThat(ItemHistoryEntry.averageActual(List.of())).isEmpty();
    }

    @Test
    @DisplayName("promedia lo confirmado")
    void promedia() {
        assertThat(ItemHistoryEntry.averageActual(List.of(de("600.00"), de("900.00"))))
                .contains(new BigDecimal("750.00"));
    }

    @Test
    @DisplayName("siempre con dos decimales")
    void dosDecimales() {
        // 1000 / 3 no cabe en dos decimales. El resultado se redondea igual que
        // cualquier otro importe del producto, no se trunca ni arrastra
        // decimales que luego no cuadran al sumarse.
        assertThat(ItemHistoryEntry.averageActual(
                        List.of(de("333.33"), de("333.33"), de("333.34"))))
                .contains(new BigDecimal("333.33"));
    }

    @Test
    @DisplayName("redondea hacia arriba en el medio exacto")
    void mediaExacta() {
        assertThat(ItemHistoryEntry.averageActual(List.of(de("100.00"), de("101.01"))))
                .contains(new BigDecimal("100.51"));
    }
}
