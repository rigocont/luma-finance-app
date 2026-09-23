package com.luma.budget.api.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Ajusta un renglon del ciclo. Todos los campos son opcionales: se modifica solo
 * lo que llega.
 *
 * <p>Cambiar el monto de un gasto variable es justo lo que lo saca de revision.
 */
public record UpdateItemRequest(
        @Pattern(
                        regexp = "^\\d{1,13}(\\.\\d{1,2})?$",
                        message = "El monto debe ser un numero con hasta dos decimales")
                String plannedAmount,
        Integer displayOrder,
        @Size(max = 500, message = "Las notas no pueden pasar de 500 caracteres") String notes) {}
