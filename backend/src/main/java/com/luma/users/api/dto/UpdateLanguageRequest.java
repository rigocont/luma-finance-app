package com.luma.users.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * El idioma de la interfaz.
 *
 * @param uiLanguage "es" o "en". Cualquier otro valor no tiene catalogo de
 *     traducciones, asi que se rechaza aqui y no hasta el dominio.
 */
public record UpdateLanguageRequest(
        @NotBlank(message = "Indica el idioma")
                @Pattern(regexp = "es|en", message = "El idioma debe ser 'es' o 'en'")
                String uiLanguage) {}
