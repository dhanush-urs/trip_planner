# TripForge — Deployment Guide

## Local Docker Deployment (Recommended)

### Prerequisites
- Docker 24+ and Docker Compose v2
- Python 3.11+ (for ML model training — one-time)
- 4GB+ RAM allocated to Docker

### First-time setup

```bash
# 1. Clone and configure
git clone https://github.com/your-org/tripforge-ai.git
cd tripforge-ai
cp .env.example .env
cp frontend/.env.example frontend/.env

# 2. Train ML models (one-time, ~30 seconds)
bash scripts/train-ml-models.sh

# 3. Start everything
make up

# 4. Wait 3-4 minutes, then verify
make health
```

### Service Ports

| Service | Port | Notes |
|---|---|---|
| Frontend | 3000 | React + Nginx |
| API Gateway | 8080 | Single entry point |
| Eureka | 8761 | Service registry |
| Auth | 8081 | JWT auth |
| Trip | 8082 | Orchestration |
| Hotel | 8083 | Recommendations |
| Route | 8084 | Itinerary |
| Budget | 8085 | Cost calculation |
| Split | 8086 | Expense split |
| ML Service | 8087 | Python FastAPI |
| External Data | 8088 | Provider integration |
| AI Orchestrator | 8089 | Gemini integration |
| Payment | 8090 | Razorpay |
| PostgreSQL | 5432 | Database |
| Redis | 6379 | Cache |

### Startup Time Expectations

```
postgres + redis       → ~10s
discovery-server       → ~30s
api-gateway + services → ~90s
All services healthy   → ~3-4 minutes total
```

### Degraded Mode

The system runs in degraded mode when optional API keys are absent:

| Missing Key | Degraded Behavior |
|---|---|
| `GOOGLE_PLACES_API_KEY` | Hotels/attractions from CSV dataset |
| `GOOGLE_DIRECTIONS_API_KEY` | Heuristic route optimization |
| `GEMINI_API_KEY` | Deterministic AI fallbacks |
| `RAZORPAY_KEY_ID/SECRET` | Payment UI shows "not configured" |
| `OPENTRIPMAP_API_KEY` | Falls back to Google Places or CSV |

**Trip creation always succeeds regardless of which keys are missing.**

### Health Verification

```bash
make health
# or
bash scripts/check-health.sh
```

### Common Commands

```bash
make up        # Start full stack
make down      # Stop (keep volumes)
make reset     # Stop + wipe all data
make health    # Check all services
make logs      # Tail all logs
make logs-auth # Tail specific service
make ps        # Container status
```

### Restart a single service

```bash
docker compose restart auth-service
docker compose up --build -d auth-service  # rebuild + restart
```

### View service logs

```bash
make logs-auth
make logs-trip
make logs-payment
make logs-external
make logs-ai
```

---

## Production Deployment Notes

TripForge is designed for local/demo deployment via Docker Compose.
For production, the following changes would be needed:

1. **Secrets management** — use Vault, AWS Secrets Manager, or K8s secrets
2. **Database** — use managed PostgreSQL (RDS, Cloud SQL)
3. **Redis** — use managed Redis (ElastiCache, Upstash)
4. **Service discovery** — replace Eureka with K8s DNS or Consul
5. **Gateway** — add TLS termination, production CORS
6. **Scaling** — each service can be scaled independently
7. **Monitoring** — add Prometheus + Grafana or Datadog

See `DEPLOYMENT_TARGETS.md` for platform-specific notes.
