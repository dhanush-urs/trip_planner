CREATE SCHEMA IF NOT EXISTS budget;

CREATE TABLE IF NOT EXISTS budget.budget_breakdowns (
    id                  BIGSERIAL PRIMARY KEY,
    trip_id             BIGINT          NOT NULL UNIQUE,
    hotel_cost          NUMERIC(12,2)   NOT NULL,
    food_cost           NUMERIC(12,2)   NOT NULL,
    transport_cost      NUMERIC(12,2)   NOT NULL,
    attraction_cost     NUMERIC(12,2)   NOT NULL,
    misc_cost           NUMERIC(12,2)   NOT NULL,
    total_estimated     NUMERIC(12,2)   NOT NULL,
    total_budget        NUMERIC(12,2)   NOT NULL,
    over_budget         BOOLEAN         NOT NULL DEFAULT FALSE,
    calculated_at       TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_budget_trip_id ON budget.budget_breakdowns(trip_id);
