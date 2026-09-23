package com.luma.budget.api.dto;

import com.luma.common.model.Money;

/**
 * Un importe, tal como sale por la API.
 *
 * <p>El monto viaja como CADENA, no como numero. El tipo numerico de JavaScript
 * pierde precision en importes grandes, y en una aplicacion de dinero eso no es
 * aceptable. La conversion la hace el cliente al mostrar, no el transporte.
 *
 * <p>Se construye a mano y no con una anotacion de serializacion a proposito:
 * Spring Boot 4 usa Jackson 3, donde las anotaciones de Jackson 2 se ignoran EN
 * SILENCIO aunque compilen. Un contrato tan importante no debe depender de que
 * una anotacion siga surtiendo efecto.
 */
public record MoneyDto(String amount, String currency) {

    public static MoneyDto from(Money money) {
        return new MoneyDto(money.amount().toPlainString(), money.currency());
    }

    public static MoneyDto fromNullable(Money money) {
        return money != null ? from(money) : null;
    }
}
