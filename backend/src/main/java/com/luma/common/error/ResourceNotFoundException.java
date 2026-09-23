package com.luma.common.error;

/** El recurso solicitado no existe, o no pertenece al usuario autenticado. */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public static ResourceNotFoundException of(String resource, Object id) {
        return new ResourceNotFoundException("%s no encontrado: %s".formatted(resource, id));
    }
}
