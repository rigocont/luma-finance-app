-- LUMA V1 - Base de usuarios y autenticacion.
--
-- Convenciones del esquema (ver docs/00-arquitectura-fase-0.md):
--   * PK interna BIGINT autoincremental; `public_id` (UUID) es lo unico que sale por la API.
--   * Los enums se guardan como VARCHAR, no como ENUM de MySQL: agregar un valor
--     no requiere ALTER TABLE y las migraciones quedan reversibles.
--   * Timestamps en UTC. Las fechas de calendario usan DATE (sin zona horaria).
--   * Importes: DECIMAL(15,2). Nunca DOUBLE ni FLOAT.

CREATE TABLE users (
    id                      BIGINT       NOT NULL AUTO_INCREMENT,
    public_id               VARCHAR(36)  NOT NULL,
    email                   VARCHAR(255) NOT NULL,
    password_hash           VARCHAR(255) NOT NULL,
    name                    VARCHAR(120) NOT NULL,
    status                  VARCHAR(32)  NOT NULL DEFAULT 'PENDING_VERIFICATION',
    email_verified_at       TIMESTAMP    NULL,
    onboarding_completed_at TIMESTAMP    NULL,
    created_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at              TIMESTAMP    NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_users_public_id (public_id),
    UNIQUE KEY uk_users_email (email),
    KEY ix_users_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE user_preferences (
    id                        BIGINT      NOT NULL AUTO_INCREMENT,
    user_id                   BIGINT      NOT NULL,
    currency                  VARCHAR(3)  NOT NULL DEFAULT 'MXN',
    locale                    VARCHAR(10) NOT NULL DEFAULT 'es-MX',
    timezone                  VARCHAR(64) NOT NULL DEFAULT 'America/Mexico_City',
    budget_cycle_type         VARCHAR(16) NOT NULL DEFAULT 'BIWEEKLY',
    cycle_anchor_day          INT         NOT NULL DEFAULT 1,
    -- BY_DUE_DATE: el gasto carga completo en el ciclo donde vence (default del producto).
    -- PRORATE: el gasto se reparte entre los ciclos del periodo natural.
    expense_allocation_policy VARCHAR(16) NOT NULL DEFAULT 'BY_DUE_DATE',
    theme                     VARCHAR(10) NOT NULL DEFAULT 'SYSTEM',
    created_at                TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at                TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_preferences_user (user_id),
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE refresh_tokens (
    id             BIGINT       NOT NULL AUTO_INCREMENT,
    user_id        BIGINT       NOT NULL,
    -- Se guarda el hash SHA-256, nunca el token en claro.
    token_hash     VARCHAR(64)  NOT NULL,
    expires_at     TIMESTAMP    NOT NULL,
    revoked_at     TIMESTAMP    NULL,
    -- Rotacion: apunta al token que lo reemplazo. Permite detectar reuso.
    replaced_by_id BIGINT       NULL,
    device_info    VARCHAR(255) NULL,
    ip_address     VARCHAR(45)  NULL,
    created_at     TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_refresh_tokens_hash (token_hash),
    KEY ix_refresh_tokens_user (user_id),
    KEY ix_refresh_tokens_expires (expires_at),
    CONSTRAINT fk_refresh_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_tokens_replaced_by FOREIGN KEY (replaced_by_id) REFERENCES refresh_tokens (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

CREATE TABLE password_reset_tokens (
    id         BIGINT    NOT NULL AUTO_INCREMENT,
    user_id    BIGINT    NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at    TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_password_reset_tokens_hash (token_hash),
    KEY ix_password_reset_tokens_user (user_id),
    CONSTRAINT fk_password_reset_tokens_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
