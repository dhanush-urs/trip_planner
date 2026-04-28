# TripForge — Resume Bullets & Interview Pitch

## Resume Bullets

### Standard (most roles)
> Built TripForge, a distributed AI travel planning platform with 11 Spring Boot microservices, Python FastAPI ML service, and React frontend — featuring Gemini-powered itinerary reasoning, hybrid ML hotel ranking (GBR, R²=0.90), Razorpay payment integration, multi-currency budgeting, and resilient provider fallback architecture deployed via Docker Compose.

### ML/AI Focus
> Designed and implemented a hybrid hotel recommendation pipeline combining a trained GradientBoostingRegressor (23,625 samples, R²=0.90, MAE=0.024) with rule-based scoring and feedback-aware re-ranking, integrated into a Java microservices backend via Python FastAPI — achieving deterministic fallback behavior when ML service is unavailable.

### Backend/Distributed Systems Focus
> Architected TripForge, a 11-service distributed system using Spring Cloud Gateway (JWT auth, rate limiting, correlation ID propagation), Netflix Eureka, OpenFeign, Flyway migrations, Redis caching, and a provider abstraction layer supporting live/fallback data strategies across hotel, route, FX, and AI providers.

### Full-Stack Focus
> Delivered TripForge end-to-end: Java Spring Boot microservices backend with Razorpay payment gateway, Python ML ranking service, and React 18 + Tailwind CSS frontend with multi-currency support, equal/custom expense splitting, Razorpay checkout integration, and AI-generated trip explanations via Gemini API.

### ATS-Optimized (concise)
> TripForge: 11-service microservices platform (Java/Spring Boot, Python/FastAPI, React) with ML hotel ranking, Gemini AI integration, Razorpay payments, multi-currency budgeting, Docker Compose deployment, and resilient provider fallback architecture.

---

## Interview Pitch

### 30-Second Version
> TripForge is a distributed AI travel planning platform I built end-to-end. It has 11 microservices — Java Spring Boot for the domain services, Python FastAPI for ML ranking, and React for the frontend. Users plan trips, get AI-explained hotel recommendations ranked by a trained GradientBoosting model, see multi-currency budgets, split expenses equally or by custom amounts, and pay via Razorpay. The whole system runs with one `make up` command and degrades gracefully when optional API keys are absent.

### 60-Second Version
> TripForge is a production-style distributed travel planning platform I designed and built from scratch. The architecture has 11 services: a Spring Cloud Gateway for JWT auth and rate limiting, Eureka for service discovery, 7 domain services for auth/trip/hotel/route/budget/split/payment, a Python FastAPI ML service for hotel ranking, and an AI orchestrator for Gemini integration.
>
> The hotel ranking uses a hybrid pipeline — 65% GradientBoosting model trained on 23,000 samples, 35% rule-based scoring — with feedback-aware re-ranking when users reject hotels. All external providers (Google Places, Directions, Frankfurter FX, Gemini) have fallback chains so trip creation never fails.
>
> The system supports multi-currency budgeting with live FX rates, equal and custom expense splitting with largest-remainder rounding, and Razorpay payment integration with HMAC-SHA256 webhook verification. Everything runs locally with Docker Compose and degrades gracefully without API keys.

### 3-Minute Version
> TripForge is a distributed AI travel planning and group expense platform I built end-to-end as a portfolio project. Let me walk you through the key technical decisions.
>
> **Architecture:** 11 microservices — Spring Cloud Gateway handles JWT validation, rate limiting, and correlation ID propagation. Netflix Eureka handles service discovery. OpenFeign handles inter-service communication with correlation ID propagation. All schema changes go through Flyway migrations with Hibernate in validate mode.
>
> **External data:** I built an `external-data-service` abstraction that wraps Google Places, OpenTripMap, Google Directions, and Frankfurter FX. This means domain services never know which provider is active. If Google Places is down, it falls back to OpenTripMap, then to the CSV dataset. Trip creation always succeeds.
>
> **ML pipeline:** The hotel ranker is a GradientBoostingRegressor trained on 23,625 synthetic samples with R² of 0.90. The final score is 65% ML + 35% rule-based. When users reject a hotel, the feedback reason (CHEAPER, BETTER_RATING, CLOSER, PREMIUM) adjusts the scoring weights for re-ranking.
>
> **AI orchestration:** Gemini is used only for explanations and preference parsing — never for factual data. Every Gemini call has a deterministic fallback so trip creation never fails due to AI unavailability.
>
> **Payments:** Razorpay integration with HMAC-SHA256 signature verification, idempotent webhook processing, and per-participant payment tracking. The payment service is non-blocking — trip planning works even if payment-service is down.
>
> **Currency:** Multi-currency support with live FX rates from Frankfurter API, Redis caching, and largest-remainder rounding for fair expense splits.
>
> The whole system runs with `make up` and degrades gracefully without any API keys configured.
