package com.luma.expenses.api.dto;

import com.luma.expenses.domain.ExpenseCategory;

/**
 * Una categoria del catalogo.
 *
 * @param system la creo el sistema y la ve todo el mundo. La interfaz puede
 *     distinguirlas de las propias sin tener que deducirlo.
 * @param defaultKind sugerencia de clasificacion al elegir esta categoria:
 *     "Despensa" propone gasto variable, "Renta" propone fijo. Es una ayuda al
 *     capturar, no una regla.
 */
public record ExpenseCategoryResponse(
        String id,
        String code,
        String name,
        String icon,
        String color,
        String defaultKind,
        boolean system) {

    public static ExpenseCategoryResponse from(ExpenseCategory category) {
        return new ExpenseCategoryResponse(
                category.getPublicId(),
                category.getCode(),
                category.getName(),
                category.getIcon(),
                category.getColor(),
                category.getDefaultKind().name(),
                category.isSystem());
    }
}
