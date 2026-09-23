package com.luma.common.error;

/**
 * Codigos de error estables de la API.
 *
 * <p>El cliente decide que mostrar segun este codigo, no segun el texto del
 * mensaje: el texto puede traducirse o reescribirse, el codigo no cambia.
 */
public enum ErrorCode {
    VALIDATION_ERROR,
    RESOURCE_NOT_FOUND,
    BUSINESS_RULE_VIOLATION,
    CONFLICT,
    UNAUTHORIZED,
    FORBIDDEN,
    INTERNAL_ERROR
}
