package com.luma.common.error;

/**
 * Una regla de negocio impidio la operacion.
 *
 * <p>Ejemplos: cerrar un ciclo que ya esta cerrado, abrir un segundo ciclo activo,
 * registrar una aportacion mayor que lo que falta para la meta.
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
