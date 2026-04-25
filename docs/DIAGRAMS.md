# TripForge — Architecture & Sequence Diagrams

---

## 1. System Architecture Diagram

```mermaid
graph TB
    Browser["🌐 Browser\n:3000"]
    FE["Frontend\nReact + Nginx\n:3000"]
    GW["API Gateway\nSpring Cloud\n:8080"]
    DS["Discovery Server\nEureka\n:8761"]

    AUTH["auth-service\n:8081"]
    TRIP["trip-service\n:8082"]
    HOTEL["hotel-service\n:8083"]
    ROUTE["route-service\n:8084"]
    BUDGET["budget-service\n:8085"]
    SPLIT["split-service\n:8086"]
    ML["ml-service\nFastAPI\n:8087"]

    PG[("PostgreSQL\n:5432\n6 schemas")]

    Browser --> FE
    FE -->|"HTTP /api/**"| GW
    GW -->|"JWT validate\nlb://"| DS
    GW --> AUTH
    GW --> TRIP
    GW --> HOTEL
    GW --> ROUTE
    GW --> BUDGET
    GW --> SPLIT
    GW -->|"direct URL"| ML

    TRIP -->|"Feign"| HOTEL
    TRIP -->|"Feign"| ROUTE
    TRIP -->|"Feign"| BUDGET
    TRIP -->|"Feign"| SPLIT
    HOTEL -->|"Feign HTTP"| ML

    AUTH --- PG
    TRIP --- PG
    HOTEL --- PG
    ROUTE --- PG
    BUDGET --- PG
    SPLIT --- PG

    AUTH -.->|"register"| DS
    TRIP -.->|"register"| DS
    HOTEL -.->|"register"| DS
    ROUTE -.->|"register"| DS
    BUDGET -.->|"register"| DS
    SPLIT -.->|"register"| DS
```

---

## 2. Sequence Diagram: Create Trip

```mermaid
sequenceDiagram
    actor User
    participant FE as Frontend
    participant GW as API Gateway
    participant TS as trip-service
    participant HS as hotel-service
    participant RS as route-service
    participant BS as budget-service
    participant SS as split-service
    participant ML as ml-service

    User->>FE: Fill trip form & submit
    FE->>GW: POST /api/trip/create + JWT
    GW->>GW: Validate JWT, inject X-User-Id
    GW->>TS: POST /api/trip/create

    TS->>TS: Save trip skeleton to DB

    par Hotel recommendations
        TS->>HS: GET /api/hotels/recommend (Feign)
        HS->>HS: Rule-based filter
        HS->>ML: POST /ml/hotel-rank
        ML->>ML: GBR predict + hybrid fusion
        ML-->>HS: ranked hotels + scores
        HS-->>TS: HotelDto[] ranked
    and Itinerary generation
        TS->>RS: POST /api/route/optimize (Feign)
        RS->>RS: Interest scoring + cluster grouping
        RS-->>TS: DayPlanDto[] itinerary
    and Budget calculation
        TS->>BS: POST /api/budget/calculate (Feign)
        BS->>BS: Destination-aware cost estimation
        BS-->>TS: BudgetBreakdownDto
    and Expense split
        TS->>SS: POST /api/split/equal (Feign)
        SS->>SS: total / travelers
        SS-->>TS: SplitResultDto
    end

    TS->>TS: Merge all results into TripResponse
    TS-->>GW: TripResponse (201)
    GW-->>FE: TripResponse
    FE->>FE: normalizeTrip() + render result page
    FE-->>User: Trip result page
```

---

## 3. Sequence Diagram: Change Hotel

