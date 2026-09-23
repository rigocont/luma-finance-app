package com.luma.budget.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;

/**
 * Confirma lo que de verdad ocurrio en un renglon.
 *
 * @param actualAmount importe real, como cadena. El patron lo valida antes de
 *     convertirlo, asi un texto que no es un numero da 400 con el campo
 *     senalado en lugar de un 500.
 */
public record SettleItemRequest(
        @NotBlank(message = "El monto real es obligatorio")
                @Pattern(
                        regexp = "^\\d{1,13}(\\.\\d{1,2})?$",
                        message = "El monto debe ser un numero con hasta dos decimales")
                String actualAmount,
        LocalDate settledOn) {}
