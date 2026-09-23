package com.luma.common.error;

/** Un error de validacion asociado a un campo concreto de la peticion. */
public record FieldError(String field, String message) {}
