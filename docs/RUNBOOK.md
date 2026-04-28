# TripForge — Operations Runbook

## Service Recovery Procedures

### auth-service unhealthy

**Symptoms:** `make health` shows auth-service UNREACHABLE or DEGRADED

**Steps:**
```bash
# 1. Check logs
make logs-auth

# 2. Common causes:
# a) Flyway migration failed
docker compose logs auth-service | grep -i "flyway\|migration\|error"

# b) DB connection failed
docker compose logs auth-service | grep -i "connection\|refused\|timeout"

# 3. Restart
docker compose restart auth-service

# 4. If Flyway issue — full reset
make reset && make up
```

### Flyway migration failure

**Symptoms:** Service logs show `FlywayException` or `relation does not exist`

```bash
# Check which migration failed
docker compose logs <service-name> | grep -i "flyway\|migration"

# Full reset (wipes all data)
make reset
make up
```

### payment-service degraded

**Symptoms:** `make health` shows Razorpay NOT CONFIGURED

**This is expected behavior when keys are absent.**

```bash
# To enable live payments, add to .env:
RAZORPAY_KEY_ID=rzp_test_...
RAZORPAY_KEY_SECRET=...
RAZORPAY_WEBHOOK_SECRET=...

# Then restart payment-service only
docker compose up --build -d payment-service
```

### external-data-service degraded

**Symptoms:** Provider health shows NOT_CONFIGURED

**This is expected — system uses CSV fallback.**

```bash
# To enable live providers, add to .env:
GOOGLE_PLACES_API_KEY=...
GOOGLE_DIRECTIONS_API_KEY=...

# Restart
docker compose up --build -d external-data-service
```

### Test external provider fallback

```bash
# Check provider health
curl http://localhost:8088/api/external/providers/health | python3 -m json.tool

# Create trip without any API keys — should succeed with CSV fallback
curl -s -X POST http://localhost:8080/api/trip/create \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"destination":"Goa","startDate":"2025-09-01","endDate":"2025-09-05","totalBudget":50000,"travelers":2}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print('providerMode:', d.get('providerMode'))"
# Expected: providerMode: FALLBACK
```

### Verify Gemini degraded mode

```bash
curl http://localhost:8089/api/ai/health | python3 -m json.tool
# Expected without key: "configured": false, "mode": "FALLBACK"

# Test parse-preferences (should use keyword fallback)
curl -s -X POST http://localhost:8089/api/ai/parse-preferences \
  -H "Content-Type: application/json" \
  -d '{"text":"relaxed beach trip with good food"}' \
  | python3 -c "import sys,json; d=json.load(sys.stdin)['data']; print('fallback:', d.get('fallbackUsed'))"
# Expected: fallback: True
```

### ML models missing

```bash
# Retrain models
make train

# Rebuild ml-service
docker compose up --build -d ml-service
```

## Common Docker Commands

```bash
# View all containers
docker compose ps

# Restart one service
docker compose restart <service-name>

# Rebuild and restart one service
docker compose up --build -d <service-name>

# View logs (last 100 lines)
docker compose logs --tail=100 <service-name>

# Follow logs
docker compose logs -f <service-name>

# Execute command in container
docker compose exec postgres psql -U tripforge_user -d tripforge

# Check DB schemas
docker compose exec postgres psql -U tripforge_user -d tripforge -c "\dn"
```

## Common Curl Checks

```bash
# Gateway health
curl http://localhost:8080/actuator/health

# Auth health
curl http://localhost:8081/actuator/health

# ML health
curl http://localhost:8087/health

# FX rate (no key needed)
curl "http://localhost:8088/api/external/fx/rate?from=INR&to=USD"

# Eureka registered services
curl -s -H "Accept: application/json" http://localhost:8761/eureka/apps | python3 -m json.tool
```
