package com.luma.expenses.api.dto;

import com.luma.budget.domain.Flexibility;
import com.luma.budget.domain.Frequency;
import com.luma.expenses.domain.ExpenseKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Alta de un gasto.
 *
 * @param kind FIXED o VARIABLE. Un gasto variable se materializa pidiendo
 *     confirmacion del monto en cada ciclo.
 * @param categoryId identificador PUBLICO de la categoria, o nulo. Un gasto sin
 *     categoria es valido: pedirla siempre solo haria que la gente eligiera
 *     "Otros" para salir del paso.
 * @param flexibility cuanto margen hay para mover el pago. No es decorativo:
 *     es lo que impide que el modulo de analisis sugiera retrasar la renta.
 * @param dueDay dia del mes en que vence. Se recorta al ultimo dia en los meses
 *     cortos. Opcional para ANNUAL y ONE_TIME.
 */
public record CreateExpenseRequest(
        @NotBlank(message = "El nombre es obligatorio")
                @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres")
                String name,

        @NotNull(message = "Indica si el monto cambia cada ciclo") ExpenseKind kind,

        @NotBlank(message = "El monto es obligatorio")
                @Pattern(
                        regexp = "^\\d{1,13}(\\.\\d{1,2})?$",
                        message = "El monto debe ser un numero con hasta dos decimales")
                String amount,

        String categoryId,

        @NotNull(message = "Indica que tanto se puede mover este pago")
                Flexibility flexibility,

        @NotNull(message = "La frecuencia es obligatoria") Frequency frequency,

        @Min(value = 1, message = "El dia debe estar entre 1 y 31")
                @Max(value = 31, message = "El dia debe estar entre 1 y 31")
                Integer dueDay,

        @NotNull(message = "La fecha de inicio es obligatoria") LocalDate startDate,

        LocalDate endDate,

        @Size(max = 500, message = "Las notas no pueden pasar de 500 caracteres") String notes) {}
