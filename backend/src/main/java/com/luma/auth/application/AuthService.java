package com.luma.auth.application;

import com.luma.auth.infrastructure.TokenIssuer;
import com.luma.common.error.AuthenticationFailedException;
import com.luma.common.error.BusinessRuleException;
import com.luma.common.error.ResourceNotFoundException;
import com.luma.users.domain.User;
import com.luma.users.infrastructure.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Registro, inicio de sesion y renovacion.
 *
 * <p>La sesion son dos piezas: un access token JWT de vida corta que viaja en la
 * cabecera Authorization, y un token de renovacion de vida larga que viaja en
 * cookie HttpOnly. El primero autoriza cada peticion; el segundo solo sirve para
 * conseguir uno nuevo.
 */
@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final TokenIssuer tokenIssuer;
    private final RefreshTokenService refreshTokens;

    public AuthService(
            UserRepository users,
            PasswordEncoder passwordEncoder,
            TokenIssuer tokenIssuer,
            RefreshTokenService refreshTokens) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.tokenIssuer = tokenIssuer;
        this.refreshTokens = refreshTokens;
    }

    @Transactional
    public Session register(String email, String name, String rawPassword, ClientInfo client) {
        String normalizedEmail = email.trim().toLowerCase();

        if (users.existsByEmailIgnoreCase(normalizedEmail)) {
            throw new BusinessRuleException("Ya existe una cuenta con ese correo.");
        }

        User user = users.save(
                User.register(normalizedEmail, name, passwordEncoder.encode(rawPassword)));

        log.info("Usuario registrado: {}", user.getPublicId());
        return openSession(user, client);
    }

    @Transactional
    public Session login(String email, String rawPassword, ClientInfo client) {
        User user = users.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(AuthenticationFailedException::new);

        // Se comprueba la contrasena aunque la cuenta este inactiva, para que el
        // tiempo de respuesta no delate si el correo existe.
        boolean passwordMatches = passwordEncoder.matches(rawPassword, user.getPasswordHash());

        if (!passwordMatches || !user.isActive()) {
            throw new AuthenticationFailedException();
        }

        return openSession(user, client);
    }

    /**
     * Canjea el token de renovacion por una sesion nueva.
     *
     * <p>El token anterior queda revocado: cada uno sirve una sola vez.
     */
    @Transactional
    public Session refresh(String rawRefreshToken, ClientInfo client) {
        RefreshTokenService.Rotation rotation =
                refreshTokens.rotate(rawRefreshToken, client.deviceInfo(), client.ipAddress());

        User user = users.findById(rotation.userId())
                .orElseThrow(AuthenticationFailedException::new);

        if (!user.isActive()) {
            refreshTokens.revokeAllForUser(user.getId());
            throw new AuthenticationFailedException();
        }

        TokenIssuer.IssuedToken accessToken = tokenIssuer.issue(user);
        return new Session(user, accessToken.value(), accessToken.expiresInSeconds(), rotation.rawToken());
    }

    /**
     * Cierra la sesion de este dispositivo.
     *
     * <p>Es idempotente a proposito: cerrar una sesion que ya no existe no es un
     * error, y responder distinto permitiria averiguar si un token es valido.
     */
    @Transactional
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken != null && !rawRefreshToken.isBlank()) {
            refreshTokens.revoke(rawRefreshToken);
        }
    }

    @Transactional(readOnly = true)
    public User requireByPublicId(String publicId) {
        return users.findByPublicId(publicId)
                .orElseThrow(() -> ResourceNotFoundException.of("Usuario", publicId));
    }

    private Session openSession(User user, ClientInfo client) {
        TokenIssuer.IssuedToken accessToken = tokenIssuer.issue(user);
        String refreshToken =
                refreshTokens.issue(user.getId(), client.deviceInfo(), client.ipAddress());

        return new Session(user, accessToken.value(), accessToken.expiresInSeconds(), refreshToken);
    }

    /** Datos del dispositivo, para poder auditar y revocar sesiones despues. */
    public record ClientInfo(String deviceInfo, String ipAddress) {}

    public record Session(
            User user, String accessToken, long expiresInSeconds, String refreshToken) {}
}
