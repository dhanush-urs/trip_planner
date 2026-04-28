-- Phase 9F: Add participantId, participantEmail, splitMode to split tables
-- Non-destructive: all new columns have safe defaults

ALTER TABLE split_schema.split_participants
    ADD COLUMN IF NOT EXISTS participant_id    BIGINT,
    ADD COLUMN IF NOT EXISTS participant_email VARCHAR(255);

ALTER TABLE split_schema.split_details
    ADD COLUMN IF NOT EXISTS split_mode VARCHAR(30) NOT NULL DEFAULT 'EQUAL';

CREATE INDEX IF NOT EXISTS idx_split_participants_trip
    ON split_schema.split_participants(split_detail_id);
