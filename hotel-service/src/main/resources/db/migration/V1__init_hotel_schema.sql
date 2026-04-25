CREATE SCHEMA IF NOT EXISTS hotel;

CREATE TABLE IF NOT EXISTS hotel.hotels (
    id                          BIGSERIAL PRIMARY KEY,
    destination                 VARCHAR(100)    NOT NULL,
    name                        VARCHAR(200)    NOT NULL,
    price_per_night             DOUBLE PRECISION NOT NULL,
    rating                      DOUBLE PRECISION NOT NULL,
    distance_from_center_km     DOUBLE PRECISION NOT NULL,
    amenities                   VARCHAR(500),
    category                    VARCHAR(20)     NOT NULL,
    popularity_score            DOUBLE PRECISION NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_hotels_destination ON hotel.hotels(LOWER(destination));
CREATE INDEX IF NOT EXISTS idx_hotels_category ON hotel.hotels(category);
