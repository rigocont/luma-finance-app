package com.luma.income.api.dto;

import org.springframework.data.domain.Sort;

/**
 * Los ordenamientos que acepta la lista de ingresos.
 *
 * <p>Es un enum y no un parametro de texto libre a proposito. Un {@code sort}
 * abierto acepta cualquier nombre de campo, y uno que no existe revienta la
 * consulta con un 500: el cliente podria tumbar el endpoint con un dato
 * invalido. Con un enum, un valor desconocido lo rechaza la propia conversion
 * de Spring y sale un 400 con el campo senalado, sin codigo extra.
 *
 * <p>De paso, ningun nombre de columna sale al contrato publico: renombrar un
 * campo de la entidad no rompe a ningun cliente.
 */
public enum IncomeSort {

    /** Lo capturado mas recientemente primero. Es el orden por omision. */
    NEWEST(Sort.by(Sort.Direction.DESC, "createdAt")),

    NAME(Sort.by(Sort.Direction.ASC, "name")),

    AMOUNT_DESC(Sort.by(Sort.Direction.DESC, "amount")),

    AMOUNT_ASC(Sort.by(Sort.Direction.ASC, "amount")),

    START_DATE(Sort.by(Sort.Direction.DESC, "startDate"));

    private final Sort sort;

    IncomeSort(Sort sort) {
        this.sort = sort;
    }

    public Sort toSort() {
        // Se desempata siempre por id: sin un criterio estable, dos ingresos con
        // el mismo monto pueden cambiar de pagina entre dos peticiones y la
        // paginacion repetiria o se saltaria registros.
        return sort.and(Sort.by(Sort.Direction.ASC, "id"));
    }
}
