# TripForge — Architecture Decision Records

## ADR-001: Microservices Architecture

**Decision:** Split into 10 independent Spring Boot services + 1 Python FastAPI service.

**Rationale:**
- Each service owns a single domain (auth, trip, hotel, route, budget, split, payment)
- Independent deployability and scalability
- Demonstrates Spring Cloud patterns (Eureka, Gateway, Feign) — directly relevant to enterprise Java interviews
- Failure isolation: hotel-service down doesn't break auth or split

**Tradeoff:** Higher operational complexity than a monolith. Acceptable for a portfolio project demonstrating distributed systems knowledge.

---

## ADR-002: Java + Python Split

**Decision:** Java for all domain services, Python for ML service.

**Rationale:**
- Java/Spring Boot is the industry standard for enterprise microservices
- Python/scikit-learn is the standard for ML pipelines
- Demonstrates polyglot architecture — a real-world pattern
- FastAPI is production-grade and well-suited for ML serving

**Tradeoff:** Two build systems, two runtimes. Managed cleanly via Docker multi-stage builds.

---

## ADR-003: external-data-service Abstraction

**Decision:** All external provider calls go through a dedicated `external-data-service`.

**Rationale:**
- Single place to manage provider credentials, timeouts, retries, caching
- Domain services (hotel, route) never know which provider is active
- Fallback chains are centralized and testable
- Provider swap (e.g. Google → OpenTripMap) requires zero changes to domain services
- Redis caching reduces provider API calls and latency

**Tradeoff:** Extra network hop. Acceptable — the abstraction value outweighs the latency cost.

---

## ADR-004: AI Orchestrator is Advisory, Not Source of Truth

**Decision:** Gemini API is used only for explanations, preference parsing, and summaries — never for factual travel data.

**Rationale:**
- LLMs hallucinate prices, coordinates, and availability
- All factual data (hotel prices, coordinates, routes, FX rates) comes from provider APIs
- Gemini enriches the response — it doesn't drive it
- Deterministic fallbacks ensure trip creation never fails due to AI unavailability

**Tradeoff:** More complex orchestration. The safety guarantee is worth it.

---

## ADR-005: Payments are Optional Enrichment

**Decision:** Payment-service is non-blocking — trip creation succeeds even if payment-service is down.

**Rationale:**
- Trip planning is the core product; payments are a value-add
- Prevents payment infrastructure issues from blocking the main user flow
- Matches real-world patterns (e.g. booking.com lets you plan before paying)

**Tradeoff:** Payment tracking may be uninitialized for some trips. Handled gracefully in UI.

---

## ADR-006: Flyway Owns Schema, Hibernate Validates

**Decision:** `ddl-auto: validate` — Flyway creates/migrates schema, Hibernate only validates.

**Rationale:**
- Flyway provides reproducible, versioned, auditable schema changes
- `ddl-auto: update` is dangerous in production (can silently alter or drop columns)
- `validate` catches schema drift early at startup
- All migrations use `IF NOT EXISTS` — safe to re-run

**Tradeoff:** Requires Flyway migration for every schema change. This is the correct production behavior.

---

## ADR-007: Degraded Mode Design

**Decision:** Every service with optional external dependencies runs in degraded mode when keys are absent.

**Rationale:**
- Demo and development should work without billing accounts
- Graceful degradation is a production-grade pattern
- Health endpoints surface degraded state without leaking secrets
- Users see clear warnings in trip responses

**Tradeoff:** More code paths to test. The resilience value is worth it.

---

## ADR-009: Free-First Provider Architecture

**Decision:** OpenTripMap, OpenRouteService, Nominatim, and Frankfurter are the primary providers. Google APIs are optional and disabled by default.

**Rationale:**
- The project must be reproducible by anyone — recruiters, interviewers, contributors — without a billing account
- Google Places and Directions require a GCP billing account even for small usage
- OpenTripMap (free tier), OpenRouteService (2000 req/day free), Nominatim (OSM, completely free), and Frankfurter (ECB rates, completely free) provide sufficient data quality for a portfolio demo
- The provider abstraction from Phase 9B makes this swap transparent to all domain services
- Degraded mode (CSV datasets + heuristic routing) ensures the system works even without any API keys

**Provider priority after this change:**
```
Hotels/Attractions: OpenTripMap → Google Places (optional) → CSV dataset
Routes:             OpenRouteService → Google Directions (optional) → Heuristic
Geocoding:          Nominatim (OSM) → cached → CSV destination fallback
FX:                 Frankfurter → cached → hardcoded approximations
AI:                 Gemini (optional free tier) → deterministic fallbacks
Payments:           Razorpay test mode (optional) → degraded
```

**Tradeoff:** OpenTripMap hotel data is less rich than Google Places (no prices, fewer photos). Acceptable for a portfolio demo — the ML ranking and CSV fallback compensate.

**Decision:** Simple in-memory rate limiter at gateway instead of Redis-backed.

**Rationale:**
- Redis-backed rate limiting adds operational complexity for a demo project
- In-memory is sufficient for single-instance local deployment
- Demonstrates the pattern clearly for interviews

**Production note:** Replace with `spring-cloud-gateway-redis-rate-limiter` for multi-instance.
