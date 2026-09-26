package com.luma.expenses.api.dto;

import org.springframework.data.domain.Sort;

/**
 * Los ordenamientos que acepta la lista de gastos.
 *
 * <p>Es un enum y no texto libre por la misma razon que en ingresos: un valor
 * desconocido lo rechaza la conversion de Spring con 400, y ningun nombre de
 * columna sale al contrato publico.
 */
public enum ExpenseSort {

    /** Lo capturado mas recientemente primero. Es el orden por omision. */
    NEWEST(Sort.by(Sort.Direction.DESC, "createdAt")),

    NAME(Sort.by(Sort.Direction.ASC, "name")),

    AMOUNT_DESC(Sort.by(Sort.Direction.DESC, "amount")),

    AMOUNT_ASC(Sort.by(Sort.Direction.ASC, "amount")),

    /** Por dia de vencimiento: responde "que me toca pagar primero". */
    DUE_DAY(Sort.by(Sort.Direction.ASC, "dueDay"));

    private final Sort sort;

    ExpenseSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        // Se desempata por id: sin un criterio estable, dos gastos con el mismo
        // monto pueden cambiar de pagina entre dos peticiones.
        return sort.and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
