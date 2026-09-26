-- LUMA V3 - Alertas internas (Fase 11).
--
-- Una fila por condicion detectada, no por cada vez que el job programado la
-- vuelve a ver: la unicidad (user_id, type, reference_id) es justo lo que
-- evita mandar el mismo aviso todos los dias mientras el pago siga vencido o
-- el ciclo siga en deficit. Se resuelve sola cuando cambia la referencia: un
-- ciclo cerrado da paso a uno nuevo con otro public_id, y ahi si puede volver
-- a dispararse.

CREATE TABLE notifications (
    id             BIGINT        NOT NULL AUTO_INCREMENT,
    public_id      VARCHAR(36)   NOT NULL,
    user_id        BIGINT        NOT NULL,
    -- PAYMENT_DUE_SOON / PAYMENT_OVERDUE / CYCLE_DEFICIT
    type           VARCHAR(24)   NOT NULL,
    -- public_id del renglon (pagos) o del ciclo (deficit). No es llave foranea
    -- a proposito, igual que source_id en cycle_items: lo que senala puede
    -- cerrarse o cambiar de estado y la alerta debe sobrevivir para consultarse.
    reference_id   VARCHAR(36)   NOT NULL,
    -- Nombre del renglon al momento de generarse. Copia, no referencia: si el
    -- gasto cambia de nombre despues, la alerta sigue contando lo que paso
    -- cuando se genero. NULL en CYCLE_DEFICIT, que no senala un renglon.
    item_name      VARCHAR(120)  NULL,
    -- Monto planeado del renglon, o lo que falta en el ciclo (deficit). Sin
    -- moneda: la pone la capa web con la preferencia del usuario, igual que en
    -- el resto de la API.
    amount         DECIMAL(15,2) NULL,
    -- Solo PAYMENT_*. Se copia para que el cliente no tenga que ir a buscar el
    -- renglon nada mas para mostrar cuando vence.
    due_date       DATE          NULL,
    is_read        BOOLEAN       NOT NULL DEFAULT FALSE,
    read_at        TIMESTAMP     NULL,
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_notifications_public_id (public_id),
    UNIQUE KEY uk_notifications_user_type_reference (user_id, type, reference_id),
    KEY ix_notifications_user_created (user_id, created_at),
    KEY ix_notifications_user_unread (user_id, is_read),
    CONSTRAINT fk_notifications_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_notifications_amount CHECK (amount IS NULL OR amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
