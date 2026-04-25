-- TripForge Auth Service — Initial Schema
-- Creates the auth schema and core tables

-- Create schema
CREATE SCHEMA IF NOT EXISTS auth;

-- ── Users table ───────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS auth.users (
    id              BIGSERIAL PRIMARY KEY,
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    first_name      VARCHAR(100) NOT NULL,
    last_name       VARCHAR(100) NOT NULL,
    phone           VARCHAR(20),
    role            VARCHAR(20)  NOT NULL DEFAULT 'USER',
    enabled         BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_users_email ON auth.users(email);

-- ── User preferences table ────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS auth.user_preferences (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT       NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
    interests           VARCHAR(500),
    hotel_preference    VARCHAR(20)  DEFAULT 'STANDARD',
    default_budget      NUMERIC(12, 2),
    default_travelers   INTEGER      DEFAULT 2,
    updated_at          TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_user_preferences_user_id ON auth.user_preferences(user_id);

-- ── Seed a demo admin user (password: Admin@123456) ───────────────────────────
-- BCrypt hash for "Admin@123456" with strength 12
INSERT INTO auth.users (email, password_hash, first_name, last_name, role, enabled)
VALUES (
    'admin@tripforge.com',
    '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/LewdBPj4J/HS.iK8i',
    'TripForge',
    'Admin',
    'ADMIN',
    TRUE
) ON CONFLICT (email) DO NOTHING;
