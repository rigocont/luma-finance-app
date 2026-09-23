package com.luma.common.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

/**
 * Un importe con su moneda.
 *
 * <p>Regla no negociable del proyecto: el dinero se representa con
 * {@link BigDecimal}, nunca con {@code double} ni {@code float}. Un test de
 * arquitectura hace cumplir esa regla en todo el codigo.
 *
 * <p>PENDIENTE (Fase 2): el importe debe viajar como cadena ({@code "1234.50"}),
 * porque el cliente es JavaScript y su tipo numerico pierde precision en
 * importes grandes. Spring Boot 4 trae Jackson 3 ({@code tools.jackson}), asi que
 * la anotacion {@code @JsonSerialize} de Jackson 2 se ignora en silencio aunque
 * compile. Se configura con un modulo de Jackson 3 cuando exista el primer
 * endpoint que devuelva dinero, y se verifica con una peticion real.
 *
 * <p>Mientras tanto no hay riesgo: la escala fija de 2 hace que
 * {@link BigDecimal#toString()} nunca use notacion cientifica.
 */
public record Money(BigDecimal amount, String currency) {

    /** Moneda por defecto del producto en v1. */
    public static final String DEFAULT_CURRENCY = "MXN";

    /** Escala y redondeo unicos de todo el sistema. */
    public static final int SCALE = 2;
    public static final RoundingMode ROUNDING = RoundingMode.HALF_UP;

    public Money {
        Objects.requireNonNull(amount, "amount");
        Objects.requireNonNull(currency, "currency");
        amount = amount.setScale(SCALE, ROUNDING);
    }

    public static Money of(BigDecimal amount, String currency) {
        return new Money(amount, currency);
    }

    public static Money of(BigDecimal amount) {
        return new Money(amount, DEFAULT_CURRENCY);
    }

    public static Money of(String amount) {
        return of(new BigDecimal(amount));
    }

    public static Money zero() {
        return of(BigDecimal.ZERO);
    }

    public static Money zero(String currency) {
        return of(BigDecimal.ZERO, currency);
    }

    public Money add(Money other) {
        requireSameCurrency(other);
        return new Money(amount.add(other.amount), currency);
    }

    public Money subtract(Money other) {
        requireSameCurrency(other);
        return new Money(amount.subtract(other.amount), currency);
    }

    public Money negate() {
        return new Money(amount.negate(), currency);
    }

    public boolean isNegative() {
        return amount.signum() < 0;
    }

    public boolean isPositive() {
        return amount.signum() > 0;
    }

    public boolean isZero() {
        return amount.signum() == 0;
    }

    private void requireSameCurrency(Money other) {
        if (!currency.equals(other.currency)) {
            throw new IllegalArgumentException(
                    "No se pueden combinar importes en monedas distintas: %s y %s"
                            .formatted(currency, other.currency));
        }
    }
}
