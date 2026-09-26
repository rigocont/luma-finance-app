package com.luma.savings.api.dto;

import jakarta.validation.constraints.NotEmpty;
import java.util.List;

/**
 * El nuevo orden de las metas, de mayor a menor prioridad.
 *
 * <p>Se manda la lista COMPLETA y no "mueve esta al lugar N": asi el resultado
 * no depende de en que estado creia el cliente que estaban las metas. Con dos
 * pestanas abiertas, el ultimo en guardar gana de forma predecible en lugar de
 * dejarlas intercaladas.
 */
public record ReorderGoalsRequest(
        @NotEmpty(message = "Manda el orden completo de tus metas") List<String> goalIds) {}
