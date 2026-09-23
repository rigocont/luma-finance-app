package com.luma.auth.application;

import com.luma.auth.domain.PasswordResetToken;
import com.luma.auth.infrastructure.PasswordResetTokenRepository;
import com.luma.common.error.AuthenticationFailedException;
import com.luma.common.error.BusinessRuleException;
import com.luma.config.LumaProperties;
import com.luma.notifications.application.Mailer;
import com.luma.users.domain.User;
import com.luma.users.infrastructure.UserRepository;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Recuperacion y cambio de contrasena.
 *
 * <p>Dos reglas gobiernan todo este flujo:
 *
 * <p><b>No revelar quien tiene cuenta.</b> Pedir recuperacion responde igual con
 * un correo registrado que con uno inventado. Si respondiera distinto, cualquiera
 * podria averiguar quien usa LUMA probando correos.
 *
 * <p><b>Cambiar la contrasena cierra todas las sesiones.</b> Quien cambia su
 * contrasena normalmente lo hace porque teme que alguien mas tenga acceso.
 * Dejar sesiones abiertas haria inutil el cambio.
 */
@Service
public class PasswordService {

    private static final Logger log = LoggerFactory.getLogger(PasswordService.class);
    private static final int TOKEN_BYTES = 32;

    private final UserRepository users;
    private final PasswordResetTokenRepository resetTokens;
    private final RefreshTokenService refreshTokens;
    private final PasswordEncoder passwordEncoder;
    private final Mailer mailer;
    private final LumaProperties properties;
    private final SecureRandom random = new SecureRandom();

    public PasswordService(
            UserRepository users,
            PasswordResetTokenRepository resetTokens,
            RefreshTokenService refreshTokens,
            PasswordEncoder passwordEncoder,
            Mailer mailer,
            LumaProperties properties) {
        this.users = users;
        this.resetTokens = resetTokens;
        this.refreshTokens = refreshTokens;
        this.passwordEncoder = passwordEncoder;
        this.mailer = mailer;
        this.properties = properties;
    }

    /**
     * Solicita un enlace de recuperacion.
     *
     * <p>Siempre termina sin error, exista o no la cuenta. Quien llama no puede
     * distinguir un caso del otro.
     */
    @Transactional
    public void requestReset(String email) {
        String normalizedEmail = email.trim().toLowerCase();

        users.findByEmailIgnoreCase(normalizedEmail).ifPresentOrElse(
                this::sendResetLinkIfNotThrottled,
                () -> log.info("Solicitud de recuperacion para un correo sin cuenta. Sin accion."));
    }

    private void sendResetLinkIfNotThrottled(User user) {
        if (!user.isActive()) {
            log.info("Solicitud de recuperacion para una cuenta inactiva: {}", user.getPublicId());
            return;
        }

        Instant cooldownStart = Instant.now().minus(properties.passwordReset().resendCooldown());
        if (resetTokens.countRecentUnused(user.getId(), cooldownStart) > 0) {
            // Evita que pedir el enlace muchas veces llene el buzon de la persona
            // y sirva de amplificador para mandar correo a terceros.
            log.info("Solicitud de recuperacion repetida para {}. No se reenvia.", user.getPublicId());
            return;
        }

        String rawToken = generateToken();
        resetTokens.save(PasswordResetToken.issue(
                user.getId(),
                hash(rawToken),
                Instant.now().plus(properties.passwordReset().ttl())));

        String link = "%s/restablecer?token=%s".formatted(
                properties.app().baseUrl(), URLEncoder.encode(rawToken, StandardCharsets.UTF_8));

        if (properties.mail().logLinks()) {
            // Solo en desarrollo: permite avanzar sin abrir la bandeja de correo.
            log.info("Enlace de recuperacion para {}: {}", user.getEmail(), link);
        }

        mailer.send(user.getEmail(), "Recupera tu acceso a LUMA", buildResetEmail(user, link));
    }

    /**
     * Cambia la contrasena usando el token del enlace.
     *
     * <p>El token se marca usado y se cierran todas las sesiones del usuario.
     */
    @Transactional
    public void resetPassword(String rawToken, String newPassword) {
        PasswordResetToken token = resetTokens.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BusinessRuleException(
                        "Este enlace ya no es valido. Solicita uno nuevo."));

        if (!token.isUsable(Instant.now())) {
            throw new BusinessRuleException("Este enlace ya no es valido. Solicita uno nuevo.");
        }

        User user = users.findById(token.getUserId())
                .orElseThrow(() -> new BusinessRuleException(
                        "Este enlace ya no es valido. Solicita uno nuevo."));

        user.changePassword(passwordEncoder.encode(newPassword));
        users.save(user);

        token.markUsed();
        resetTokens.save(token);

        int closedSessions = refreshTokens.revokeAllForUser(user.getId());
        log.info(
                "Contrasena restablecida para {}. Se cerraron {} sesiones.",
                user.getPublicId(),
                closedSessions);
    }

    /**
     * Cambia la contrasena desde dentro de la aplicacion.
     *
     * <p>Exige la contrasena actual: si alguien deja la sesion abierta en una
     * computadora ajena, no debe poder apropiarse de la cuenta.
     */
    @Transactional
    public void changePassword(String publicId, String currentPassword, String newPassword) {
        User user = users.findByPublicId(publicId).orElseThrow(AuthenticationFailedException::new);

        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new AuthenticationFailedException();
        }

        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new BusinessRuleException("La contrasena nueva debe ser distinta de la actual.");
        }

        user.changePassword(passwordEncoder.encode(newPassword));
        users.save(user);

        resetTokens.invalidateAllForUser(user.getId(), Instant.now());
        int closedSessions = refreshTokens.revokeAllForUser(user.getId());

        log.info(
                "Contrasena cambiada por el usuario {}. Se cerraron {} sesiones.",
                user.getPublicId(),
                closedSessions);
    }

    @Transactional
    public int deleteExpiredTokensBefore(Instant before) {
        return resetTokens.deleteExpiredBefore(before);
    }

    private String buildResetEmail(User user, String link) {
        long hours = properties.passwordReset().ttl().toHours();
        String validity = hours >= 1
                ? "%d %s".formatted(hours, hours == 1 ? "hora" : "horas")
                : "%d minutos".formatted(properties.passwordReset().ttl().toMinutes());

        return """
                Hola %s:

                Recibimos una solicitud para recuperar el acceso a tu cuenta de LUMA.

                Abre este enlace para elegir una contrasena nueva:

                %s

                El enlace vence en %s y sirve una sola vez.

                Si no fuiste tu, puedes ignorar este mensaje: tu contrasena sigue
                siendo la misma y nadie ha entrado a tu cuenta.

                --
                LUMA
                """
                .formatted(user.getName(), link, validity);
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
