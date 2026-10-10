-- ============================================================
-- 01_create_schema.sql
-- Creates the atmdb database and the atm schema inside it.
-- This script is executed once on first initialization of the
-- Postgres container (mounted under /docker-entrypoint-initdb.d).
-- ============================================================

-- Create the application database (idempotent-safe via DO block).
-- Note: the running role (typically the POSTGRES_USER) must be
-- able to CREATE DATABASE. On hosted Postgres / RDS where this
-- is not allowed, the database should be provisioned out-of-band.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_database WHERE datname = 'atmdb') THEN
        PERFORM dblink_exec(
            'dbname=' || current_database(),
            'CREATE DATABASE atmdb'
        );
    END IF;
END$$;

-- Connect to the new database so the rest of the init scripts
-- operate inside it. \c is a psql meta-command (only valid when
-- the file is run via psql, which is how the docker-entrypoint
-- runs init scripts).
\c atmdb

-- Application schema. Using a dedicated schema keeps the
-- application tables separate from default public objects.
CREATE SCHEMA IF NOT EXISTS atm;

-- Make atm the default search_path so subsequent SQL in this
-- session (and any psql session inheriting this) resolves tables
-- without schema-qualifying them.
SET search_path TO atm, public;

-- Enable the pgcrypto extension so gen_random_uuid() works for
-- the transactions.reference_number default.
CREATE EXTENSION IF NOT EXISTS pgcrypto;
