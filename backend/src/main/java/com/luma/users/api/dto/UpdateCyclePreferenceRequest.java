package com.luma.users.api.dto;

import com.luma.budget.domain.CycleType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * El tipo de ciclo y el dia en que empieza.
 *
 * @param anchorDay el dia del mes en que arranca el primer periodo. En los meses
 *     cortos, un 31 cae el ultimo dia: el motor ya lo resuelve, aqui solo se
 *     valida el rango.
 */
public record UpdateCyclePreferenceRequest(
        @NotNull(message = "Indica cada cuanto quieres presupuestar") CycleType cycleType,

        @Min(value = 1, message = "El dia debe estar entre 1 y 31")
                @Max(value = 31, message = "El dia debe estar entre 1 y 31")
                int anchorDay) {}
