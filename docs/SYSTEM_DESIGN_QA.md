# TripForge — System Design Q&A

## Q1: Why microservices instead of a monolith?

**Answer:** The domain naturally decomposes into independent concerns — auth, trip planning, hotel recommendations, routing, budgeting, splitting, and payments. Each service has a single responsibility, its own database schema, and can be scaled independently. For example, hotel-service (which calls the ML service and external providers) is more compute-intensive than split-service (pure math). In a monolith, you'd scale everything together.

More importantly, this demonstrates Spring Cloud patterns — Eureka, Gateway, Feign — that are directly relevant to enterprise Java roles.

---

## Q2: Why not start with a monolith and extract later?

**Answer:** For a portfolio project, the goal is to demonstrate distributed systems knowledge, not to optimize for development speed. A monolith would be faster to build but wouldn't show the same depth. In a real startup, I'd start with a modular monolith and extract services when a specific scaling or team boundary justifies it.

---

## Q3: Why the external-data-service abstraction?

**Answer:** Without it, every domain service would need to know about Google Places, OpenTripMap, Frankfurter, etc. — their auth, rate limits, error formats, and fallback logic. The abstraction centralizes all of that. Domain services call `/api/external/hotels/search` and get a normalized `HotelCandidateDto` — they don't care if it came from Google or a CSV file. Provider swaps require zero changes to domain services.

---

## Q4: Why is the AI orchestrator advisory and not the source of truth?

**Answer:** LLMs hallucinate. If Gemini told us a hotel costs $50/night and it actually costs $200, that's a serious product failure. Gemini is used only for things where being slightly wrong is acceptable — explaining why a hotel was chosen, summarizing a trip, parsing user preferences. All factual data (prices, coordinates, routes, FX rates) comes from provider APIs. This is the correct pattern for LLM integration in production systems.

---

## Q5: How do you handle provider outages?

**Answer:** Three-layer fallback chain. For hotels: Google Places → CSV dataset. For routes: Google Directions → heuristic nearest-neighbor (always succeeds). For FX: Frankfurter → Redis-cached rate → hardcoded approximations. For Gemini: live response → deterministic template. Every response includes `sourceProvider`, `fallbackUsed`, and `degradedMode` flags so the frontend can show appropriate badges.

---

## Q6: How do you keep trip creation resilient?

**Answer:** All downstream calls in trip-service are wrapped in try/catch. If hotel-service fails, `selectedHotel` is null but the trip is still saved. If route-service fails, `itinerary` is empty but the trip is saved. If AI orchestrator fails, `aiSummary` is null. If payment-service fails, `paymentSummary` is null. The trip creation endpoint always returns 201 with whatever data was successfully assembled.

---

## Q7: How do you ensure payment safety?

**Answer:** Three mechanisms. First, Razorpay signature verification — we compute `HMAC-SHA256(orderId + "|" + paymentId, keySecret)` and compare with the signature from the frontend. If it doesn't match, we mark the transaction FAILED and return an error. Second, idempotency keys — if the same order is verified twice, we return the existing result without double-applying the payment. Third, webhook idempotency — we check if the payment is already PAID before processing a webhook event.

---

## Q8: How do you maintain currency consistency?

**Answer:** Currency is a first-class field on the trip entity. When a trip is created with `currency: USD`, that currency flows through: budget-service converts all INR base rates to USD via Frankfurter FX, split-service stores amounts in USD, payment-service creates Razorpay orders in USD. The exchange rate used is stored in the budget breakdown for auditability. If FX is unavailable, we fall back to INR with a warning.

---

## Q9: Why Flyway + validate instead of ddl-auto: update?

**Answer:** `ddl-auto: update` is dangerous in production — it can silently add columns, and in some cases alter or drop them. Flyway gives you versioned, auditable, reproducible schema changes. `validate` mode means Hibernate checks that the schema matches the entities at startup — if there's a mismatch, the service fails fast with a clear error rather than silently corrupting data. All migrations use `IF NOT EXISTS` so they're safe to re-run.

---

## Q10: What would you do next in production?

**Answer:** Several things. First, replace in-memory rate limiting with Redis-backed `RequestRateLimiter`. Second, add distributed tracing with Zipkin — correlation IDs are already propagated, just need the tracing backend. Third, replace JWT in localStorage with httpOnly cookies. Fourth, add a real hotel data provider (Booking.com or Amadeus API) to replace the CSV dataset. Fifth, add Kafka for async event processing — currently all Feign calls are synchronous, which means trip creation latency is the sum of all downstream calls. With Kafka, hotel/route/budget/split could process in parallel.
