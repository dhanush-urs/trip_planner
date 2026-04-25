# TripForge — Database Schema

All services share one PostgreSQL 16 container with separate logical schemas.
Migrations are managed by Flyway and run automatically on service startup.

---

## Schema Overview

| Schema | Owned By | Tables |
|---|---|---|
| `auth` | auth-service | `users`, `user_preferences` |
| `trip` | trip-service | `trips`, `itinerary_days`, `trip_places` |
| `hotel` | hotel-service | `hotels` |
| `route` | route-service | `attractions` |
| `budget` | budget-service | `budget_breakdowns` |
| `split_schema` | split-service | `split_details`, `split_participants` |

---

## auth schema

### users
```sql
id              BIGSERIAL PRIMARY KEY
email           VARCHAR(255) NOT NULL UNIQUE
password_hash   VARCHAR(255) NOT NULL          -- BCrypt strength 12
first_name      VARCHAR(100) NOT NULL
last_name       VARCHAR(100) NOT NULL
phone           VARCHAR(20)
role            VARCHAR(20)  DEFAULT 'USER'    -- USER | ADMIN
enabled         BOOLEAN      DEFAULT TRUE
created_at      TIMESTAMP    DEFAULT NOW()
updated_at      TIMESTAMP    DEFAULT NOW()
```

### user_preferences
```sql
id                  BIGSERIAL PRIMARY KEY
user_id             BIGINT UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE
interests           VARCHAR(500)               -- comma-separated: "nature,food,beaches"
hotel_preference    VARCHAR(20) DEFAULT 'STANDARD'  -- BUDGET | STANDARD | LUXURY
default_budget      NUMERIC(12,2)
default_travelers   INTEGER DEFAULT 2
updated_at          TIMESTAMP DEFAULT NOW()
```

**Relationship:** One user → one preference record (created on first update, lazy).

---

## trip schema

### trips
```sql
id                  BIGSERIAL PRIMARY KEY
user_id             BIGINT NOT NULL            -- FK to auth.users (cross-schema reference, not enforced)
destination         VARCHAR(100) NOT NULL
start_date          DATE NOT NULL
end_date            DATE NOT NULL
total_budget        NUMERIC(12,2) NOT NULL
travelers           INTEGER NOT NULL
interests           VARCHAR(500)               -- comma-separated
hotel_preference    VARCHAR(20) DEFAULT 'STANDARD'
status              VARCHAR(20) DEFAULT 'PLANNED'  -- PLANNED | ACTIVE | COMPLETED | CANCELLED
selected_hotel_id   BIGINT                     -- FK to hotel.hotels (cross-schema, not enforced)
created_at          TIMESTAMP DEFAULT NOW()
updated_at          TIMESTAMP DEFAULT NOW()
```

### itinerary_days
```sql
id          BIGSERIAL PRIMARY KEY
trip_id     BIGINT NOT NULL REFERENCES trip.trips(id) ON DELETE CASCADE
day_number  INTEGER NOT NULL
date        DATE NOT NULL
theme       VARCHAR(200)
```

### trip_places
```sql
id                  BIGSERIAL PRIMARY KEY
itinerary_day_id    BIGINT NOT NULL REFERENCES trip.itinerary_days(id) ON DELETE CASCADE
attraction_id       BIGINT                     -- FK to route.attractions (cross-schema, not enforced)
name                VARCHAR(200) NOT NULL
category            VARCHAR(100)
visit_time          VARCHAR(20)                -- e.g. "09:00 AM"
avg_visit_hours     DOUBLE PRECISION
ticket_cost         NUMERIC(8,2)
notes               VARCHAR(500)
visit_order         INTEGER DEFAULT 1
```

**Relationships:**
```
trips (1) ──── (N) itinerary_days (1) ──── (N) trip_places
```

---

## hotel schema

