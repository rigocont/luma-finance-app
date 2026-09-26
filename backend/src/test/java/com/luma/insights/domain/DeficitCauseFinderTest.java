package com.luma.insights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.luma.common.model.Money;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("El buscador de causa de deficit")
class DeficitCauseFinderTest {

    @Test
    @DisplayName("elige la categoria que mas subio")
    void eligeLaQueMasSubio() {
        Map<Long, BigDecimal> anterior = Map.of(1L, new BigDecimal("500.00"), 2L, new BigDecimal("300.00"));
        Map<Long, BigDecimal> actual = Map.of(1L, new BigDecimal("600.00"), 2L, new BigDecimal("900.00"));

        Optional<DeficitCauseFinder.CategoryDelta> resultado =
                DeficitCauseFinder.biggestIncrease(anterior, actual, "MXN");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().categoryId()).isEqualTo(2L);
        assertThat(resultado.get().increase()).isEqualTo(Money.of("600.00"));
    }

    @Test
    @DisplayName("una categoria nueva cuenta como si viniera de cero")
    void unaCategoriaNuevaVieneDeCero() {
        Map<Long, BigDecimal> anterior = Map.of();
        Map<Long, BigDecimal> actual = Map.of(5L, new BigDecimal("200.00"));

        Optional<DeficitCauseFinder.CategoryDelta> resultado =
                DeficitCauseFinder.biggestIncrease(anterior, actual, "MXN");

        assertThat(resultado).isPresent();
        assertThat(resultado.get().previousAmount()).isEqualTo(Money.of("0.00"));
        assertThat(resultado.get().currentAmount()).isEqualTo(Money.of("200.00"));
    }

    @Test
    @DisplayName("sin ninguna categoria al alza, no hay nada que senalar")
    void sinNingunaAlAlzaNoHayNada() {
        Map<Long, BigDecimal> anterior = Map.of(1L, new BigDecimal("500.00"));
        Map<Long, BigDecimal> actual = Map.of(1L, new BigDecimal("300.00"));

        assertThat(DeficitCauseFinder.biggestIncrease(anterior, actual, "MXN")).isEmpty();
    }

    @Test
    @DisplayName("sin categorias en ninguno de los dos ciclos, no hay nada que comparar")
    void sinCategoriasNoHayNada() {
        assertThat(DeficitCauseFinder.biggestIncrease(Map.of(), Map.of(), "MXN")).isEmpty();
    }
}
