# TripForge — System Design

## Overview

TripForge is a microservices-based trip planning application. The system decomposes the trip planning domain into independent, single-responsibility services that communicate via REST (synchronous) and are discovered via Eureka.

---

## Architecture Overview

```
Browser
  │
  ▼
Frontend (React + Nginx :3000)
  │  HTTP
  ▼
API Gateway (:8080)
  │  JWT validation
  │  CORS
  │  Load-balanced routing via Eureka
  ├──▶ auth-service (:8081)
  ├──▶ trip-service (:8082)
  ├──▶ hotel-service (:8083)
  ├──▶ route-service (:8084)
  ├──▶ budget-service (:8085)
  ├──▶ split-service (:8086)
  └──▶ ml-service (:8087)  [direct URL, not Eureka]

Discovery Server (:8761)
  └── All Java services register here on startup

PostgreSQL (:5432)
  └── 6 logical schemas, one per service domain

ml-service
  └── Standalone FastAPI, no Eureka registration
  └── Called directly by hotel-service via HTTP
```

---

## Service Responsibilities

### discovery-server
- Netflix Eureka server
- All Java microservices register on startup
- Gateway uses `lb://service-name` URIs for load-balanced routing
- Dashboard at http://localhost:8761

### api-gateway
- Single entry point for all client traffic
- JWT validation via `JwtAuthFilter` (Spring Cloud Gateway filter)
- Forwards `X-User-Id` and `X-User-Email` headers to downstream services
- CORS configured for React frontend (localhost:5173, localhost:3000)
- Public paths: `/api/auth/**`, `/actuator/**`
- All other `/api/**` paths require valid JWT

### auth-service
- User registration with BCrypt password hashing (strength 12)
- Login with JWT generation (JJWT 0.12, HS256)
- User profile and travel preference storage
- JWT secret must match api-gateway exactly
- Database schema: `auth`

### trip-service
- Orchestrates the full trip planning flow
- On `POST /api/trip/create`:
  1. Saves trip skeleton to DB
  2. Calls hotel-service for recommendations (Feign)
  3. Calls route-service for itinerary (Feign)
  4. Calls budget-service for cost breakdown (Feign)
  5. Calls split-service for expense split (Feign)
  6. Merges all results into TripResponse
- All downstream calls are wrapped in try/catch — partial failures return partial responses
- Database schema: `trip`

### hotel-service
- Maintains hotel dataset (45 hotels, 5 destinations)
- Loads CSV on startup via CommandLineRunner
- Recommendation pipeline:
  1. Rule-based filter (destination, budget, category)
  2. ML ranking via ml-service (with fallback)
  3. Returns top-N ranked hotels
- Supports hotel change with feedback-aware re-ranking
- Database schema: `hotel`

### route-service
- Maintains attractions dataset (50 attractions, 5 destinations)
- Loads CSV on startup via CommandLineRunner
- Itinerary algorithm:
  1. Filter by destination and user interests
  2. Score by interest match + priority
  3. Group by distance cluster (A/B/C) for geographic coherence
  4. Assign 3 attractions/day with time slots
- Database schema: `route`

### budget-service
- Destination-aware cost estimation
- Daily food rates: Bangalore ₹800, Goa/Manali ₹600, others ₹400
- Daily transport rates: Manali ₹600, Bangalore ₹500, Goa ₹400, others ₹300
- Misc = 5% of subtotal
- Flags over-budget conditions
- Database schema: `budget`

### split-service
- Equal split: total / travelers
- Custom split: percentage-based per participant
- Stores participant breakdown
- Database schema: `split_schema`

### ml-service
- Python 3.11 + FastAPI
- Does NOT register with Eureka (Python service)
- Called directly by hotel-service via `http://ml-service:8087`
- Three endpoints:
  - `POST /api/ml/hotel-rank` — hybrid GBR ranking
  - `POST /api/ml/recommend-alternative-hotel` — feedback re-ranking
  - `POST /api/ml/classify-trip-style` — RandomForest classifier
- Graceful degradation: if models missing, falls back to rule-based scoring

---

## Inter-Service Communication

### Synchronous REST (OpenFeign)
All service-to-service calls use Spring Cloud OpenFeign with Eureka load balancing.

