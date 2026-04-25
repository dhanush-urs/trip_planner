CREATE SCHEMA IF NOT EXISTS route;

CREATE TABLE IF NOT EXISTS route.attractions (
    id                      BIGSERIAL PRIMARY KEY,
    destination             VARCHAR(100)    NOT NULL,
    name                    VARCHAR(200)    NOT NULL,
    category                VARCHAR(100)    NOT NULL,
    avg_visit_hours         DOUBLE PRECISION NOT NULL,
    ticket_cost             DOUBLE PRECISION NOT NULL DEFAULT 0,
    priority_score          DOUBLE PRECISION NOT NULL,
    distance_cluster        VARCHAR(5),
    suitable_for_interests  VARCHAR(300)
);

CREATE INDEX IF NOT EXISTS idx_attractions_destination ON route.attractions(LOWER(destination));
CREATE INDEX IF NOT EXISTS idx_attractions_category ON route.attractions(category);
