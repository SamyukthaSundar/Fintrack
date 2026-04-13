-- ============================================================
-- FinTrack - Collaborative Expense Engine
-- MySQL Database Schema (DDL)
-- UE23CS352B OOAD Mini Project
-- ============================================================



-- ============================================================
-- TABLE: users
-- Owner: Saanvi Kakkar (Identity & Optimization Lead)
-- ============================================================
CREATE TABLE IF NOT EXISTS users (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    username   VARCHAR(50)  NOT NULL UNIQUE,
    email      VARCHAR(100) NOT NULL UNIQUE,
    password   VARCHAR(255) NOT NULL,
    full_name  VARCHAR(100) NOT NULL,
    role       ENUM('USER', 'ADMIN') NOT NULL DEFAULT 'USER',
    avatar_url VARCHAR(255),
    is_active  BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_users_email (email),
    INDEX idx_users_username (username)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: groups_table
-- Owner: Saanvi Kakkar
-- ============================================================
CREATE TABLE IF NOT EXISTS groups_table (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(500),
    created_by  BIGINT NOT NULL,
    currency    VARCHAR(10) NOT NULL DEFAULT 'INR',
    is_active   BOOLEAN NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_group_creator FOREIGN KEY (created_by) REFERENCES users(id) ON DELETE RESTRICT,
    INDEX idx_group_creator (created_by)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: group_members
-- Owner: Saanvi Kakkar
-- ============================================================
CREATE TABLE IF NOT EXISTS group_members (
    id        BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id  BIGINT NOT NULL,
    user_id   BIGINT NOT NULL,
    role      ENUM('ADMIN', 'MEMBER') NOT NULL DEFAULT 'MEMBER',
    joined_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_gm_group FOREIGN KEY (group_id) REFERENCES groups_table(id) ON DELETE CASCADE,
    CONSTRAINT fk_gm_user  FOREIGN KEY (user_id)  REFERENCES users(id)         ON DELETE CASCADE,
    UNIQUE KEY uq_group_member (group_id, user_id),
    INDEX idx_gm_user (user_id)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: expenses
-- Owner: Samyuktha S (Automation & Strategy Lead)
-- ============================================================
CREATE TABLE IF NOT EXISTS expenses (
    id             BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id       BIGINT NOT NULL,
    paid_by        BIGINT NOT NULL,
    title          VARCHAR(200) NOT NULL,
    description    VARCHAR(1000),
    total_amount   DECIMAL(12, 2) NOT NULL,
    currency       VARCHAR(10) NOT NULL DEFAULT 'INR',
    split_type     ENUM('EQUAL', 'PERCENTAGE', 'EXACT', 'WEIGHTED') NOT NULL DEFAULT 'EQUAL',
    category       ENUM('FOOD', 'TRANSPORT', 'ACCOMMODATION', 'ENTERTAINMENT', 'UTILITIES', 'SHOPPING', 'HEALTHCARE', 'OTHER') NOT NULL DEFAULT 'OTHER',
    receipt_image  VARCHAR(500),
    ocr_raw_text   TEXT,
    expense_date   DATE NOT NULL,
    is_settled     BOOLEAN NOT NULL DEFAULT FALSE,
    created_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_expense_group   FOREIGN KEY (group_id) REFERENCES groups_table(id) ON DELETE CASCADE,
    CONSTRAINT fk_expense_paidby  FOREIGN KEY (paid_by)  REFERENCES users(id)        ON DELETE RESTRICT,
    INDEX idx_expense_group (group_id),
    INDEX idx_expense_paidby (paid_by),
    INDEX idx_expense_date (expense_date)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: expense_splits
-- Owner: Samyuktha S
-- ============================================================
CREATE TABLE IF NOT EXISTS expense_splits (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    expense_id BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    amount     DECIMAL(12, 2) NOT NULL,
    percentage DECIMAL(5, 2),
    weight     DECIMAL(5, 2),
    is_paid    BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_split_expense FOREIGN KEY (expense_id) REFERENCES expenses(id)  ON DELETE CASCADE,
    CONSTRAINT fk_split_user    FOREIGN KEY (user_id)    REFERENCES users(id)     ON DELETE RESTRICT,
    UNIQUE KEY uq_expense_user_split (expense_id, user_id),
    INDEX idx_split_user (user_id)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: settlements
-- Owner: Sanika Gupta (Settlement & Analytics Lead)
-- ============================================================
CREATE TABLE IF NOT EXISTS settlements (
    id              BIGINT AUTO_INCREMENT PRIMARY KEY,
    group_id        BIGINT NOT NULL,
    payer_id        BIGINT NOT NULL,
    payee_id        BIGINT NOT NULL,
    amount          DECIMAL(12, 2) NOT NULL,
    currency        VARCHAR(10) NOT NULL DEFAULT 'INR',
    status          ENUM('PENDING', 'SUBMITTED', 'VERIFIED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    payment_method  ENUM('CASH', 'UPI', 'BANK_TRANSFER', 'OTHER') DEFAULT 'CASH',
    payment_ref     VARCHAR(200),
    notes           VARCHAR(500),
    initiated_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at    TIMESTAMP,
    verified_at     TIMESTAMP,
    rejected_at     TIMESTAMP,
    verified_by     BIGINT,
    CONSTRAINT fk_settle_group   FOREIGN KEY (group_id)    REFERENCES groups_table(id) ON DELETE CASCADE,
    CONSTRAINT fk_settle_payer   FOREIGN KEY (payer_id)    REFERENCES users(id)        ON DELETE RESTRICT,
    CONSTRAINT fk_settle_payee   FOREIGN KEY (payee_id)    REFERENCES users(id)        ON DELETE RESTRICT,
    CONSTRAINT fk_settle_verifier FOREIGN KEY (verified_by) REFERENCES users(id)       ON DELETE SET NULL,
    INDEX idx_settle_group (group_id),
    INDEX idx_settle_payer (payer_id),
    INDEX idx_settle_payee (payee_id),
    INDEX idx_settle_status (status)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: notifications (Observer Pattern)
-- Owner: Samyuktha S / Saanvi Kakkar
-- ============================================================
CREATE TABLE IF NOT EXISTS notifications (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT NOT NULL,
    type       ENUM('EXPENSE_ADDED', 'SETTLEMENT_REQUEST', 'SETTLEMENT_VERIFIED', 'SETTLEMENT_REJECTED', 'GROUP_INVITE', 'DEBT_REMINDER') NOT NULL,
    title      VARCHAR(200) NOT NULL,
    message    TEXT NOT NULL,
    is_read    BOOLEAN NOT NULL DEFAULT FALSE,
    ref_id     BIGINT,
    ref_type   VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_notif_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_notif_user (user_id),
    INDEX idx_notif_unread (user_id, is_read)
) ENGINE=InnoDB;

-- ============================================================
-- TABLE: audit_logs
-- Owner: Saanvi Kakkar (Admin Feature)
-- ============================================================
CREATE TABLE IF NOT EXISTS audit_logs (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id     BIGINT,
    action      VARCHAR(100) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id   BIGINT,
    old_value   TEXT,
    new_value   TEXT,
    ip_address  VARCHAR(45),
    user_agent  VARCHAR(500),
    timestamp   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
    INDEX idx_audit_user (user_id),
    INDEX idx_audit_timestamp (timestamp),
    INDEX idx_audit_entity (entity_type, entity_id)
) ENGINE=InnoDB;

-- ============================================================
-- SEED DATA: Default Admin User
-- Password: admin123 (BCrypt encoded)
-- ============================================================
INSERT INTO users (username, email, password, full_name, role)
VALUES ('admin', 'admin@fintrack.com',
        '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lH',
        'FinTrack Administrator', 'ADMIN')
ON DUPLICATE KEY UPDATE id = id;
