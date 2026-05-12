-- =====================================================================
-- Distributed Online Marketplace - Database Schema
-- MariaDB 11.4 with Table Partitioning
-- =====================================================================

SET NAMES utf8mb4;
SET CHARACTER SET utf8mb4;

-- ============================================
-- USERS TABLE — HASH partitioned by user_id
-- ============================================
CREATE TABLE IF NOT EXISTS users (
    user_id      BIGINT AUTO_INCREMENT,
    username     VARCHAR(50)  NOT NULL,
    email        VARCHAR(100) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    full_name    VARCHAR(100),
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id),
    INDEX idx_username (username),
    INDEX idx_email (email)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY HASH(user_id) PARTITIONS 4;

-- ============================================
-- WALLETS TABLE — HASH partitioned by user_id
-- ============================================
CREATE TABLE IF NOT EXISTS wallets (
    wallet_id     BIGINT AUTO_INCREMENT,
    user_id       BIGINT NOT NULL,
    balance_cents BIGINT NOT NULL DEFAULT 0,
    created_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at    DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (wallet_id, user_id),
    INDEX idx_wallet_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY HASH(user_id) PARTITIONS 4;

-- ============================================
-- ITEMS TABLE — HASH partitioned by seller_id
-- ============================================
CREATE TABLE IF NOT EXISTS items (
    item_id     BIGINT AUTO_INCREMENT,
    seller_id   BIGINT NOT NULL,
    name        VARCHAR(200) NOT NULL,
    description TEXT,
    brand       VARCHAR(100),
    category    VARCHAR(100),
    price_cents BIGINT NOT NULL,
    image_url   VARCHAR(500),
    status      ENUM('ACTIVE','SOLD','REMOVED') DEFAULT 'ACTIVE',
    created_at  DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (item_id, seller_id),
    INDEX idx_item_seller (seller_id),
    INDEX idx_item_status (status),
    INDEX idx_item_name (name),
    INDEX idx_item_brand (brand)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY HASH(seller_id) PARTITIONS 4;

-- ============================================
-- INVENTORY TABLE — HASH partitioned by item_id
-- ============================================
CREATE TABLE IF NOT EXISTS inventory (
    inventory_id BIGINT AUTO_INCREMENT,
    item_id      BIGINT NOT NULL,
    seller_id    BIGINT NOT NULL,
    quantity     INT NOT NULL DEFAULT 0,
    reserved     INT NOT NULL DEFAULT 0,
    updated_at   DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (inventory_id, item_id),
    INDEX idx_inv_item (item_id),
    INDEX idx_inv_seller (seller_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY HASH(item_id) PARTITIONS 4;

-- ============================================
-- TRANSACTIONS TABLE — RANGE partitioned by month
-- ============================================
CREATE TABLE IF NOT EXISTS transactions (
    transaction_id BIGINT AUTO_INCREMENT,
    buyer_id       BIGINT NOT NULL,
    seller_id      BIGINT NOT NULL,
    item_id        BIGINT NOT NULL,
    quantity       INT NOT NULL DEFAULT 1,
    total_cents    BIGINT NOT NULL,
    type           ENUM('PURCHASE','DEPOSIT','WITHDRAWAL','REFUND') NOT NULL,
    status         ENUM('PENDING','COMPLETED','FAILED','REFUNDED') DEFAULT 'PENDING',
    reference_code VARCHAR(100),
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (transaction_id, created_at),
    INDEX idx_txn_buyer (buyer_id),
    INDEX idx_txn_seller (seller_id),
    INDEX idx_txn_item (item_id),
    INDEX idx_txn_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY RANGE (YEAR(created_at) * 100 + MONTH(created_at)) (
    PARTITION p202501 VALUES LESS THAN (202502),
    PARTITION p202502 VALUES LESS THAN (202503),
    PARTITION p202503 VALUES LESS THAN (202504),
    PARTITION p202504 VALUES LESS THAN (202505),
    PARTITION p202505 VALUES LESS THAN (202506),
    PARTITION p202506 VALUES LESS THAN (202507),
    PARTITION p202507 VALUES LESS THAN (202508),
    PARTITION p_future VALUES LESS THAN MAXVALUE
);

-- ============================================
-- OTP_CODES TABLE — HASH partitioned by user_id
-- ============================================
CREATE TABLE IF NOT EXISTS otp_codes (
    otp_id     BIGINT AUTO_INCREMENT,
    user_id    BIGINT NOT NULL,
    code       VARCHAR(6) NOT NULL,
    purpose    ENUM('LOGIN','PURCHASE','ACCOUNT_CREATE') NOT NULL,
    expires_at DATETIME NOT NULL,
    used       BOOLEAN DEFAULT FALSE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (otp_id, user_id),
    INDEX idx_otp_user_code (user_id, code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY HASH(user_id) PARTITIONS 4;

-- ============================================
-- EXTERNAL_STORES TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS external_stores (
    store_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_name VARCHAR(200) NOT NULL,
    api_key    VARCHAR(255) NOT NULL UNIQUE,
    contact_email VARCHAR(150),
    is_active  BOOLEAN DEFAULT TRUE,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- STORE_LISTINGS TABLE
-- ============================================
CREATE TABLE IF NOT EXISTS store_listings (
    listing_id   BIGINT AUTO_INCREMENT PRIMARY KEY,
    store_id     BIGINT NOT NULL,
    item_id      BIGINT NOT NULL,
    external_ref VARCHAR(200),
    listed_at    DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_sl_store (store_id),
    INDEX idx_sl_item (item_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ============================================
-- DEPOSIT_LEDGER TABLE — HASH partitioned
-- ============================================
CREATE TABLE IF NOT EXISTS deposit_ledger (
    deposit_id   BIGINT AUTO_INCREMENT,
    user_id      BIGINT NOT NULL,
    amount_cents BIGINT NOT NULL,
    method       VARCHAR(50) DEFAULT 'MANUAL',
    reference_code VARCHAR(100),
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (deposit_id, user_id),
    INDEX idx_dep_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
PARTITION BY HASH(user_id) PARTITIONS 4;