```
trip-service
  ├── HotelServiceClient  → GET  /api/hotels/recommend
  ├── RouteServiceClient  → POST /api/route/optimize
  ├── BudgetServiceClient → POST /api/budget/calculate
  └── SplitServiceClient  → POST /api/split/equal

hotel-service
  └── MlServiceClient     → POST /ml/hotel-rank
                          → POST /ml/recommend-alternative-hotel
```

Feign timeouts: connect 5s, read 10s. All calls wrapped in try/catch for graceful degradation.

### Header Propagation
Gateway injects `X-User-Id` and `X-User-Email` headers after JWT validation. Downstream services read user identity from these headers — they never re-parse the JWT.

---

## Gateway Role

```
Request flow:
  1. Browser sends request with Authorization: Bearer <token>
  2. Gateway JwtAuthFilter validates token signature + expiry
  3. Gateway extracts userId and email from token claims
  4. Gateway adds X-User-Id and X-User-Email headers
  5. Gateway routes to downstream service via Eureka lb://
  6. Downstream service reads X-User-Id for authorization
```

Public paths bypass JWT validation:
- `POST /api/auth/register`
- `POST /api/auth/login`
- `/actuator/**`

---

## Eureka Role

```
Startup sequence:
  1. discovery-server starts, Eureka registry ready
  2. Each Java service registers with:
     - spring.application.name (e.g., "auth-service")
     - instance IP and port
  3. Gateway fetches registry, resolves lb://auth-service → actual IP:port
  4. Feign clients resolve lb://hotel-service → actual IP:port
  5. Registry refreshes every 30s
```

ml-service does NOT register with Eureka. hotel-service calls it via direct URL (`http://ml-service:8087`), which resolves via Docker DNS.

---

## ML Integration

```
hotel-service receives recommendation request
  │
  ▼
Step 1: Rule-based filter
  Filter by destination, budget, hotel category preference
  │
  ▼
Step 2: Build feature vectors (16 features per hotel)
  budget_per_day, duration, travelers, price, rating, distance,
  destination_encoded, preference_encoded, category_encoded,
  popularity, price_fit_score, rating_score, distance_score,
  category_match_score, amenities_match_score, price_fit_ratio
  │
  ▼
Step 3: Call ml-service POST /ml/hotel-rank
  GradientBoostingRegressor predicts relevance score
  │
  ▼
Step 4: Hybrid fusion
  final_score = 0.65 × ml_score + 0.35 × rule_score
  │
  ▼
Step 5: Generate reason tags
  budget_fit, high_rating, close_to_center, matches_preference, etc.
  │
  ▼
Return ranked hotels to trip-service
```

If ml-service is unavailable, hotel-service falls back to pure rule-based scoring. The trip creation still succeeds.

---

## Deployment Topology (Local Docker)

```
Docker network: tripforge-net (bridge)

┌─────────────────────────────────────────────────────────┐
│  tripforge-net                                          │
│                                                         │
│  tripforge-postgres    :5432  (volume: postgres-data)   │
│  tripforge-discovery   :8761                            │
│  tripforge-gateway     :8080                            │
│  tripforge-auth        :8081                            │
│  tripforge-trip        :8082                            │
│  tripforge-hotel       :8083                            │
│  tripforge-route       :8084                            │
│  tripforge-budget      :8085                            │
│  tripforge-split       :8086                            │
│  tripforge-ml          :8087                            │
│  tripforge-frontend    :80 → host:3000                  │
└─────────────────────────────────────────────────────────┘
```

All services communicate via container names (Docker DNS). No hardcoded IPs.

---

## Design Decisions

| Decision | Rationale |
|---|---|
| One PostgreSQL, multiple schemas | Simpler local setup; logical isolation without operational overhead |
| Feign over RestTemplate | Declarative, integrates with Eureka load balancing, cleaner code |
| Python for ML | scikit-learn ecosystem; Java ML libraries are less mature for this use case |
| Direct URL for ml-service | Python services don't support Eureka client natively |
| Graceful degradation on Feign failures | Trip creation should not fail if one downstream service is slow |
| JWT in gateway only | Downstream services trust X-User-Id header; avoids JWT parsing in every service |
| Flyway for migrations | Reproducible schema management; `validate` mode in production prevents drift |
| CSV data loading | Simple, portable, no external data dependency for demo |
