-- LUMA V2 - Nucleo presupuestal.
--
-- Crea el esquema completo del que dependen las fases 4 a 10. Las mismas
-- convenciones de V1: public_id UUID hacia afuera, enums como VARCHAR,
-- importes DECIMAL(15,2), timestamps en UTC y fechas de calendario como DATE.
--
-- Orden de creacion dictado por las llaves foraneas:
--   expense_categories -> incomes -> expenses -> savings_goals
--   -> budget_cycles -> cycle_items -> savings_contributions

-- ---------------------------------------------------------------------------
-- Catalogo de categorias
-- ---------------------------------------------------------------------------
-- Las del sistema llevan user_id NULL y se siembran mas abajo. Las que crea
-- una persona apuntan a su usuario.
--
-- NOTA: en MySQL dos filas con user_id NULL no chocan en una llave unica, asi
-- que la unicidad del codigo entre categorias del sistema la garantiza la
-- semilla, no la base. Es aceptable porque la semilla es la unica que las crea.
CREATE TABLE expense_categories (
    id           BIGINT      NOT NULL AUTO_INCREMENT,
    public_id    VARCHAR(36) NOT NULL,
    user_id      BIGINT      NULL,
    code         VARCHAR(64) NOT NULL,
    name         VARCHAR(80) NOT NULL,
    icon         VARCHAR(40) NULL,
    color        VARCHAR(9)  NULL,
    -- Sugerencia de clasificacion al crear un gasto con esta categoria.
    default_kind VARCHAR(16) NOT NULL DEFAULT 'FIXED',
    is_system    BOOLEAN     NOT NULL DEFAULT FALSE,
    display_order INT        NOT NULL DEFAULT 0,
    created_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP   NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_expense_categories_public_id (public_id),
    UNIQUE KEY uk_expense_categories_code_user (code, user_id),
    KEY ix_expense_categories_user (user_id),
    CONSTRAINT fk_expense_categories_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Ingresos
-- ---------------------------------------------------------------------------
CREATE TABLE incomes (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    public_id    VARCHAR(36)   NOT NULL,
    user_id      BIGINT        NOT NULL,
    name         VARCHAR(120)  NOT NULL,
    income_type  VARCHAR(24)   NOT NULL DEFAULT 'RECURRENT',
    amount       DECIMAL(15,2) NOT NULL,
    frequency    VARCHAR(16)   NOT NULL DEFAULT 'BIWEEKLY',
    -- Dia del mes en que se espera. NULL para ANNUAL y ONE_TIME, que se
    -- resuelven con start_date.
    expected_day INT           NULL,
    start_date   DATE          NOT NULL,
    end_date     DATE          NULL,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    notes        VARCHAR(500)  NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_incomes_public_id (public_id),
    KEY ix_incomes_user_active (user_id, active),
    CONSTRAINT fk_incomes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_incomes_amount CHECK (amount >= 0),
    CONSTRAINT ck_incomes_expected_day CHECK (expected_day IS NULL OR (expected_day BETWEEN 1 AND 31))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Gastos (fijos y variables en una sola tabla)
-- ---------------------------------------------------------------------------
-- Ver docs/00-arquitectura-fase-0.md seccion 1.3: separar fijos de variables en
-- dos mecanismos distintos duplicaba el calculo sin beneficio. La distincion se
-- conserva como clasificacion en expense_kind.
CREATE TABLE expenses (
    id           BIGINT        NOT NULL AUTO_INCREMENT,
    public_id    VARCHAR(36)   NOT NULL,
    user_id      BIGINT        NOT NULL,
    category_id  BIGINT        NULL,
    name         VARCHAR(120)  NOT NULL,
    expense_kind VARCHAR(16)   NOT NULL DEFAULT 'FIXED',
    amount       DECIMAL(15,2) NOT NULL,
    frequency    VARCHAR(16)   NOT NULL DEFAULT 'MONTHLY',
    due_day      INT           NULL,
    -- CRITICAL / IMPORTANT / FLEXIBLE. Es lo que permite que el modulo de
    -- analisis NUNCA sugiera retrasar un pago que no se puede retrasar.
    flexibility  VARCHAR(16)   NOT NULL DEFAULT 'IMPORTANT',
    start_date   DATE          NOT NULL,
    end_date     DATE          NULL,
    active       BOOLEAN       NOT NULL DEFAULT TRUE,
    notes        VARCHAR(500)  NULL,
    created_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at   TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at   TIMESTAMP     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_expenses_public_id (public_id),
    KEY ix_expenses_user_kind_active (user_id, expense_kind, active),
    KEY ix_expenses_category (category_id),
    CONSTRAINT fk_expenses_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_expenses_category FOREIGN KEY (category_id) REFERENCES expense_categories (id) ON DELETE SET NULL,
    CONSTRAINT ck_expenses_amount CHECK (amount >= 0),
    CONSTRAINT ck_expenses_due_day CHECK (due_day IS NULL OR (due_day BETWEEN 1 AND 31))
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Metas de ahorro
-- ---------------------------------------------------------------------------
-- contribution_mode y planned_per_cycle resuelven el hallazgo 1.4 del analisis:
-- la formula del presupuesto resta "ahorros", pero sin un aporte por ciclo el
-- motor no tenia nada que restar.
CREATE TABLE savings_goals (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    public_id         VARCHAR(36)   NOT NULL,
    user_id           BIGINT        NOT NULL,
    name              VARCHAR(120)  NOT NULL,
    target_amount     DECIMAL(15,2) NOT NULL,
    -- Denormalizado: se mantiene al registrar cada aportacion, en la misma
    -- transaccion, para no recalcular la suma en cada lectura del dashboard.
    current_amount    DECIMAL(15,2) NOT NULL DEFAULT 0.00,
    target_date       DATE          NULL,
    -- AUTO_BY_TARGET_DATE: el aporte se deriva de lo que falta y los ciclos
    -- restantes. FIXED_PER_CYCLE: lo fija la persona. MANUAL: no entra en el
    -- calculo del presupuesto.
    contribution_mode VARCHAR(24)   NOT NULL DEFAULT 'AUTO_BY_TARGET_DATE',
    planned_per_cycle DECIMAL(15,2) NULL,
    -- Orden para repartir un remanente y para sugerir recortes ante un deficit.
    priority          INT           NOT NULL DEFAULT 0,
    status            VARCHAR(16)   NOT NULL DEFAULT 'ACTIVE',
    icon              VARCHAR(40)   NULL,
    color             VARCHAR(9)    NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at        TIMESTAMP     NULL,
    PRIMARY KEY (id),
    UNIQUE KEY uk_savings_goals_public_id (public_id),
    KEY ix_savings_goals_user_status (user_id, status),
    CONSTRAINT fk_savings_goals_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_savings_goals_target CHECK (target_amount > 0),
    CONSTRAINT ck_savings_goals_planned CHECK (planned_per_cycle IS NULL OR planned_per_cycle >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Ciclos presupuestales
-- ---------------------------------------------------------------------------
CREATE TABLE budget_cycles (
    id              BIGINT      NOT NULL AUTO_INCREMENT,
    public_id       VARCHAR(36) NOT NULL,
    user_id         BIGINT      NOT NULL,
    cycle_type      VARCHAR(16) NOT NULL,
    start_date      DATE        NOT NULL,
    end_date        DATE        NOT NULL,
    -- DRAFT -> ACTIVE -> CLOSED. Un ciclo cerrado es inmutable.
    status          VARCHAR(16) NOT NULL DEFAULT 'DRAFT',
    sequence_number INT         NOT NULL,
    closed_at       TIMESTAMP   NULL,
    created_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_budget_cycles_public_id (public_id),
    UNIQUE KEY uk_budget_cycles_user_start (user_id, start_date),
    KEY ix_budget_cycles_user_status (user_id, status),
    CONSTRAINT fk_budget_cycles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_budget_cycles_range CHECK (end_date >= start_date)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Renglones del ciclo  (la entidad central del producto)
-- ---------------------------------------------------------------------------
-- Hallazgos 1.1 y 1.2 del analisis. Al abrir un ciclo se materializa aqui una
-- copia de cada ingreso, gasto y aporte que aplica. Los campos name, category
-- y flexibility se COPIAN a proposito: si manana sube la renta, los ciclos ya
-- cerrados no deben cambiar.
--
-- planned_amount es lo presupuestado; actual_amount es lo que de verdad paso.
-- Sin esa distincion no hay pagos vencidos, ni historial, ni analisis posible.
CREATE TABLE cycle_items (
    id              BIGINT        NOT NULL AUTO_INCREMENT,
    public_id       VARCHAR(36)   NOT NULL,
    budget_cycle_id BIGINT        NOT NULL,
    -- INCOME / FIXED_EXPENSE / VARIABLE_EXPENSE / SAVING
    item_type       VARCHAR(24)   NOT NULL,
    -- Origen del renglon. No es llave foranea a proposito: la plantilla puede
    -- borrarse y el historial debe sobrevivir.
    source_type     VARCHAR(24)   NOT NULL,
    source_id       BIGINT        NULL,
    name            VARCHAR(120)  NOT NULL,
    category_id     BIGINT        NULL,
    planned_amount  DECIMAL(15,2) NOT NULL,
    actual_amount   DECIMAL(15,2) NULL,
    due_date        DATE          NULL,
    -- NEEDS_REVIEW / PENDING / PAID / PARTIAL / SKIPPED / OVERDUE
    status          VARCHAR(16)   NOT NULL DEFAULT 'PENDING',
    -- Dos fechas distintas a proposito: settled_on es el dia en que la persona
    -- dice que ocurrio (fecha de calendario, sin zona); settled_at es cuando lo
    -- registro en el sistema (auditoria).
    settled_on      DATE          NULL,
    settled_at      TIMESTAMP     NULL,
    flexibility     VARCHAR(16)   NULL,
    display_order   INT           NOT NULL DEFAULT 0,
    notes           VARCHAR(500)  NULL,
    created_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_cycle_items_public_id (public_id),
    KEY ix_cycle_items_cycle_type (budget_cycle_id, item_type),
    KEY ix_cycle_items_cycle_status_due (budget_cycle_id, status, due_date),
    KEY ix_cycle_items_source (source_type, source_id),
    CONSTRAINT fk_cycle_items_cycle FOREIGN KEY (budget_cycle_id) REFERENCES budget_cycles (id) ON DELETE CASCADE,
    CONSTRAINT fk_cycle_items_category FOREIGN KEY (category_id) REFERENCES expense_categories (id) ON DELETE SET NULL,
    CONSTRAINT ck_cycle_items_planned CHECK (planned_amount >= 0),
    CONSTRAINT ck_cycle_items_actual CHECK (actual_amount IS NULL OR actual_amount >= 0)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Aportaciones a metas
-- ---------------------------------------------------------------------------
CREATE TABLE savings_contributions (
    id                BIGINT        NOT NULL AUTO_INCREMENT,
    public_id         VARCHAR(36)   NOT NULL,
    savings_goal_id   BIGINT        NOT NULL,
    -- Enlaza la aportacion con el renglon del ciclo que la origino, cuando
    -- viene de un plan. NULL si fue una aportacion extra o un retiro.
    cycle_item_id     BIGINT        NULL,
    amount            DECIMAL(15,2) NOT NULL,
    contribution_date DATE          NOT NULL,
    -- PLANNED / EXTRA / WITHDRAWAL. El retiro lleva monto negativo: es lo que
    -- permite modelar "saque de la meta para cubrir un deficit".
    type              VARCHAR(16)   NOT NULL DEFAULT 'PLANNED',
    notes             VARCHAR(500)  NULL,
    created_at        TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uk_savings_contributions_public_id (public_id),
    KEY ix_savings_contributions_goal_date (savings_goal_id, contribution_date),
    CONSTRAINT fk_savings_contributions_goal FOREIGN KEY (savings_goal_id) REFERENCES savings_goals (id) ON DELETE CASCADE,
    CONSTRAINT fk_savings_contributions_cycle_item FOREIGN KEY (cycle_item_id) REFERENCES cycle_items (id) ON DELETE SET NULL
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;

-- ---------------------------------------------------------------------------
-- Semilla del catalogo de categorias del sistema
-- ---------------------------------------------------------------------------
-- Son las sugerencias que ve la persona en el onboarding (Fase 9).
INSERT INTO expense_categories (public_id, user_id, code, name, icon, color, default_kind, is_system, display_order) VALUES
    (UUID(), NULL, 'HOUSING',       'Vivienda',          'home',        '#C98A2B', 'FIXED',    TRUE,  10),
    (UUID(), NULL, 'UTILITIES',     'Servicios',         'bolt',        '#C98A2B', 'FIXED',    TRUE,  20),
    (UUID(), NULL, 'INTERNET',      'Internet',          'wifi',        '#C98A2B', 'FIXED',    TRUE,  30),
    (UUID(), NULL, 'PHONE',         'Telefono',          'phone',       '#C98A2B', 'FIXED',    TRUE,  40),
    (UUID(), NULL, 'TRANSPORT',     'Transporte',        'commute',     '#C98A2B', 'FIXED',    TRUE,  50),
    (UUID(), NULL, 'FUEL',          'Gasolina',          'local_gas',   '#C98A2B', 'FIXED',    TRUE,  60),
    (UUID(), NULL, 'VEHICLE',       'Automovil',         'car',         '#C98A2B', 'FIXED',    TRUE,  70),
    (UUID(), NULL, 'INSURANCE',     'Seguros',           'shield',      '#C98A2B', 'FIXED',    TRUE,  80),
    (UUID(), NULL, 'GROCERIES',     'Despensa',          'cart',        '#C98A2B', 'VARIABLE', TRUE,  90),
    (UUID(), NULL, 'SUBSCRIPTIONS', 'Suscripciones',     'subs',        '#C98A2B', 'FIXED',    TRUE, 100),
    (UUID(), NULL, 'CREDIT_CARD',   'Tarjetas de credito','credit_card','#B3402F', 'VARIABLE', TRUE, 110),
    (UUID(), NULL, 'DEBT',          'Deudas',            'account',     '#B3402F', 'VARIABLE', TRUE, 120),
    (UUID(), NULL, 'HEALTH',        'Salud',             'health',      '#C98A2B', 'VARIABLE', TRUE, 130),
    (UUID(), NULL, 'EDUCATION',     'Educacion',         'school',      '#C98A2B', 'VARIABLE', TRUE, 140),
    (UUID(), NULL, 'MAINTENANCE',   'Mantenimiento',     'build',       '#C98A2B', 'VARIABLE', TRUE, 150),
    (UUID(), NULL, 'TAXES',         'Impuestos',         'receipt',     '#C98A2B', 'VARIABLE', TRUE, 160),
    (UUID(), NULL, 'OTHER',         'Otros',             'more',        '#6F6862', 'VARIABLE', TRUE, 999);