### hotels
```sql
id                          BIGSERIAL PRIMARY KEY
destination                 VARCHAR(100) NOT NULL
name                        VARCHAR(200) NOT NULL
price_per_night             DOUBLE PRECISION NOT NULL
rating                      DOUBLE PRECISION NOT NULL
distance_from_center_km     DOUBLE PRECISION NOT NULL
amenities                   VARCHAR(500)               -- comma-separated
category                    VARCHAR(20) NOT NULL       -- BUDGET | STANDARD | LUXURY
popularity_score            DOUBLE PRECISION NOT NULL  -- 0-10
```

**Data source:** Loaded from `hotel-service/src/main/resources/dataset/hotels.csv` on startup.
45 hotels across Goa (10), Mysore (8), Bangalore (10), Ooty (8), Manali (9).

---

## route schema

### attractions
```sql
id                      BIGSERIAL PRIMARY KEY
destination             VARCHAR(100) NOT NULL
name                    VARCHAR(200) NOT NULL
category                VARCHAR(100) NOT NULL   -- nature | temple | beach | adventure | food | nightlife | shopping
avg_visit_hours         DOUBLE PRECISION NOT NULL
ticket_cost             DOUBLE PRECISION DEFAULT 0
priority_score          DOUBLE PRECISION NOT NULL  -- 1-10
distance_cluster        VARCHAR(5)              -- A | B | C (geographic grouping)
suitable_for_interests  VARCHAR(300)            -- comma-separated interest tags
```

**Data source:** Loaded from `route-service/src/main/resources/dataset/attractions.csv` on startup.
50 attractions across the same 5 destinations.

---

## budget schema

### budget_breakdowns
```sql
id                  BIGSERIAL PRIMARY KEY
trip_id             BIGINT NOT NULL UNIQUE     -- one breakdown per trip
hotel_cost          NUMERIC(12,2) NOT NULL
food_cost           NUMERIC(12,2) NOT NULL
transport_cost      NUMERIC(12,2) NOT NULL
attraction_cost     NUMERIC(12,2) NOT NULL
misc_cost           NUMERIC(12,2) NOT NULL     -- 5% of subtotal
total_estimated     NUMERIC(12,2) NOT NULL
total_budget        NUMERIC(12,2) NOT NULL
over_budget         BOOLEAN DEFAULT FALSE
calculated_at       TIMESTAMP DEFAULT NOW()
```

**Upsert behavior:** If a breakdown exists for a trip_id, it is updated (replan scenario).

---

## split_schema schema

### split_details
```sql
id                  BIGSERIAL PRIMARY KEY
trip_id             BIGINT NOT NULL UNIQUE
total_amount        NUMERIC(12,2) NOT NULL
travelers           INTEGER NOT NULL
per_person_amount   NUMERIC(12,2) NOT NULL
calculated_at       TIMESTAMP DEFAULT NOW()
```

### split_participants
```sql
split_detail_id     BIGINT NOT NULL REFERENCES split_schema.split_details(id) ON DELETE CASCADE
name                VARCHAR(100) NOT NULL      -- "Traveler 1", "Traveler 2", etc.
amount              NUMERIC(12,2) NOT NULL
percentage          DOUBLE PRECISION NOT NULL
```

**Relationship:** One split_detail → N participants (stored as @ElementCollection).

---

## Cross-Schema References

Services do not enforce foreign keys across schemas. References are logical:

| Field | Logical Reference | Enforcement |
|---|---|---|
| `trips.user_id` | `auth.users.id` | None (services are independent) |
| `trips.selected_hotel_id` | `hotel.hotels.id` | None |
| `trip_places.attraction_id` | `route.attractions.id` | None |

This is intentional — microservices own their data and do not share DB connections.

---

## Migration Notes

- All migrations use Flyway with `baseline-on-migrate: true`
- Migration files: `V1__init_<schema>_schema.sql` per service
- `JPA_DDL_AUTO=validate` in production — Flyway manages schema, Hibernate only validates
- Each service creates its own schema with `CREATE SCHEMA IF NOT EXISTS` in V1 migration
- Safe to run multiple services simultaneously — each owns a unique schema name
