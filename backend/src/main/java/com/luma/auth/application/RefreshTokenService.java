package com.luma.auth.application;

import com.luma.auth.domain.RefreshToken;
import com.luma.auth.infrastructure.RefreshTokenRepository;
import com.luma.common.error.AuthenticationFailedException;
import com.luma.config.LumaProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emision, rotacion y revocacion de tokens de renovacion.
 *
 * <p>El token es opaco y aleatorio, no un JWT: no necesita llevar informacion,
 * solo ser imposible de adivinar. Se generan 256 bits con {@link SecureRandom}.
 */
@Service
public class RefreshTokenService {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenService.class);
    private static final int TOKEN_BYTES = 32;

    /**
     * Margen para carreras legitimas.
     *
     * <p>Dos pestanas abiertas renuevan casi a la vez y la segunda llega con el
     * token que la primera acaba de rotar. Sin margen, eso se leeria como un robo
     * y cerraria la sesion del usuario sin motivo.
     *
     * <p>Dentro de esta ventana la peticion se rechaza igual, pero no se revoca la
     * familia. Un replay real reaparece mucho despues; el riesgo que se acepta es
     * estrecho y el costo de no aceptarlo son cierres de sesion constantes.
     */
    private static final Duration REUSE_GRACE = Duration.ofSeconds(30);

    private final RefreshTokenRepository tokens;
    private final LumaProperties properties;
    private final SecureRandom random = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository tokens, LumaProperties properties) {
        this.tokens = tokens;
        this.properties = properties;
    }

    @Transactional
    public String issue(Long userId, String deviceInfo, String ipAddress) {
        String rawToken = generateToken();
        Instant expiresAt = Instant.now().plus(properties.refreshToken().ttl());

        tokens.save(RefreshToken.issue(userId, hash(rawToken), expiresAt, deviceInfo, ipAddress));
        return rawToken;
    }

    /**
     * Canjea un token por uno nuevo.
     *
     * <p>Si llega un token que ya fue usado, se asume que alguien tiene una copia
     * robada y se revoca la sesion completa del usuario. Es agresivo a proposito:
     * pedirle a la persona que vuelva a entrar es mucho menos grave que dejar una
     * sesion en manos ajenas.
     */
    @Transactional
    public Rotation rotate(String rawToken, String deviceInfo, String ipAddress) {
        RefreshToken current = tokens.findByTokenHash(hash(rawToken))
                .orElseThrow(AuthenticationFailedException::new);

        Instant now = Instant.now();

        if (current.isRevoked()) {
            if (isLikelyRace(current, now)) {
                log.info(
                        "Token de renovacion ya rotado, presentado dentro del margen "
                                + "de carrera para el usuario {}. No se revoca la sesion.",
                        current.getUserId());
                throw new AuthenticationFailedException();
            }

            int revoked = tokens.revokeAllActiveForUser(current.getUserId(), now);
            log.warn(
                    "Reuso de token de renovacion detectado para el usuario {}. "
                            + "Se revocaron {} tokens activos.",
                    current.getUserId(),
                    revoked);
            throw new AuthenticationFailedException();
        }

        if (current.isExpired(now)) {
            throw new AuthenticationFailedException();
        }

        String newRawToken = generateToken();
        RefreshToken replacement = tokens.save(RefreshToken.issue(
                current.getUserId(),
                hash(newRawToken),
                now.plus(properties.refreshToken().ttl()),
                deviceInfo,
                ipAddress));

        current.replaceWith(replacement);
        tokens.save(current);

        return new Rotation(current.getUserId(), newRawToken);
    }

    /** Revoca un token concreto. Cerrar sesion en un dispositivo no toca los demas. */
    @Transactional
    public void revoke(String rawToken) {
        tokens.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            token.revoke();
            tokens.save(token);
        });
    }

    @Transactional
    public int revokeAllForUser(Long userId) {
        return tokens.revokeAllActiveForUser(userId, Instant.now());
    }

    @Transactional
    public int deleteExpiredBefore(Instant before) {
        return tokens.deleteExpiredBefore(before);
    }

    /**
     * Distingue una carrera de un robo: el token fue rotado (no revocado por un
     * cierre de sesion) y hace muy poco.
     */
    private boolean isLikelyRace(RefreshToken token, Instant now) {
        return token.getReplacedById() != null
                && token.getRevokedAt() != null
                && token.getRevokedAt().isAfter(now.minus(REUSE_GRACE));
    }

    private String generateToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /**
     * SHA-256 sin sal ni estiramiento, a diferencia de las contrasenas.
     *
     * <p>Aqui es lo correcto: el token tiene 256 bits de entropia aleatoria, asi
     * que no hay diccionario que probar. BCrypt solo haria lento cada refresco.
     */
    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }

    public record Rotation(Long userId, String rawToken) {}
}
