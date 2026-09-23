package com.luma.common.web;

import java.util.List;
import org.springframework.data.domain.Page;

/**
 * Envoltura de paginacion comun a toda la API.
 *
 * <p>Se expone esta forma en lugar del {@code Page} de Spring Data porque el
 * contrato debe sobrevivir a cambios internos de la libreria y ser predecible
 * para el cliente web y para la aplicacion movil futura.
 */
public record PageResponse<T>(
        List<T> content, int page, int size, long totalElements, int totalPages, boolean last) {

    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }

    public static <T, R> PageResponse<R> from(Page<T> page, java.util.function.Function<T, R> mapper) {
        return new PageResponse<>(
                page.getContent().stream().map(mapper).toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isLast());
    }
}
