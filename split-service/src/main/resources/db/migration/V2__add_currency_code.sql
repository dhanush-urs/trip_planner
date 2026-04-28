-- Phase 9E: Add currency_code to split_details table
-- Non-destructive: default INR so existing rows are unaffected

ALTER TABLE split_schema.split_details
    ADD COLUMN IF NOT EXISTS currency_code VARCHAR(10) NOT NULL DEFAULT 'INR';