```mermaid
sequenceDiagram
    actor User
    participant FE as Frontend
    participant GW as API Gateway
    participant HS as hotel-service
    participant ML as ml-service
    participant TS as trip-service

    User->>FE: Click "Change Hotel"
    FE->>FE: Open HotelChangeModal
    User->>FE: Select reason (e.g. CHEAPER)
    User->>FE: Click "Find Alternatives"

    FE->>GW: POST /api/hotels/change + JWT
    GW->>HS: POST /api/hotels/change

    HS->>HS: Exclude current hotel
    HS->>HS: Apply reason-based pre-filter
    HS->>ML: POST /ml/recommend-alternative-hotel\n(feedback_reason: CHEAPER)
    ML->>ML: Adjust weights (price_fit → 0.45)
    ML->>ML: Re-rank candidates
    ML-->>HS: ranked alternatives
    HS-->>GW: HotelDto[] alternatives
    GW-->>FE: HotelDto[]
    FE->>FE: Render alternatives in modal

    User->>FE: Select alternative hotel
    FE->>GW: PUT /api/trip/replan + JWT
    GW->>TS: PUT /api/trip/replan

    TS->>TS: Update selected_hotel_id
    TS->>TS: Re-fetch budget (Feign → budget-service)
    TS->>TS: Re-fetch split (Feign → split-service)
    TS-->>GW: Updated TripResponse
    GW-->>FE: Updated TripResponse
    FE->>FE: Close modal, update hotel card + budget
    FE-->>User: Updated trip result
```

---

## 4. ER Diagram

```mermaid
erDiagram
    USERS {
        bigint id PK
        varchar email UK
        varchar password_hash
        varchar first_name
        varchar last_name
        varchar phone
        varchar role
        boolean enabled
        timestamp created_at
    }

    USER_PREFERENCES {
        bigint id PK
        bigint user_id FK
        varchar interests
        varchar hotel_preference
        numeric default_budget
        int default_travelers
    }

    TRIPS {
        bigint id PK
        bigint user_id
        varchar destination
        date start_date
        date end_date
        numeric total_budget
        int travelers
        varchar interests
        varchar hotel_preference
        varchar status
        bigint selected_hotel_id
        timestamp created_at
    }

    ITINERARY_DAYS {
        bigint id PK
        bigint trip_id FK
        int day_number
        date date
        varchar theme
    }

    TRIP_PLACES {
        bigint id PK
        bigint itinerary_day_id FK
        bigint attraction_id
        varchar name
        varchar category
        varchar visit_time
        numeric ticket_cost
        int visit_order
    }

    HOTELS {
        bigint id PK
        varchar destination
        varchar name
        double price_per_night
        double rating
        double distance_from_center_km
        varchar amenities
        varchar category
        double popularity_score
    }

    ATTRACTIONS {
        bigint id PK
        varchar destination
        varchar name
        varchar category
        double avg_visit_hours
        double ticket_cost
        double priority_score
        varchar distance_cluster
        varchar suitable_for_interests
    }

    BUDGET_BREAKDOWNS {
        bigint id PK
        bigint trip_id UK
        numeric hotel_cost
        numeric food_cost
        numeric transport_cost
        numeric attraction_cost
        numeric misc_cost
        numeric total_estimated
        numeric total_budget
        boolean over_budget
    }

    SPLIT_DETAILS {
        bigint id PK
        bigint trip_id UK
        numeric total_amount
        int travelers
        numeric per_person_amount
    }

    SPLIT_PARTICIPANTS {
        bigint split_detail_id FK
        varchar name
        numeric amount
        double percentage
    }

    USERS ||--o| USER_PREFERENCES : "has"
    TRIPS ||--|{ ITINERARY_DAYS : "contains"
    ITINERARY_DAYS ||--|{ TRIP_PLACES : "has"
    TRIPS ||--o| BUDGET_BREAKDOWNS : "has"
    TRIPS ||--o| SPLIT_DETAILS : "has"
    SPLIT_DETAILS ||--|{ SPLIT_PARTICIPANTS : "splits into"
```

---

## 5. ML Pipeline Diagram

```mermaid
flowchart TD
    A["Hotel Candidates\n(from DB filter)"] --> B

    B["Step 1: Rule-Based Scoring\n─────────────────\nprice_fit_score\nrating_score\ndistance_score\ncategory_match_score\namenities_match_score\npopularity_score_normalized"]

    B --> C["Step 2: Feature Vector\n16 numeric features\nper hotel candidate"]

    C --> D["GradientBoostingRegressor\n23,625 training samples\nR²=0.90 · MAE=0.024"]

    D --> E["ML Score\n(normalized 0-1)"]
    B --> F["Rule Score\n(composite 0-1)"]

    E --> G["Step 3: Hybrid Fusion\nfinal = 0.65×ML + 0.35×rule"]
    F --> G

    G --> H["Ranked Hotels\n+ reason tags"]

    style D fill:#0891b2,color:#fff
    style G fill:#0e7490,color:#fff
```
