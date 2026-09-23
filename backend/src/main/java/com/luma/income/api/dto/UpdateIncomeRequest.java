package com.luma.income.api.dto;

import com.luma.budget.domain.Frequency;
import com.luma.income.domain.IncomeType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Cambios a un ingreso. Todos los campos son opcionales: lo que llega nulo no
 * se toca.
 *
 * <p>Ojo con el calendario: {@code frequency}, {@code expectedDay},
 * {@code startDate} y {@code endDate} se aplican JUNTOS. Cambiar uno solo sin
 * mandar los demas dejaria una combinacion que el usuario no pidio, asi que si
 * se manda cualquiera de los cuatro hay que mandar tambien la frecuencia y la
 * fecha de inicio.
 *
 * <p>Ningun cambio altera los ciclos ya abiertos. Los renglones de un ciclo son
 * copias del momento en que se genero; editar la plantilla aplica desde el
 * siguiente.
 */
public record UpdateIncomeRequest(
        @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres") String name,

        IncomeType type,

        @Pattern(
                        regexp = "^\\d{1,13}(\\.\\d{1,2})?$",
                        message = "El monto debe ser un numero con hasta dos decimales")
                String amount,

        Frequency frequency,

        @Min(value = 1, message = "El dia debe estar entre 1 y 31")
                @Max(value = 31, message = "El dia debe estar entre 1 y 31")
                Integer expectedDay,

        LocalDate startDate,

        LocalDate endDate,

        @Size(max = 500, message = "Las notas no pueden pasar de 500 caracteres") String notes) {

    /** Se pidio cambiar algo del calendario. */
    public boolean touchesSchedule() {
        return frequency != null || expectedDay != null || startDate != null || endDate != null;
    }
}
