-- TripForge Auth Service — V2 Migration
-- Ensures auth schema and user_preferences table exist.
-- This migration is idempotent and safe to run on any state.
-- It guards against cases where V1 ran but user_preferences was not created
-- (e.g. partial migration, schema search_path issues, or baseline edge cases).

-- Ensure schema exists (safe no-op if already present)
CREATE SCHEMA IF NOT EXISTS auth;

-- Ensure users table exists (dependency for the FK below)
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

-- Ensure user_preferences table exists
-- Matches UserPreference entity exactly:
--   @Id                          → id BIGSERIAL PK
--   @JoinColumn(user_id, unique) → user_id BIGINT NOT NULL UNIQUE FK
--   @Column(length=500)          → interests VARCHAR(500)
--   @Enumerated(STRING) len=20   → hotel_preference VARCHAR(20)
--   @Column(precision=12,scale=2)→ default_budget NUMERIC(12,2)
--   @Column                      → default_travelers INTEGER
--   @UpdateTimestamp             → updated_at TIMESTAMP
CREATE TABLE IF NOT EXISTS auth.user_preferences (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL UNIQUE
                            REFERENCES auth.users(id) ON DELETE CASCADE,
    interests           VARCHAR(500),
    hotel_preference    VARCHAR(20)     DEFAULT 'STANDARD',
    default_budget      NUMERIC(12, 2),
    default_travelers   INTEGER         DEFAULT 2,
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

-- Indexes (safe no-op if already present)
CREATE INDEX IF NOT EXISTS idx_users_email
    ON auth.users(email);

CREATE INDEX IF NOT EXISTS idx_user_preferences_user_id
    ON auth.user_preferences(user_id);
