package com.luma.budget.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * Fija de una vez el monto de varios renglones en revision.
 *
 * <p>No marca pagos. Confirmar el monto de un gasto variable es decir CUANTO es
 * este ciclo; que ya se haya pagado es otro hecho y tiene su propia accion.
 *
 * @param items hasta 200 renglones. El tope no es para el servidor sino para el
 *     cliente: un lote mas grande que eso no sale de una pantalla de revision,
 *     sale de un error o de alguien probando limites.
 */
public record ConfirmAmountsRequest(
        @NotEmpty(message = "Manda al menos un renglon por confirmar")
                @Size(max = 200, message = "No se pueden confirmar mas de 200 renglones a la vez")
                List<@Valid Entry> items) {

    public record Entry(
            @NotBlank(message = "Falta el identificador del renglon") String itemId,

            @NotBlank(message = "El monto es obligatorio")
                    @Pattern(
                            regexp = "^\\d{1,13}(\\.\\d{1,2})?$",
                            message = "El monto debe ser un numero con hasta dos decimales")
                    String amount) {}
}
