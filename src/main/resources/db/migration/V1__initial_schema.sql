-- ============================================================
--  QueueSmart – Initial Schema
--  Migration : V1__initial_schema.sql
--  Database  : MySQL 8+
-- ============================================================

-- ── 1. user_credentials ──────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_credentials (
    id         VARCHAR(36)  NOT NULL,
    email      VARCHAR(100) NOT NULL,
    password   VARCHAR(255) NOT NULL,        -- BCrypt hash; plain-text NEVER stored
    role       ENUM('USER','ADMIN') NOT NULL DEFAULT 'USER',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_credentials_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 2. user_profile ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS user_profile (
    id             VARCHAR(36)  NOT NULL,
    credential_id  VARCHAR(36)  NOT NULL,
    username       VARCHAR(50)  NOT NULL,
    full_name      VARCHAR(150),
    phone          VARCHAR(30),
    preferences    TEXT,
    email_verified TINYINT(1)   NOT NULL DEFAULT 0,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_profile_credential (credential_id),
    UNIQUE KEY uq_profile_username   (username),
    CONSTRAINT fk_profile_credential
        FOREIGN KEY (credential_id) REFERENCES user_credentials(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 3. service ───────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS service (
    id                        VARCHAR(36)  NOT NULL,
    name                      VARCHAR(100) NOT NULL,
    description               VARCHAR(500) NOT NULL,
    expected_duration_minutes INT          NOT NULL,
    priority_level            ENUM('LOW','MEDIUM','HIGH') NOT NULL DEFAULT 'MEDIUM',
    active                    TINYINT(1)   NOT NULL DEFAULT 1,
    created_by_admin_id       VARCHAR(36),
    created_at                DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_service_name (name),
    CONSTRAINT chk_service_duration CHECK (expected_duration_minutes BETWEEN 1 AND 480),
    CONSTRAINT fk_service_admin
        FOREIGN KEY (created_by_admin_id) REFERENCES user_credentials(id)
        ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 4. queue ─────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS queue (
    id         VARCHAR(36) NOT NULL,
    service_id VARCHAR(36) NOT NULL,
    status     ENUM('OPEN','CLOSED') NOT NULL DEFAULT 'OPEN',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE KEY uq_queue_service (service_id),
    CONSTRAINT fk_queue_service
        FOREIGN KEY (service_id) REFERENCES service(id)
        ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 5. queue_entry ───────────────────────────────────────────
CREATE TABLE IF NOT EXISTS queue_entry (
    id                     VARCHAR(36) NOT NULL,
    queue_id               VARCHAR(36) NOT NULL,
    user_id                VARCHAR(36) NOT NULL,
    position               INT         NOT NULL,
    joined_at              DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status                 ENUM('WAITING','SERVING','SERVED','LEFT') NOT NULL DEFAULT 'WAITING',
    priority_level         ENUM('LOW','MEDIUM','HIGH')               NOT NULL DEFAULT 'MEDIUM',
    estimated_wait_minutes INT,
    PRIMARY KEY (id),
    CONSTRAINT fk_entry_queue
        FOREIGN KEY (queue_id) REFERENCES queue(id) ON DELETE CASCADE,
    CONSTRAINT fk_entry_user
        FOREIGN KEY (user_id)  REFERENCES user_credentials(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 6. notification ──────────────────────────────────────────
CREATE TABLE IF NOT EXISTS notification (
    id         VARCHAR(36)  NOT NULL,
    user_id    VARCHAR(36)  NOT NULL,
    message    VARCHAR(500) NOT NULL,
    type       ENUM('QUEUE_JOINED','ALMOST_YOUR_TURN','YOUR_TURN','QUEUE_LEFT','QUEUE_STATUS_CHANGED')
               NOT NULL,
    is_read    TINYINT(1)   NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT fk_notif_user
        FOREIGN KEY (user_id) REFERENCES user_credentials(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── 7. history_record ────────────────────────────────────────
CREATE TABLE IF NOT EXISTS history_record (
    id             VARCHAR(36)  NOT NULL,
    user_id        VARCHAR(36)  NOT NULL,
    service_id     VARCHAR(36),
    service_name   VARCHAR(100) NOT NULL,
    joined_at      DATETIME     NOT NULL,
    completed_at   DATETIME,
    final_status   ENUM('WAITING','SERVING','SERVED','LEFT') NOT NULL,
    waited_minutes INT          NOT NULL DEFAULT 0,
    PRIMARY KEY (id),
    CONSTRAINT fk_history_user
        FOREIGN KEY (user_id) REFERENCES user_credentials(id),
    CONSTRAINT fk_history_service
        FOREIGN KEY (service_id) REFERENCES service(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Indexes ──────────────────────────────────────────────────
CREATE INDEX idx_entry_queue    ON queue_entry   (queue_id);
CREATE INDEX idx_entry_user     ON queue_entry   (user_id);
CREATE INDEX idx_entry_status   ON queue_entry   (status);
CREATE INDEX idx_notif_user     ON notification  (user_id);
CREATE INDEX idx_notif_read     ON notification  (user_id, is_read);
CREATE INDEX idx_history_user   ON history_record(user_id);
CREATE INDEX idx_history_service ON history_record(service_id);
