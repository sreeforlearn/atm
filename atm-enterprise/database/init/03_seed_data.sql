-- ============================================================
-- 03_seed_data.sql
-- Demo data for local development.
-- PINs are stored as BCrypt hashes; the plaintext PIN for every
-- demo card is 1234 (cost factor 10). DO NOT use these hashes
-- in production — they are publicly known demo values.
-- ============================================================

SET search_path TO atm, public;

-- ------------------------------------------------------------
-- Cards (3 demo cards)
-- ------------------------------------------------------------
INSERT INTO cards
    (card_number, pin_hash, holder_name, status, card_type, failed_attempts, expiry_date, daily_limit, monthly_limit)
VALUES
    ('4111111111111111',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAg8B6CXuRjEaQKqHkXRjQ8lJjvqm',
     'Alice Sharma',
     'ACTIVE', 'DEBIT', 0, DATE '2027-12-31', 50000, 200000),

    ('4222222222222222',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAg8B6CXuRjEaQKqHkXRjQ8lJjvqm',
     'Bob Verma',
     'ACTIVE', 'DEBIT', 0, DATE '2027-06-30', 50000, 200000),

    ('4333333333333333',
     '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAg8B6CXuRjEaQKqHkXRjQ8lJjvqm',
     'Carol Iyer',
     'ACTIVE', 'CREDIT', 0, DATE '2028-03-31', 100000, 500000)
ON CONFLICT (card_number) DO NOTHING;

-- ------------------------------------------------------------
-- Accounts (linked 1:1 to the demo cards by card_number)
-- Balances: 50,000 / 25,000 / 100,000
-- ------------------------------------------------------------
INSERT INTO accounts
    (account_number, card_number, holder_name, balance, account_type, status, currency)
VALUES
    ('ACCT1000000001', '4111111111111111', 'Alice Sharma', 50000.00,  'SAVINGS', 'ACTIVE', 'INR'),
    ('ACCT2000000002', '4222222222222222', 'Bob Verma',   25000.00,  'CURRENT', 'ACTIVE', 'INR'),
    ('ACCT3000000003', '4333333333333333', 'Carol Iyer',  100000.00, 'SAVINGS', 'ACTIVE', 'INR')
ON CONFLICT (account_number) DO NOTHING;

-- ------------------------------------------------------------
-- Card limits (zero-initialized)
-- ------------------------------------------------------------
INSERT INTO card_limits (card_number, daily_spent, monthly_spent, last_reset_date)
VALUES
    ('4111111111111111', 0, 0, CURRENT_DATE),
    ('4222222222222222', 0, 0, CURRENT_DATE),
    ('4333333333333333', 0, 0, CURRENT_DATE)
ON CONFLICT (card_number) DO NOTHING;

-- ------------------------------------------------------------
-- Sample transactions
-- ------------------------------------------------------------
INSERT INTO transactions
    (reference_number, from_account, to_account, amount, type, status, balance_before, balance_after, remark, channel, timestamp)
VALUES
    (gen_random_uuid(), 'ACCT1000000001', NULL,                5000.00, 'WITHDRAWAL', 'SUCCESS', 55000.00, 50000.00, 'ATM cash withdrawal', 'ATM', NOW() - INTERVAL '2 days'),
    (gen_random_uuid(), 'ACCT2000000002', 'ACCT1000000001',   1000.00, 'TRANSFER',   'SUCCESS', 26000.00, 25000.00, 'IMPS transfer',       'MOBILE', NOW() - INTERVAL '1 day'),
    (gen_random_uuid(), 'ACCT3000000003', NULL,               500.00,  'WITHDRAWAL', 'FAILED',  100000.00, 100000.00,'Insufficient funds at ATM', 'ATM', NOW() - INTERVAL '3 hours'),
    (gen_random_uuid(), NULL,                'ACCT1000000001',  5000.00, 'DEPOSIT',    'SUCCESS', 45000.00, 50000.00,  'Cash deposit',        'ATM',   NOW() - INTERVAL '5 days'),
    (gen_random_uuid(), 'ACCT3000000003', 'ACCT2000000002',   2000.00, 'TRANSFER',   'SUCCESS', 102000.00, 100000.00,'NEFT transfer',       'MOBILE', NOW() - INTERVAL '6 hours')
ON CONFLICT DO NOTHING;

-- ------------------------------------------------------------
-- Sample account_audit entries (mirror of the transactions)
-- ------------------------------------------------------------
INSERT INTO account_audit
    (account_number, action, amount, balance_after, reference_number, performed_by, timestamp)
SELECT
    from_account, 'WITHDRAWAL', amount, balance_after, reference_number, 'system', timestamp
FROM transactions
WHERE from_account IS NOT NULL AND type = 'WITHDRAWAL' AND status = 'SUCCESS';

INSERT INTO account_audit
    (account_number, action, amount, balance_after, reference_number, performed_by, timestamp)
SELECT
    to_account, 'DEPOSIT', amount, balance_after, reference_number, 'system', timestamp
FROM transactions
WHERE to_account IS NOT NULL AND type = 'DEPOSIT' AND status = 'SUCCESS';

-- ------------------------------------------------------------
-- Sample audit_logs
-- ------------------------------------------------------------
INSERT INTO audit_logs (card_number, action, ip_address, status, timestamp, details)
VALUES
    ('4111111111111111', 'PIN_VERIFY', '192.168.1.10', 'SUCCESS', NOW() - INTERVAL '2 days', 'PIN verified for withdrawal'),
    ('4222222222222222', 'PIN_VERIFY', '192.168.1.11', 'SUCCESS', NOW() - INTERVAL '1 day',  'PIN verified for transfer'),
    ('4333333333333333', 'PIN_VERIFY', '192.168.1.12', 'FAILED',  NOW() - INTERVAL '3 hours','Insufficient funds — declined');

-- Sanity row-counts (visible in docker logs on first boot).
SELECT 'cards'         AS table_name, COUNT(*) FROM cards
UNION ALL SELECT 'accounts',      COUNT(*) FROM accounts
UNION ALL SELECT 'transactions',  COUNT(*) FROM transactions
UNION ALL SELECT 'audit_logs',    COUNT(*) FROM audit_logs
UNION ALL SELECT 'account_audit', COUNT(*) FROM account_audit
UNION ALL SELECT 'card_limits',   COUNT(*) FROM card_limits;
