# TripForge — Final Risk List

Top 5 things most likely to fail during a live demo and how to recover in 30 seconds.

---

## Risk 1: ML Models Missing

**Symptom:**
- `make health` shows ML Service as DEGRADED
- Hotel recommendations still work but `modelUsed: "rule_based"` instead of `"hybrid_gbr"`
- ML health endpoint shows `"hotel_ranker_loaded": false`

**Likely Cause:**
- `bash scripts/train-ml-models.sh` was not run before `docker-compose up --build`
- The `saved_models/` directory was empty when the Docker image was built

**30-Second Recovery:**
```bash
# Stop the stack
make down

# Train models (takes ~30 seconds)
make train

# Rebuild and restart
make rebuild
```

**Prevention:** Always run `make train` before the first `make up`. The `scripts/verify-setup.sh` checks for this.

---

## Risk 2: Services Not Registered in Eureka

**Symptom:**
- Eureka dashboard shows fewer than 7 services
- Trip creation returns 500 or empty results
- `make health` shows some services as UNREACHABLE

**Likely Cause:**
- Stack was checked too early (services need 2-3 minutes to register)
- One service failed to start (check logs)

**30-Second Recovery:**
```bash
# Check which services are down
make ps

# View logs for a specific service
make logs-trip

# If a service crashed, restart it
docker compose restart trip-service
```

**Prevention:** Wait the full 3-4 minutes after `make up` before running `make health`. The print-urls script says this explicitly.

---

## Risk 3: Frontend Shows Blank Page or API Errors

**Symptom:**
- http://localhost:3000 loads but shows blank content
- Browser console shows `net::ERR_CONNECTION_REFUSED` or CORS errors
- Login/register fails with network error

**Likely Cause:**
- `VITE_API_BASE_URL` was not set correctly at build time
- API Gateway is not yet healthy when frontend tries to connect
- CORS configuration mismatch

**30-Second Recovery:**
```bash
# Check gateway health
curl http://localhost:8080/actuator/health

# If gateway is down, restart it
docker compose restart api-gateway

# If VITE_API_BASE_URL is wrong, rebuild frontend
docker compose build frontend
docker compose up -d frontend
```

**Prevention:** Ensure `frontend/.env` has `VITE_API_BASE_URL=http://localhost:8080` before building.

---

## Risk 4: Database Migration Fails on Startup

**Symptom:**
- A service (auth, trip, hotel, etc.) shows as UNHEALTHY
- Service logs show: `FlywayException` or `relation does not exist`
- Service keeps restarting

**Likely Cause:**
- `DB_USERNAME` or `DB_PASSWORD` in `.env` doesn't match the postgres container
- Flyway `validate` mode detects schema drift after a code change
- PostgreSQL container not fully ready when service starts

**30-Second Recovery:**
```bash
# Check the failing service logs
make logs-auth   # or logs-trip, logs-hotel, etc.

# If credentials mismatch — fix .env and restart
docker compose restart auth-service

# If schema drift — temporarily set JPA_DDL_AUTO=update in .env
# Then restart the service, then set back to validate
```

**Prevention:** Use `make reset` for a clean slate. Ensure `.env` credentials match `.env.example` defaults.

---

## Risk 5: Hotel Change Returns Empty Alternatives

**Symptom:**
- Hotel change modal shows "No alternatives found"
- `POST /api/hotels/change` returns empty array `[]`

**Likely Cause:**
- The pre-filter for the selected reason is too strict (e.g., CHEAPER when current hotel is already the cheapest)
- ml-service is in DEGRADED mode and fallback scoring returns no results
- Current hotel is the only one in that destination/category

**30-Second Recovery:**
```bash
# Try a different reason
# In the modal: switch from CHEAPER to BETTER_RATING or CLOSER

# Or test directly
curl -s -X POST http://localhost:8080/api/hotels/change \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"tripId":1,"currentHotelId":3,"reason":"BETTER_RATING","destination":"Goa","budget":50000,"durationDays":4,"travelers":2}'
```

**Prevention:** During demo, use STANDARD hotel preference for the initial trip. This gives the most room to change — can go cheaper (BUDGET) or premium (LUXURY). Avoid starting with BUDGET preference as there are fewer cheaper alternatives.

---

## Quick Reference: Recovery Commands

```bash
make ps              # See which containers are running
make logs            # Tail all logs
make logs-auth       # Tail specific service logs
make health          # Check all health endpoints
docker compose restart <service>   # Restart one service
make rebuild         # Full rebuild (takes 3-5 min)
make reset           # Nuclear option — wipe everything
```
