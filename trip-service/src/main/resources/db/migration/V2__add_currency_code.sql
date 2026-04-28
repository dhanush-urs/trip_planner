-- Phase 9E: Add currency_code to trips table
-- Non-destructive: adds column with default INR so existing rows are unaffected

ALTER TABLE trip.trips
    ADD COLUMN IF NOT EXISTS currency_code VARCHAR(10) NOT NULL DEFAULT 'INR';
