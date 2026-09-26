package com.luma.expenses.api.dto;

import com.luma.budget.domain.Flexibility;
import com.luma.budget.domain.Frequency;
import com.luma.expenses.domain.ExpenseKind;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Cambios a un gasto. Lo que llega nulo no se toca.
 *
 * <p>El calendario se aplica COMPLETO: {@code frequency}, {@code dueDay},
 * {@code startDate} y {@code endDate} son una sola decision. Si mandas
 * cualquiera de los cuatro, manda tambien la frecuencia y la fecha de inicio.
 *
 * <p>Ningun cambio altera los ciclos ya abiertos: sus renglones son copias del
 * momento en que se generaron.
 *
 * @param clearCategory distingue "no mande categoria" de "quiero quitarle la
 *     categoria". Sin esta bandera las dos cosas llegan como nulo y significan
 *     lo contrario.
 */
public record UpdateExpenseRequest(
        @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres") String name,

        ExpenseKind kind,

        @Pattern(
                        regexp = "^\\d{1,13}(\\.\\d{1,2})?$",
                        message = "El monto debe ser un numero con hasta dos decimales")
                String amount,

        String categoryId,

        boolean clearCategory,

        Flexibility flexibility,

        Frequency frequency,

        @Min(value = 1, message = "El dia debe estar entre 1 y 31")
                @Max(value = 31, message = "El dia debe estar entre 1 y 31")
                Integer dueDay,

        LocalDate startDate,

        LocalDate endDate,

        @Size(max = 500, message = "Las notas no pueden pasar de 500 caracteres") String notes) {

    /** Se pidio cambiar la categoria, ya sea a otra o a ninguna. */
    public boolean touchesCategory() {
        return clearCategory || categoryId != null;
    }

    /** Se pidio cambiar algo del calendario. */
    public boolean touchesSchedule() {
        return frequency != null || dueDay != null || startDate != null || endDate != null;
    }
}
