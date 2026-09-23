package com.luma.auth.infrastructure;

import com.luma.config.LumaProperties;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Construye y lee la cookie del token de renovacion.
 *
 * <p>HttpOnly es lo que hace segura toda la estrategia: el JavaScript de la
 * pagina no puede leer esta cookie, asi que un script inyectado no se lleva la
 * sesion aunque logre ejecutarse.
 *
 * <p>La ruta se limita a los endpoints de autenticacion: la cookie no viaja en
 * cada peticion a la API, solo donde hace falta.
 */
@Component
public class RefreshTokenCookies {

    private final LumaProperties properties;

    public RefreshTokenCookies(LumaProperties properties) {
        this.properties = properties;
    }

    public ResponseCookie create(String rawToken) {
        return build(rawToken, properties.refreshToken().ttl());
    }

    /** Cookie vacia y ya vencida: asi el navegador la borra. */
    public ResponseCookie clear() {
        return build("", Duration.ZERO);
    }

    public Optional<String> read(HttpServletRequest request) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        String name = properties.refreshToken().cookieName();
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(jakarta.servlet.http.Cookie::getValue)
                .filter(value -> value != null && !value.isBlank())
                .findFirst();
    }

    private ResponseCookie build(String value, Duration maxAge) {
        LumaProperties.RefreshToken config = properties.refreshToken();
        return ResponseCookie.from(config.cookieName(), value)
                .httpOnly(true)
                .secure(config.cookieSecure())
                .sameSite(config.cookieSameSite())
                .path(config.cookiePath())
                .maxAge(maxAge)
                .build();
    }
}
