CREATE SCHEMA IF NOT EXISTS trip;

CREATE TABLE IF NOT EXISTS trip.trips (
    id                  BIGSERIAL PRIMARY KEY,
    user_id             BIGINT          NOT NULL,
    destination         VARCHAR(100)    NOT NULL,
    start_date          DATE            NOT NULL,
    end_date            DATE            NOT NULL,
    total_budget        NUMERIC(12,2)   NOT NULL,
    travelers           INTEGER         NOT NULL,
    interests           VARCHAR(500),
    hotel_preference    VARCHAR(20)     DEFAULT 'STANDARD',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PLANNED',
    selected_hotel_id   BIGINT,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_trips_user_id ON trip.trips(user_id);
CREATE INDEX IF NOT EXISTS idx_trips_destination ON trip.trips(destination);

CREATE TABLE IF NOT EXISTS trip.itinerary_days (
    id          BIGSERIAL PRIMARY KEY,
    trip_id     BIGINT       NOT NULL REFERENCES trip.trips(id) ON DELETE CASCADE,
    day_number  INTEGER      NOT NULL,
    date        DATE         NOT NULL,
    theme       VARCHAR(200)
);

CREATE INDEX IF NOT EXISTS idx_itinerary_days_trip_id ON trip.itinerary_days(trip_id);

CREATE TABLE IF NOT EXISTS trip.trip_places (
    id                  BIGSERIAL PRIMARY KEY,
    itinerary_day_id    BIGINT          NOT NULL REFERENCES trip.itinerary_days(id) ON DELETE CASCADE,
    attraction_id       BIGINT,
    name                VARCHAR(200)    NOT NULL,
    category            VARCHAR(100),
    visit_time          VARCHAR(20),
    avg_visit_hours     DOUBLE PRECISION,
    ticket_cost         NUMERIC(8,2),
    notes               VARCHAR(500),
    visit_order         INTEGER         NOT NULL DEFAULT 1
);
