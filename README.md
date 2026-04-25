<div align="center">

# ✦ TripForge

### AI-Powered Smart Trip Planner

*Plan smarter. Travel better.*

[![Java](https://img.shields.io/badge/Java-17-orange?style=flat-square&logo=openjdk)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.11-blue?style=flat-square&logo=python)](https://python.org)
[![FastAPI](https://img.shields.io/badge/FastAPI-0.111-teal?style=flat-square&logo=fastapi)](https://fastapi.tiangolo.com/)
[![React](https://img.shields.io/badge/React-18-61DAFB?style=flat-square&logo=react)](https://react.dev/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?style=flat-square&logo=docker)](https://docs.docker.com/compose/)

</div>

---

## What is TripForge?

TripForge is a **production-grade microservices application** that generates personalized trip itineraries using a hybrid ML recommendation pipeline.

A user enters a destination, budget, travel dates, and interests. TripForge returns:
- A **day-wise itinerary** with attraction scheduling
- **Hotel recommendations** ranked by a trained ML model
- A **budget breakdown** across hotel, food, transport, and attractions
- A **per-person expense split**
- The ability to **change hotels** with feedback-aware re-ranking

---

## Architecture

```
┌─────────────────┐     ┌──────────────────┐     ┌──────────────────────┐
│  React Frontend │────▶│   API Gateway    │────▶│  Discovery Server    │
│  :3000 (Nginx)  │     │  :8080           │     │  :8761 (Eureka)      │
└─────────────────┘     └────────┬─────────┘     └──────────────────────┘
                                 │
          ┌──────────────────────┼──────────────────────────┐
          ▼                      ▼                          ▼
   ┌─────────────┐      ┌──────────────┐          ┌──────────────────┐
   │ auth-service│      │ trip-service │          │  hotel-service   │
   │   :8081     │      │   :8082      │          │     :8083        │
   └─────────────┘      └──────┬───────┘          └────────┬─────────┘
                               │                           │
              ┌────────────────┼──────────┐                ▼
              ▼                ▼          ▼         ┌──────────────┐
       ┌────────────┐  ┌──────────┐  ┌────────┐    │  ml-service  │
       │route-svc   │  │budget-svc│  │split   │    │  :8087       │
       │  :8084     │  │  :8085   │  │ :8086  │    └──────────────┘
       └────────────┘  └──────────┘  └────────┘
                               │
                    ┌──────────┴──────────┐
                    ▼                     ▼
             ┌────────────┐       ┌────────────┐
             │ PostgreSQL │       │  (Redis -  │
             │   :5432    │       │  optional) │
             └────────────┘       └────────────┘
```

---

## Tech Stack

| Layer | Technology |
|---|---|
| Frontend | React 18, Vite, Tailwind CSS, React Router DOM 6 |
| API Gateway | Spring Cloud Gateway 2023, JWT validation, CORS |
| Backend Services | Java 17, Spring Boot 3.2, Spring Cloud OpenFeign |
| ML Service | Python 3.11, FastAPI, scikit-learn (GBR + RandomForest) |
| Database | PostgreSQL 16 — 6 logical schemas |
| Service Discovery | Netflix Eureka |
| Auth | JWT (JJWT 0.12), BCrypt strength 12 |
| DB Migrations | Flyway |
| Containerization | Docker, Docker Compose |

---

## Microservices

| Service | Port | Responsibility |
|---|---|---|
| `discovery-server` | 8761 | Eureka service registry |
| `api-gateway` | 8080 | JWT validation, routing, CORS |
| `auth-service` | 8081 | Register, login, JWT, user profile |
| `trip-service` | 8082 | Trip orchestration — calls all downstream services |
| `hotel-service` | 8083 | Hotel recommendations + ML ranking |
| `route-service` | 8084 | Day-wise itinerary generation |
| `budget-service` | 8085 | Budget breakdown calculation |
| `split-service` | 8086 | Per-person expense split |
| `ml-service` | 8087 | Hybrid ML hotel ranking + trip classifier |
| `frontend` | 3000 | React web application |

---

## Prerequisites

| Tool | Version | Purpose |
|---|---|---|
| Docker | 24+ | Run the full stack |
| Docker Compose | v2+ | Orchestration |
| Python 3.11+ | 3.11 | Train ML models (one-time) |
| Java 17+ | 17 | Local service development |
| Node.js | 20+ | Local frontend development |

---

## Quick Start

```bash
# 1. Clone
git clone https://github.com/your-org/tripforge-ai.git
cd tripforge-ai

# 2. Configure environment
cp .env.example .env
cp frontend/.env.example frontend/.env

# 3. Train ML models (one-time, ~30 seconds)
bash scripts/train-ml-models.sh

# 4. Verify setup
bash scripts/verify-setup.sh

# 5. Start everything
docker-compose up --build
# or: make up

# 6. Check health
bash scripts/check-health.sh
```

| URL | Description |
|---|---|
| http://localhost:3000 | TripForge Web App |
| http://localhost:8761 | Eureka Dashboard |
| http://localhost:8080/actuator/health | Gateway Health |
| http://localhost:8087/docs | ML Service API Docs |

---

## Startup Order

```
postgres (~10s) → discovery-server (~30s) → api-gateway + auth-service (~90s)
→ trip/hotel/route/budget/split (~120s) → ml-service (~30s) → frontend (~20s)

Total cold start: ~3-4 minutes
```

---

## Key API Endpoints

```
POST /api/auth/register       Register new user
POST /api/auth/login          Login, receive JWT
GET  /api/users/profile       Get user profile

POST /api/trip/create         Create + plan a trip (full orchestration)
GET  /api/trip/{id}           Get trip details
GET  /api/trip/user/{id}      Get user's trip history
PUT  /api/trip/replan         Replan with a different hotel

GET  /api/hotels/recommend    Get ML-ranked hotel recommendations
POST /api/hotels/change       Get alternatives by feedback reason

POST /api/ml/hotel-rank       Rank hotels (hybrid ML + rule-based)
POST /api/ml/classify-trip-style  Classify trip style
GET  /health                  ML service health
```

---

## ML Pipeline

```
Input: hotel candidates + trip context
  │
  ▼ Step 1: Rule-based sub-scores
    price_fit · rating · distance · category_match · amenities · popularity
  │
  ▼ Step 2: GradientBoostingRegressor prediction
    Trained on 23,625 samples · R²=0.90 · MAE=0.024
  │
  ▼ Step 3: Hybrid fusion
    final_score = 0.65 × ML + 0.35 × rule
  │
  ▼ Output: ranked hotels + reason tags + confidence scores
```

Feedback re-ranking adjusts weights per reason:
`CHEAPER` → price weight 0.45 · `BETTER_RATING` → rating weight 0.45
`CLOSER` → distance weight 0.45 · `PREMIUM` → category+amenities weight 0.50

---

## Datasets

| Dataset | Location | Records | Destinations |
|---|---|---|---|
| Hotels | `hotel-service/src/main/resources/dataset/hotels.csv` | 45 | Goa, Mysore, Bangalore, Ooty, Manali |
| Attractions | `route-service/src/main/resources/dataset/attractions.csv` | 50 | Same 5 |
| ML Training | `ml-service/training/datasets/` | 23,625 + 2,400 | Synthetic |

---

## Database Schemas

| Schema | Service | Key Tables |
|---|---|---|
| `auth` | auth-service | `users`, `user_preferences` |
| `trip` | trip-service | `trips`, `itinerary_days`, `trip_places` |
| `hotel` | hotel-service | `hotels` |
| `route` | route-service | `attractions` |
| `budget` | budget-service | `budget_breakdowns` |
| `split_schema` | split-service | `split_details`, `split_participants` |

---

## Troubleshooting

| Problem | Fix |
|---|---|
| ML models missing | `bash scripts/train-ml-models.sh` |
| 401 on all requests | `JWT_SECRET` must match in `.env` for gateway + auth |
| Empty trip history | Wait 2 min after startup for Eureka registration |
| Flyway migration fails | Check `DB_USERNAME`/`DB_PASSWORD` match postgres container |
| Frontend blank page | Rebuild with correct `VITE_API_BASE_URL` |
| Services not in Eureka | Wait full 2 minutes; Spring Cloud registration takes time |

---

## Documentation

| Doc | Description |
|---|---|
| [System Design](docs/SYSTEM_DESIGN.md) | Architecture, service responsibilities, communication |
| [API Contracts](docs/API_CONTRACTS.md) | All endpoints, request/response examples |
| [Database Schema](docs/DATABASE_SCHEMA.md) | Tables, relationships, migrations |
| [Demo Script](docs/DEMO_SCRIPT.md) | Live demo flow with exact inputs |
| [Test Plan](docs/TEST_PLAN.md) | Smoke tests, happy paths, failure scenarios |
| [Resume & Viva Pack](docs/RESUME_VIVA.md) | Resume bullets, explanations, interview answers |

---

## Resume Summary

> **TripForge** is a full-stack microservices application built with Java 17 + Spring Boot, Python FastAPI, and React 18. It features a 10-service architecture with Eureka service discovery, JWT authentication, a hybrid ML recommendation pipeline (GradientBoostingRegressor, R²=0.90), and a responsive React frontend. The system generates personalized trip itineraries with hotel recommendations, budget breakdowns, and expense splits for 5 Indian destinations.

---

## License

MIT
