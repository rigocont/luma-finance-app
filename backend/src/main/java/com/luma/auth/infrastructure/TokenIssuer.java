package com.luma.auth.infrastructure;

import com.luma.config.LumaProperties;
import com.luma.users.domain.User;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Component;

/**
 * Emite el access token.
 *
 * <p>El {@code sub} es el {@code publicId} del usuario, no el id numerico: el
 * token puede acabar en logs o en el almacenamiento del cliente, y no tiene por
 * que revelar cuantos usuarios hay.
 *
 * <p>El refresh token, su rotacion y la deteccion de reuso llegan en la Fase 2.
 */
@Component
public class TokenIssuer {

    private final JwtEncoder encoder;
    private final LumaProperties properties;

    public TokenIssuer(JwtEncoder encoder, LumaProperties properties) {
        this.encoder = encoder;
        this.properties = properties;
    }

    public IssuedToken issue(User user) {
        Instant now = Instant.now();
        Instant expiresAt = now.plus(properties.jwt().accessTokenTtl());

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("luma")
                .issuedAt(now)
                .expiresAt(expiresAt)
                .subject(user.getPublicId())
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .build();

        String value = encoder
                .encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        return new IssuedToken(value, properties.jwt().accessTokenTtl().toSeconds());
    }

    public record IssuedToken(String value, long expiresInSeconds) {}
}
