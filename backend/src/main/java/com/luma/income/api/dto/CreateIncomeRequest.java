package com.luma.income.api.dto;

import com.luma.budget.domain.Frequency;
import com.luma.income.domain.IncomeType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Alta de un ingreso.
 *
 * @param amount importe como CADENA, no como numero. El patron lo valida antes
 *     de convertirlo, asi un texto que no es un numero da 400 con el campo
 *     senalado en lugar de un 500. Ver docs/api.md.
 * @param expectedDay dia del mes en que se espera. Se recorta al ultimo dia en
 *     los meses cortos: el 31 cae el 30 en abril y el 28 o 29 en febrero.
 *     Opcional para ANNUAL y ONE_TIME, que se resuelven con la fecha de inicio.
 * @param endDate hasta cuando aplica, o nulo si no termina.
 */
public record CreateIncomeRequest(
        @NotBlank(message = "El nombre es obligatorio")
                @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
                String name,

        @NotNull(message = "El tipo de ingreso es obligatorio") IncomeType type,

        @NotBlank(message = "El monto es obligatorio")
                @Pattern(
                        regexp = "^\\d{1,13}(\\.\\d{1,2})?$",
                        message = "El monto debe ser un numero con hasta dos decimales")
                String amount,

        @NotNull(message = "La frecuencia es obligatoria") Frequency frequency,

        @Min(value = 1, message = "El dia debe estar entre 1 y 31")
                @Max(value = 31, message = "El dia debe estar entre 1 y 31")
                Integer expectedDay,

        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate startDate,

        LocalDate endDate,

        @Size(max = 500, message = "Las notas no pueden pasar de 500 caracteres") String notes) {}
