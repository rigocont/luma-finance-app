package com.luma.users.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Usuario de LUMA.
 *
 * <p>El {@code id} numerico nunca sale por la API: hacia afuera se expone
 * {@link #publicId}, un UUID. Un id autoincremental permite enumerar recursos y
 * deja ver el volumen del negocio.
 *
 * <p>{@code createdAt} y {@code updatedAt} los gobierna la base de datos
 * (DEFAULT CURRENT_TIMESTAMP y ON UPDATE), por eso se mapean como solo lectura.
 */
@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false, unique = true, length = 36)
    private String publicId;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Column(nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private UserStatus status;

    @Column(name = "email_verified_at")
    private Instant emailVerifiedAt;

    @Column(name = "onboarding_completed_at")
    private Instant onboardingCompletedAt;

    @Column(name = "created_at", insertable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", insertable = false, updatable = false)
    private Instant updatedAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    protected User() {
        // Requerido por JPA.
    }

    private User(String email, String name, String passwordHash) {
        this.publicId = UUID.randomUUID().toString();
        this.email = email;
        this.name = name;
        this.passwordHash = passwordHash;
        this.status = UserStatus.ACTIVE;
    }

    /**
     * Crea un usuario nuevo.
     *
     * <p>El correo se normaliza a minusculas: la unicidad no debe depender de
     * como lo escribio la persona.
     *
     * <p>Se crea directamente como {@code ACTIVE}. La verificacion por correo
     * llega en la Fase 2, cuando exista el envio de email.
     */
    public static User register(String email, String name, String passwordHash) {
        return new User(email.trim().toLowerCase(), name.trim(), passwordHash);
    }

    public boolean isActive() {
        return status == UserStatus.ACTIVE && deletedAt == null;
    }

    public boolean hasCompletedOnboarding() {
        return onboardingCompletedAt != null;
    }

    public void completeOnboarding() {
        this.onboardingCompletedAt = Instant.now();
    }

    public void changePassword(String newPasswordHash) {
        this.passwordHash = newPasswordHash;
    }

    public Long getId() {
        return id;
    }

    public String getPublicId() {
        return publicId;
    }

    public String getEmail() {
        return email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public String getName() {
        return name;
    }

    public UserStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getOnboardingCompletedAt() {
        return onboardingCompletedAt;
    }
}
