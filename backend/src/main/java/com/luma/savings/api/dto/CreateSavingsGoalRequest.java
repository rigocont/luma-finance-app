package com.luma.savings.api.dto;

import com.luma.savings.domain.ContributionMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Alta de una meta de ahorro.
 *
 * @param mode como se decide cuanto apartar por ciclo.
 *     AUTO_BY_TARGET_DATE lo calcula a partir de la fecha objetivo;
 *     FIXED_PER_CYCLE usa {@code plannedPerCycle}; MANUAL no entra en el
 *     presupuesto.
 * @param targetDate obligatoria con AUTO_BY_TARGET_DATE: sin fecha no hay
 *     ciclos entre los cuales repartir lo que falta.
 */
public record CreateSavingsGoalRequest(
        @NotBlank(message = "El nombre es obligatorio")
                @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
                String name,

        @NotBlank(message = "La meta es obligatoria")
                @Pattern(
                        regexp = "^\\d{1,13}(\\.\\d{1,2})?$",
                        message = "La meta debe ser un numero con hasta dos decimales")
                String target,

        LocalDate targetDate,

        @NotNull(message = "Indica como vas a decidir cuanto apartar")
                ContributionMode mode,

        @Pattern(
                        regexp = "^\\d{1,13}(\\.\\d{1,2})?$",
                        message = "El aporte debe ser un numero con hasta dos decimales")
                String plannedPerCycle,

        @Size(max = 40) String icon,

        @Size(max = 9) String color) {}
