package com.luma.auth.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * Token de renovacion.
 *
 * <p>NUNCA se guarda el token en claro: solo su hash SHA-256. Si alguien lee la
 * base de datos, no obtiene con que suplantar a nadie.
 *
 * <p>Cada token se usa una sola vez. Al usarlo se emite uno nuevo y este queda
 * revocado, apuntando al que lo reemplazo. Esa cadena es lo que permite detectar
 * un robo: si aparece un token ya revocado, alguien esta usando una copia.
 */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "replaced_by_id")
    private Long replacedById;

    @Column(name = "device_info", length = 255)
    private String deviceInfo;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    protected RefreshToken() {
        // Requerido por JPA.
    }

    private RefreshToken(
            Long userId, String tokenHash, Instant expiresAt, String deviceInfo, String ipAddress) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.deviceInfo = deviceInfo;
        this.ipAddress = ipAddress;
    }

    public static RefreshToken issue(
            Long userId, String tokenHash, Instant expiresAt, String deviceInfo, String ipAddress) {
        return new RefreshToken(userId, tokenHash, expiresAt, deviceInfo, ipAddress);
    }

    public boolean isExpired(Instant now) {
        return expiresAt.isBefore(now);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public boolean isUsable(Instant now) {
        return !isRevoked() && !isExpired(now);
    }

    public void revoke() {
        if (revokedAt == null) {
            this.revokedAt = Instant.now();
        }
    }

    public void replaceWith(RefreshToken replacement) {
        revoke();
        this.replacedById = replacement.getId();
    }

    public Long getId() {
        return id;
    }

    public Long getUserId() {
        return userId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Long getReplacedById() {
        return replacedById;
    }

    public String getDeviceInfo() {
        return deviceInfo;
    }
}
