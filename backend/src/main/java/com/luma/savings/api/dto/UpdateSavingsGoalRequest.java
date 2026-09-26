package com.luma.savings.api.dto;

import com.luma.savings.domain.ContributionMode;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

/**
 * Cambios a una meta. Lo que llega nulo no se toca.
 *
 * @param clearTargetDate distingue "no mande fecha" de "quitale la fecha". Sin
 *     esta bandera las dos cosas llegan como nulo y significan lo contrario.
 */
public record UpdateSavingsGoalRequest(
        @Size(max = 120, message = "El nombre no puede pasar de 120 caracteres") String name,

        @Pattern(
                        regexp = "^\\d{1,13}(\\.\\d{1,2})?$",
                        message = "La meta debe ser un numero con hasta dos decimales")
                String target,

        LocalDate targetDate,

        boolean clearTargetDate,

        ContributionMode mode,

        @Pattern(
                        regexp = "^\\d{1,13}(\\.\\d{1,2})?$",
                        message = "El aporte debe ser un numero con hasta dos decimales")
                String plannedPerCycle,

        @Size(max = 40) String icon,

        @Size(max = 9) String color) {

    public boolean touchesTargetDate() {
        return clearTargetDate || targetDate != null;
    }
}
