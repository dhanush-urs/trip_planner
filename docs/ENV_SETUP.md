# TripForge — Environment Variables Reference

Copy `.env.example` to `.env` before running:
```bash
cp .env.example .env
```

---

## Required Variables (system won't start without these)

| Variable | Default | Description |
|---|---|---|
| `DB_USERNAME` | `tripforge_user` | PostgreSQL username |
| `DB_PASSWORD` | `tripforge_pass` | PostgreSQL password — **change in production** |
| `DB_NAME` | `tripforge` | PostgreSQL database name |
| `JWT_SECRET` | (long string) | JWT signing secret — **must be 32+ chars, change in production** |

---

## Optional Variables (system degrades gracefully without these)

### External Data Providers

| Variable | Feature Enabled | Fallback When Missing |
|---|---|---|
| `GOOGLE_PLACES_API_KEY` | Live hotel/attraction search | CSV dataset (45 hotels, 50 attractions) |
| `GOOGLE_DIRECTIONS_API_KEY` | Real travel times + route optimization | Heuristic nearest-neighbor |
| `OPENTRIPMAP_API_KEY` | Fallback attraction search | CSV dataset |
| `OPENROUTESERVICE_API_KEY` | Fallback route optimization | Heuristic |

### AI Orchestration

| Variable | Default | Feature Enabled | Fallback |
|---|---|---|---|
| `GEMINI_API_KEY` | (empty) | Live AI explanations | Deterministic templates |
| `GEMINI_MODEL` | `gemini-1.5-flash` | Model selection | N/A |
| `GEMINI_ENABLED` | `true` | Enable/disable Gemini | N/A |
| `GEMINI_TIMEOUT_MS` | `5000` | Request timeout | N/A |

### Payment Gateway

| Variable | Feature Enabled | Behavior When Missing |
|---|---|---|
| `RAZORPAY_KEY_ID` | Live payments | Payment UI shows "not configured" |
| `RAZORPAY_KEY_SECRET` | Payment verification | Orders cannot be created |
| `RAZORPAY_WEBHOOK_SECRET` | Webhook signature verification | Webhooks rejected |
| `PAYMENTS_ENABLED` | Enable/disable payments | N/A |

### Infrastructure

| Variable | Default | Description |
|---|---|---|
| `REDIS_HOST` | `redis` | Redis hostname |
| `REDIS_PORT` | `6379` | Redis port |
| `EUREKA_URL` | `http://discovery-server:8761/eureka/` | Eureka endpoint |

### Service Ports

| Variable | Default |
|---|---|
| `GATEWAY_PORT` | `8080` |
| `AUTH_PORT` | `8081` |
| `TRIP_PORT` | `8082` |
| `HOTEL_PORT` | `8083` |
| `ROUTE_PORT` | `8084` |
| `BUDGET_PORT` | `8085` |
| `SPLIT_PORT` | `8086` |
| `ML_PORT` | `8087` |
| `EXTERNAL_DATA_PORT` | `8088` |
| `AI_ORCHESTRATOR_PORT` | `8089` |
| `PAYMENT_SERVICE_PORT` | `8090` |
| `FRONTEND_PORT` | `3000` |

---

## Feature Matrix

| Feature | No Keys | Google Keys | + Gemini | + Razorpay |
|---|---|---|---|---|
| Trip creation | ✅ CSV data | ✅ Live data | ✅ + AI explanations | ✅ + Payments |
| Hotel recommendations | ✅ CSV | ✅ Live | ✅ + Why this hotel | ✅ |
| Route optimization | ✅ Heuristic | ✅ Real times | ✅ + Why this route | ✅ |
| Currency conversion | ✅ Frankfurter (no key) | ✅ | ✅ | ✅ |
| Split expenses | ✅ | ✅ | ✅ | ✅ |
| Payments | ❌ Degraded | ❌ Degraded | ❌ Degraded | ✅ Live |
