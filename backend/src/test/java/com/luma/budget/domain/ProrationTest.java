package com.luma.budget.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.luma.common.model.Money;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("Proration")
class ProrationTest {

    @Test
    void repartirUnMontoDivisibleDaPartesIguales() {
        List<Money> partes = Proration.split(Money.of("10.00"), 4);

        assertThat(partes).hasSize(4);
        assertThat(partes).allSatisfy(parte -> assertThat(parte.amount()).isEqualTo(new BigDecimal("2.50")));
    }

    @Test
    void lasPartesSiempreSumanElTotalExacto() {
        // 1000 entre 3 no da tres veces 333.33: eso sumaria 999.99 y el centavo
        // perdido reaparece como descuadre en el balance.
        List<Money> partes = Proration.split(Money.of("1000.00"), 3);

        assertThat(partes.get(0).amount()).isEqualTo(new BigDecimal("333.34"));
        assertThat(partes.get(1).amount()).isEqualTo(new BigDecimal("333.33"));
        assertThat(partes.get(2).amount()).isEqualTo(new BigDecimal("333.33"));

        assertThat(sumar(partes).amount()).isEqualTo(new BigDecimal("1000.00"));
    }

    @Test
    void losCentavosSobrantesVanEnLasPrimerasPartes() {
        List<Money> partes = Proration.split(Money.of("0.05"), 3);

        assertThat(partes.get(0).amount()).isEqualTo(new BigDecimal("0.02"));
        assertThat(partes.get(1).amount()).isEqualTo(new BigDecimal("0.02"));
        assertThat(partes.get(2).amount()).isEqualTo(new BigDecimal("0.01"));
        assertThat(sumar(partes).amount()).isEqualTo(new BigDecimal("0.05"));
    }

    @ParameterizedTest
    @CsvSource({
        "1000.00, 3",
        "1000.00, 7",
        "12500.00, 11",
        "0.01, 5",
        "999999.99, 13",
        "30000.00, 11",
    })
    void cualquierRepartoSumaElTotal(String total, int partes) {
        Money monto = Money.of(total);

        assertThat(sumar(Proration.split(monto, partes)).amount()).isEqualTo(monto.amount());
    }

    @Test
    void repartirUnMontoNegativoConservaElSigno() {
        List<Money> partes = Proration.split(Money.of("-740.00"), 2);

        assertThat(partes.get(0).amount()).isEqualTo(new BigDecimal("-370.00"));
        assertThat(sumar(partes).amount()).isEqualTo(new BigDecimal("-740.00"));
    }

    @Test
    void repartirEnUnaSolaParteDevuelveElTotal() {
        List<Money> partes = Proration.split(Money.of("1234.56"), 1);

        assertThat(partes).hasSize(1);
        assertThat(partes.get(0).amount()).isEqualTo(new BigDecimal("1234.56"));
    }

    @Test
    void devuelveLaParteQueSePide() {
        assertThat(Proration.share(Money.of("1000.00"), 3, 0).amount())
                .isEqualTo(new BigDecimal("333.34"));
        assertThat(Proration.share(Money.of("1000.00"), 3, 2).amount())
                .isEqualTo(new BigDecimal("333.33"));
    }

    @Test
    void rechazaUnNumeroDePartesInvalido() {
        assertThatThrownBy(() -> Proration.split(Money.of("100.00"), 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Proration.split(Money.of("100.00"), -3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rechazaUnIndiceFueraDeRango() {
        assertThatThrownBy(() -> Proration.share(Money.of("100.00"), 3, 3))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static Money sumar(List<Money> partes) {
        Money total = Money.zero();
        for (Money parte : partes) {
            total = total.add(parte);
        }
        return total;
    }
}
