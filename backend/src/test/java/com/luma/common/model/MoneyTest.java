package com.luma.common.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class MoneyTest {

    @Test
    void normalizaLaEscalaADosDecimales() {
        assertThat(Money.of("10").amount()).isEqualTo(new BigDecimal("10.00"));
        assertThat(Money.of("10.5").amount()).isEqualTo(new BigDecimal("10.50"));
    }

    @Test
    void redondeaConHalfUp() {
        assertThat(Money.of("10.005").amount()).isEqualTo(new BigDecimal("10.01"));
        assertThat(Money.of("10.004").amount()).isEqualTo(new BigDecimal("10.00"));
    }

    @Test
    void sumaYResta() {
        Money ingresos = Money.of("12500.00");
        Money fijos = Money.of("7800.00");
        Money variables = Money.of("2100.00");
        Money ahorro = Money.of("1500.00");

        Money balance = ingresos.subtract(fijos).subtract(variables).subtract(ahorro);

        assertThat(balance.amount()).isEqualTo(new BigDecimal("1100.00"));
        assertThat(balance.isPositive()).isTrue();
    }

    @Test
    void detectaDeficit() {
        Money balance = Money.of("12500.00").subtract(Money.of("13240.00"));

        assertThat(balance.amount()).isEqualTo(new BigDecimal("-740.00"));
        assertThat(balance.isNegative()).isTrue();
    }

    @Test
    void rechazaMezclarMonedas() {
        assertThatThrownBy(() -> Money.of(BigDecimal.ONE, "MXN").add(Money.of(BigDecimal.ONE, "USD")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("monedas distintas");
    }
}
