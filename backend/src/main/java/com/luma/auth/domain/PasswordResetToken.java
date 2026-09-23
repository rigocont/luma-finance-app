package com.luma.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Token de recuperacion de contrasena.
 *
 * <p>Como con los tokens de renovacion, en la base solo vive el hash. El valor
 * en claro existe unicamente dentro del enlace que se envia por correo.
 *
 * <p>Sirve una sola vez y caduca. Ambas cosas son necesarias: un enlace que
 * siguiera funcionando despues de usarse dejaria la cuenta abierta para
 * cualquiera con acceso al buzon.
 */
@Entity
@Table(name = "password_reset_tokens")
public class PasswordResetToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "used_at")
    private Instant usedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected PasswordResetToken() {
        // Requerido por JPA.
    }

    private PasswordResetToken(Long userId, String tokenHash, Instant expiresAt) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
    }

    public static PasswordResetToken issue(Long userId, String tokenHash, Instant expiresAt) {
        return new PasswordResetToken(userId, tokenHash, expiresAt);
    }

    public boolean isUsable(Instant now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed() {
        this.usedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getUsedAt() {
        return usedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
