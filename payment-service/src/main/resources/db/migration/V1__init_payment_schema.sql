-- TripForge Payment Service — Initial Schema

CREATE SCHEMA IF NOT EXISTS payment;

-- ── Transactions ──────────────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment.transactions (
    id                      BIGSERIAL       PRIMARY KEY,
    trip_id                 BIGINT          NOT NULL,
    user_id                 BIGINT          NOT NULL,
    participant_id          BIGINT,
    participant_name        VARCHAR(150),
    participant_email       VARCHAR(255),
    gateway_provider        VARCHAR(50)     NOT NULL DEFAULT 'RAZORPAY',
    gateway_order_id        VARCHAR(255)    NOT NULL,
    gateway_payment_id      VARCHAR(255),
    gateway_signature       VARCHAR(512),
    amount                  NUMERIC(14,2)   NOT NULL,
    currency_code           VARCHAR(10)     NOT NULL DEFAULT 'INR',
    payment_type            VARCHAR(20)     NOT NULL,   -- FULL / SHARE
    status                  VARCHAR(20)     NOT NULL DEFAULT 'CREATED',
    payment_link            VARCHAR(1000),
    idempotency_key         VARCHAR(255),
    raw_gateway_payload_json TEXT,
    created_at              TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_txn_trip_id         ON payment.transactions(trip_id);
CREATE INDEX IF NOT EXISTS idx_txn_user_id         ON payment.transactions(user_id);
CREATE UNIQUE INDEX IF NOT EXISTS idx_txn_order_id ON payment.transactions(gateway_order_id);
CREATE INDEX IF NOT EXISTS idx_txn_payment_id      ON payment.transactions(gateway_payment_id)
    WHERE gateway_payment_id IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_txn_idempotency     ON payment.transactions(idempotency_key)
    WHERE idempotency_key IS NOT NULL;

-- ── Trip Payment Summary ──────────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment.trip_payment_summary (
    id                  BIGSERIAL       PRIMARY KEY,
    trip_id             BIGINT          NOT NULL UNIQUE,
    total_amount        NUMERIC(14,2)   NOT NULL,
    currency_code       VARCHAR(10)     NOT NULL DEFAULT 'INR',
    amount_paid         NUMERIC(14,2)   NOT NULL DEFAULT 0,
    amount_remaining    NUMERIC(14,2)   NOT NULL,
    status              VARCHAR(20)     NOT NULL DEFAULT 'UNPAID',
    last_payment_at     TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_summary_trip_id ON payment.trip_payment_summary(trip_id);

-- ── Participant Payment Status ────────────────────────────────────────────────
CREATE TABLE IF NOT EXISTS payment.participant_payment_status (
    id                  BIGSERIAL       PRIMARY KEY,
    trip_id             BIGINT          NOT NULL,
    participant_id      BIGINT,
    participant_name    VARCHAR(150)    NOT NULL,
    participant_email   VARCHAR(255),
    allocated_amount    NUMERIC(14,2)   NOT NULL,
    paid_amount         NUMERIC(14,2)   NOT NULL DEFAULT 0,
    currency_code       VARCHAR(10)     NOT NULL DEFAULT 'INR',
    status              VARCHAR(20)     NOT NULL DEFAULT 'UNPAID',
    payment_link        VARCHAR(1000),
    last_payment_at     TIMESTAMP,
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_pps_trip_id ON payment.participant_payment_status(trip_id);
