package com.luma.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuracion propia de LUMA, bajo el prefijo {@code luma}.
 *
 * <p>Todo lo que puede cambiar por ambiente entra aqui en lugar de quedar
 * codificado. Los secretos llegan por variable de entorno, nunca por este archivo.
 */
@ConfigurationProperties(prefix = "luma")
public record LumaProperties(
        Cors cors, Jwt jwt, RefreshToken refreshToken, App app, Mail mail, PasswordReset passwordReset) {

    public record Cors(List<String> allowedOrigins) {}

    /**
     * @param secret clave HMAC. Minimo 32 caracteres para HS256. Fuera de
     *     desarrollo llega por {@code LUMA_JWT_SECRET} y nunca se versiona.
     * @param accessTokenTtl vigencia del access token. Corta a proposito: el
     *     refresh token de la Fase 2 es el que da continuidad a la sesion.
     */
    public record Jwt(String secret, Duration accessTokenTtl) {}

    /**
     * @param ttl vigencia del token de renovacion. Larga: es lo que evita pedir
     *     credenciales todos los dias.
     * @param cookieName nombre de la cookie HttpOnly que lo transporta.
     * @param cookieSecure exige HTTPS. Falso solo en desarrollo local.
     * @param cookieSameSite politica SameSite. "Lax" basta mientras la aplicacion
     *     y la API compartan sitio; si acaban en dominios distintos hay que pasar
     *     a "None", que a su vez obliga a cookieSecure.
     * @param cookiePath ruta donde vive la cookie. Limitada a los endpoints de
     *     autenticacion: no viaja en cada peticion a la API.
     */
    public record RefreshToken(
            Duration ttl,
            String cookieName,
            boolean cookieSecure,
            String cookieSameSite,
            String cookiePath) {}

    /**
     * @param baseUrl direccion publica de la aplicacion web. Es la base de los
     *     enlaces que se envian por correo, asi que un valor incorrecto produce
     *     enlaces que no llevan a ningun lado.
     */
    public record App(String baseUrl) {}

    /**
     * @param from remitente de los correos salientes.
     * @param logLinks escribe los enlaces de recuperacion en el log. SOLO para
     *     desarrollo: permite avanzar sin abrir la bandeja. En produccion un log
     *     con enlaces de recuperacion es una via de acceso a las cuentas.
     */
    public record Mail(String from, boolean logLinks) {}

    /**
     * @param ttl vigencia del enlace de recuperacion. Corta a proposito.
     * @param resendCooldown tiempo minimo entre dos envios al mismo usuario.
     *     Evita llenar el buzon de la persona y que el endpoint sirva de
     *     amplificador para mandar correo a terceros.
     */
    public record PasswordReset(Duration ttl, Duration resendCooldown) {}
}
