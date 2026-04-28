-- Phase 9E: Add currency fields to budget_breakdowns table
-- Non-destructive: all columns have safe defaults

ALTER TABLE budget.budget_breakdowns
    ADD COLUMN IF NOT EXISTS currency_code      VARCHAR(10)     NOT NULL DEFAULT 'INR',
    ADD COLUMN IF NOT EXISTS exchange_rate_used NUMERIC(18, 8),
    ADD COLUMN IF NOT EXISTS fx_source_provider VARCHAR(50),
    ADD COLUMN IF NOT EXISTS fx_fallback_used   BOOLEAN         NOT NULL DEFAULT FALSE;
