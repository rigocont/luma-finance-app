package com.luma.auth.infrastructure;

import com.luma.config.LumaProperties;
import com.nimbusds.jose.jwk.source.ImmutableSecret;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;

/**
 * Firma y verificacion de tokens con HMAC-SHA256.
 *
 * <p>Se usa el soporte nativo de Spring Security en lugar de un filtro propio:
 * menos codigo que mantener y menos superficie donde equivocarse en algo
 * delicado.
 *
 * <p>HS256 requiere una clave de al menos 256 bits (32 caracteres). El valor de
 * desarrollo vive en {@code application.yml}; en cualquier otro ambiente llega
 * por {@code LUMA_JWT_SECRET} y nunca se versiona.
 */
@Configuration
public class JwtConfig {

    private static final int MIN_SECRET_LENGTH = 32;

    private SecretKey secretKey(LumaProperties properties) {
        String secret = properties.jwt().secret();
        if (secret == null || secret.length() < MIN_SECRET_LENGTH) {
            throw new IllegalStateException(
                    "luma.jwt.secret debe tener al menos %d caracteres para HS256"
                            .formatted(MIN_SECRET_LENGTH));
        }
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(LumaProperties properties) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(secretKey(properties)));
    }

    @Bean
    JwtDecoder jwtDecoder(LumaProperties properties) {
        return NimbusJwtDecoder.withSecretKey(secretKey(properties))
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
    }
}
