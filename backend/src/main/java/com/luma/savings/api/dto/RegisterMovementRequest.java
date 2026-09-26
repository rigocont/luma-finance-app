package com.luma.savings.api.dto;

import com.luma.savings.domain.ContributionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Registra una aportacion o un retiro.
 *
 * @param amount siempre POSITIVO. El que decide el signo es {@code type}, no
 *     quien llama: aceptar negativos abriria la puerta a un retiro disfrazado
 *     de aportacion.
 * @param type EXTRA para una aportacion suelta, WITHDRAWAL para un retiro.
 *     PLANNED no se acepta aqui: esos nacen de confirmar el renglon del ciclo.
 */
public record RegisterMovementRequest(
        @NotBlank(message = "El monto es obligatorio")
                @Pattern(
                        regexp = "^\\d{1,13}(\\.\\d{1,2})?$",
                        message = "El monto debe ser un numero con hasta dos decimales")
                String amount,

        LocalDate date,

        @NotNull(message = "Indica si es una aportacion o un retiro")
                ContributionType type,

        @Size(max = 500, message = "Las notas no pueden pasar de 500 caracteres") String notes) {}
