package com.luma.insights.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.luma.common.model.Money;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("El detector de crecimiento por categoria")
class CategoryGrowthDetectorTest {

    private static Money m(String monto) {
        return Money.of(monto);
    }

    @Test
    @DisplayName("tres ciclos subiendo cuentan como sostenido")
    void tresSeguidosAlAlza() {
        assertThat(CategoryGrowthDetector.isSustainedGrowth(List.of(m("100.00"), m("150.00"), m("200.00"))))
                .isTrue();
    }

    @Test
    @DisplayName("un ciclo plano en medio rompe la racha")
    void unPlanoRompeLaRacha() {
        assertThat(CategoryGrowthDetector.isSustainedGrowth(List.of(m("100.00"), m("100.00"), m("200.00"))))
                .isFalse();
    }

    @Test
    @DisplayName("una baja en medio rompe la racha, aunque el total suba")
    void unaBajaRompeLaRachaAunqueElTotalSuba() {
        assertThat(CategoryGrowthDetector.isSustainedGrowth(List.of(m("300.00"), m("100.00"), m("400.00"))))
                .isFalse();
    }

    @Test
    @DisplayName("subir y terminar en cero no cuenta: la categoria dejo de existir")
    void terminarEnCeroNoCuenta() {
        assertThat(CategoryGrowthDetector.isSustainedGrowth(List.of(m("100.00"), m("200.00"), m("0.00"))))
                .isFalse();
    }

    @Test
    @DisplayName("una sola cifra no es una tendencia")
    void unaSolaCifraNoEsTendencia() {
        assertThat(CategoryGrowthDetector.isSustainedGrowth(List.of(m("100.00")))).isFalse();
    }

    @Test
    @DisplayName("subir desde cero cuenta, si cada ciclo es mayor que el anterior")
    void subirDesdeCeroCuenta() {
        assertThat(CategoryGrowthDetector.isSustainedGrowth(List.of(m("0.00"), m("50.00"), m("100.00"))))
                .isTrue();
    }
}
