-- ============================================================
-- 02_create_tables.sql
-- All ATM Enterprise application tables.
-- Schema: atm (set in 01_create_schema.sql)
-- ============================================================

SET search_path TO atm, public;

-- ------------------------------------------------------------
-- cards: physical/virtual ATM cards.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS cards (
    id                BIGSERIAL PRIMARY KEY,
    card_number       VARCHAR(16)  UNIQUE NOT NULL,
    pin_hash         VARCHAR       NOT NULL,
    holder_name      VARCHAR(100),
    status           VARCHAR(20)  DEFAULT 'ACTIVE',
    card_type        VARCHAR(20)  DEFAULT 'DEBIT',
    failed_attempts  INT           DEFAULT 0,
    expiry_date      DATE,
    daily_limit      DECIMAL(12,2) DEFAULT 50000,
    monthly_limit    DECIMAL(12,2) DEFAULT 200000,
    created_at       TIMESTAMP     DEFAULT NOW(),
    updated_at       TIMESTAMP,
    blocked_at       TIMESTAMP,
    blocked_reason   VARCHAR(255)
);

-- ------------------------------------------------------------
-- accounts: bank accounts linked (1:1) to a card via card_number.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS accounts (
    id              BIGSERIAL PRIMARY KEY,
    account_number  VARCHAR(14)  UNIQUE NOT NULL,
    card_number     VARCHAR(16)  REFERENCES cards(card_number),
    holder_name     VARCHAR(100),
    balance         DECIMAL(12,2) DEFAULT 0,
    account_type    VARCHAR(20)  DEFAULT 'SAVINGS',
    status          VARCHAR(20)  DEFAULT 'ACTIVE',
    currency        VARCHAR(3)   DEFAULT 'INR',
    created_at       TIMESTAMP    DEFAULT NOW(),
    updated_at       TIMESTAMP
);

-- ------------------------------------------------------------
-- transactions: immutable ledger of all money movement.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS transactions (
    id              BIGSERIAL PRIMARY KEY,
    reference_number UUID DEFAULT gen_random_uuid() UNIQUE,
    from_account    VARCHAR(14),
    to_account      VARCHAR(14),
    amount          DECIMAL(12,2),
    type            VARCHAR(20),
    status          VARCHAR(20),
    balance_before  DECIMAL(12,2),
    balance_after   DECIMAL(12,2),
    remark          VARCHAR(255),
    channel         VARCHAR(20)  DEFAULT 'ATM',
    timestamp       TIMESTAMP    DEFAULT NOW()
);

-- ------------------------------------------------------------
-- audit_logs: low-level security audit of card-scoped actions.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS audit_logs (
    id              BIGSERIAL PRIMARY KEY,
    card_number     VARCHAR(16),
    action          VARCHAR(50),
    ip_address      VARCHAR(45),
    status          VARCHAR(20),
    timestamp       TIMESTAMP DEFAULT NOW(),
    details         TEXT
);

-- ------------------------------------------------------------
-- account_audit: account-scoped audit (deposits, withdrawals,
-- transfers) — denormalized from transactions for fast lookup.
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS account_audit (
    id                BIGSERIAL PRIMARY KEY,
    account_number    VARCHAR(14),
    action            VARCHAR(20),
    amount            DECIMAL(12,2),
    balance_after     DECIMAL(12,2),
    reference_number  UUID,
    performed_by      VARCHAR(100),
    timestamp         TIMESTAMP DEFAULT NOW()
);

-- ------------------------------------------------------------
-- card_limits: rolling spend counters per card, reset daily /
-- monthly by a scheduled job (or on first txn of a new day).
-- ------------------------------------------------------------
CREATE TABLE IF NOT EXISTS card_limits (
    id               BIGSERIAL PRIMARY KEY,
    card_number      VARCHAR(16) UNIQUE,
    daily_spent      DECIMAL(12,2) DEFAULT 0,
    monthly_spent    DECIMAL(12,2) DEFAULT 0,
    last_reset_date  DATE DEFAULT CURRENT_DATE
);

-- ------------------------------------------------------------
-- Indexes for the hot access paths.
-- ------------------------------------------------------------
CREATE INDEX IF NOT EXISTS idx_transactions_from_account_ts
    ON transactions (from_account, timestamp);

CREATE INDEX IF NOT EXISTS idx_audit_logs_card_ts
    ON audit_logs (card_number, timestamp);

CREATE INDEX IF NOT EXISTS idx_accounts_card_number
    ON accounts (card_number);

-- Convenience index for reverse lookup (to_account history).
CREATE INDEX IF NOT EXISTS idx_transactions_to_account_ts
    ON transactions (to_account, timestamp);

-- Reference-number lookups (already unique, but explicit btree
-- helps ORM-side queries that filter by reference_number alone).
CREATE INDEX IF NOT EXISTS idx_account_audit_ref
    ON account_audit (reference_number);

-- Comments for the next DBA who reads this in pg_dump output.
COMMENT ON TABLE cards         IS 'ATM cards (debit/credit), with PIN hash and limits';
COMMENT ON TABLE accounts      IS 'Bank accounts, FK-linked to cards via card_number';
COMMENT ON TABLE transactions  IS 'Immutable money-movement ledger';
COMMENT ON TABLE audit_logs    IS 'Card-scoped security audit trail';
COMMENT ON TABLE account_audit IS 'Account-scoped audit trail';
COMMENT ON TABLE card_limits   IS 'Rolling daily/monthly spend per card';
