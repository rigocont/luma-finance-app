package com.luma.auth.application;

import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Borra los tokens vencidos hace mas de una semana.
 *
 * <p>Sin esto las tablas crecen sin limite: cada refresco deja una fila revocada
 * y cada recuperacion una usada. Se conserva una semana de margen por si hace
 * falta revisar una cadena de rotacion al investigar un incidente.
 */
@Component
public class RefreshTokenCleanupJob {

    private static final Logger log = LoggerFactory.getLogger(RefreshTokenCleanupJob.class);
    private static final Duration RETENTION = Duration.ofDays(7);

    private final RefreshTokenService refreshTokens;
    private final PasswordService passwords;

    public RefreshTokenCleanupJob(RefreshTokenService refreshTokens, PasswordService passwords) {
        this.refreshTokens = refreshTokens;
        this.passwords = passwords;
    }

    @Scheduled(cron = "0 30 3 * * *")
    public void deleteExpiredTokens() {
        Instant cutoff = Instant.now().minus(RETENTION);

        int refreshDeleted = refreshTokens.deleteExpiredBefore(cutoff);
        int resetDeleted = passwords.deleteExpiredTokensBefore(cutoff);

        if (refreshDeleted > 0 || resetDeleted > 0) {
            log.info(
                    "Limpieza de tokens: {} de renovacion y {} de recuperacion eliminados",
                    refreshDeleted,
                    resetDeleted);
        }
    }
}
