CREATE SCHEMA IF NOT EXISTS split_schema;

CREATE TABLE IF NOT EXISTS split_schema.split_details (
    id                  BIGSERIAL PRIMARY KEY,
    trip_id             BIGINT          NOT NULL UNIQUE,
    total_amount        NUMERIC(12,2)   NOT NULL,
    travelers           INTEGER         NOT NULL,
    per_person_amount   NUMERIC(12,2)   NOT NULL,
    calculated_at       TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS split_schema.split_participants (
    split_detail_id     BIGINT          NOT NULL REFERENCES split_schema.split_details(id) ON DELETE CASCADE,
    name                VARCHAR(100)    NOT NULL,
    amount              NUMERIC(12,2)   NOT NULL,
    percentage          DOUBLE PRECISION NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_split_trip_id ON split_schema.split_details(trip_id);
