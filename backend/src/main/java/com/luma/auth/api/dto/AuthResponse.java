package com.luma.auth.api.dto;

import com.luma.auth.application.AuthService;

/**
 * Respuesta de una sesion abierta.
 *
 * <p>El token de renovacion NO aparece aqui en el caso normal: viaja en cookie
 * HttpOnly, fuera del alcance del JavaScript de la pagina. Solo se incluye en el
 * cuerpo cuando el cliente se identifica como movil, porque una aplicacion
 * nativa no tiene donde recibir una cookie.
 */
public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user,
        String refreshToken) {

    public static AuthResponse webSession(AuthService.Session session) {
        return new AuthResponse(
                session.accessToken(),
                "Bearer",
                session.expiresInSeconds(),
                UserResponse.from(session.user()),
                null);
    }

    public static AuthResponse mobileSession(AuthService.Session session) {
        return new AuthResponse(
                session.accessToken(),
                "Bearer",
                session.expiresInSeconds(),
                UserResponse.from(session.user()),
                session.refreshToken());
    }
}
