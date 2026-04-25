# TripForge — Resume & Viva Pack

---

## Resume Bullets

**Option A (technical focus):**
> Designed and built TripForge, a 10-service microservices application using Java 17 + Spring Boot 3.2, Python FastAPI, and React 18, featuring JWT authentication, Eureka service discovery, OpenFeign inter-service communication, and a hybrid ML hotel ranking pipeline (GradientBoostingRegressor, R²=0.90) deployed via Docker Compose.

**Option B (ML focus):**
> Implemented a hybrid ML recommendation pipeline for hotel ranking combining a trained GradientBoostingRegressor (23,625 samples, R²=0.90, MAE=0.024) with rule-based scoring and feedback-aware re-ranking across 4 user preference signals (price, rating, distance, premium), integrated into a Spring Boot microservices backend via FastAPI.

**Option C (full-stack focus):**
> Built a full-stack trip planning application with a React 18 + Tailwind CSS frontend, 8 Spring Boot microservices, a Python FastAPI ML service, PostgreSQL with 6 logical schemas, Flyway migrations, and Docker Compose orchestration — demonstrating end-to-end system design from auth to AI-powered recommendations.

---

## 60-Second Explanation

> TripForge is an AI-powered trip planner I built as a full-stack microservices project. A user enters a destination, budget, dates, and interests. The system generates a day-wise itinerary, recommends hotels using a trained machine learning model, calculates a budget breakdown, and splits expenses per person.
>
> The backend is 8 Spring Boot microservices — auth, trip, hotel, route, budget, split, a gateway, and a Eureka registry. The ML component is a separate Python FastAPI service with a GradientBoosting model that ranks hotels using 16 features. The frontend is React with Tailwind CSS.
>
> Everything runs locally with one `docker-compose up` command. The ML models are trained with scikit-learn and achieve R² of 0.90 on the hotel ranking task.

---

## 3-Minute Explanation

> TripForge is a production-grade microservices application I designed and built end-to-end. Let me walk you through the key parts.
>
> **The problem:** Planning a trip involves multiple decisions — where to stay, what to do each day, how much it costs, and how to split expenses. I wanted to automate this with a real ML pipeline, not fake AI.
>
> **The architecture:** I used a microservices pattern with 8 Java Spring Boot services, each owning its own database schema. A Spring Cloud Gateway handles JWT validation and routes requests to the right service via Eureka service discovery. Services communicate using OpenFeign clients.
>
> **The ML part:** The hotel ranking uses a hybrid pipeline. First, rule-based scoring computes interpretable sub-scores — price fit, rating, distance, category match. Then a trained GradientBoostingRegressor predicts a relevance score. The final score is 65% ML, 35% rule-based. I also built feedback-aware re-ranking — when a user says "I want something cheaper," the system adjusts the price weight to 0.45 and re-ranks. The classifier achieves R² of 0.90 on 23,625 training samples.
>
> **The frontend:** React 18 with Tailwind CSS. I built a clean dark-themed UI with protected routes, an auth context, and resilient API adapters that normalize backend response shapes so components never crash on null fields.
>
> **The infrastructure:** Everything runs with `docker-compose up`. PostgreSQL has 6 logical schemas — one per service. Flyway manages migrations. The ML service is Python FastAPI, separate from the Java stack, called directly by hotel-service.
>
> The whole system demonstrates separation of concerns, graceful degradation, and a real ML pipeline — not a demo with hardcoded responses.

---

## Viva Q&A

### Why microservices?

> The domain naturally decomposes into independent concerns — authentication, trip planning, hotel recommendations, routing, budgeting, and splitting. Each service has a single responsibility, its own database schema, and can be developed, deployed, and scaled independently. For a trip planner, hotel recommendations and itinerary generation are the most compute-intensive parts — in production, you'd scale those independently.
>
> I also wanted to demonstrate Spring Cloud patterns — Eureka, Gateway, OpenFeign — which are industry-standard for Java microservices.

---

### Why Java for backend and Python for ML?

> Java with Spring Boot is the industry standard for enterprise microservices. It gives you strong typing, mature libraries, and production-grade tooling out of the box — Eureka, Feign, Flyway, Actuator.
>
> Python is the right choice for ML. scikit-learn, pandas, and joblib are far more mature than Java ML libraries for this use case. Keeping the ML service separate also means you can retrain models, swap algorithms, or scale the ML component independently without touching the Java services.
>
> The integration is clean — hotel-service calls ml-service via HTTP, and if ml-service is down, hotel-service falls back to rule-based scoring. The Java services never need to know about Python internals.

---

### What is the ML part exactly?

> There are two models. The hotel ranker is a GradientBoostingRegressor trained on 23,625 synthetic samples. Each sample represents a hotel-trip context pair with 16 features: budget per day, duration, travelers, hotel price, rating, distance, destination encoding, preference encoding, category encoding, popularity, and five computed sub-scores. The label is a synthetic relevance score computed from a weighted formula. The model achieves R² of 0.90 and MAE of 0.024 on the test set.
>
> The trip style classifier is a RandomForestClassifier trained on 2,400 samples across 6 classes — Budget, Family, Nightlife, Adventure, Luxury, Cultural. It achieves 99.2% cross-validation accuracy.
>
> The hybrid pipeline combines ML predictions with rule-based scores. This is important because pure ML can be a black box — the rule-based component provides interpretability and a fallback if the model is unavailable.

---

### What challenges did you face?

> Three main ones.
>
> First, the API response wrapper. The backend returns `ApiResponse<T>` for every endpoint. Early on, frontend components were reading `res.data` when they needed `res.data.data`. I fixed this by standardizing the API layer — all API modules now unwrap the payload internally, so components receive typed payloads directly.
>
> Second, service startup ordering. Spring Boot services register with Eureka asynchronously. If trip-service starts before hotel-service is registered, the first Feign call fails. I solved this with Docker Compose `depends_on` health checks and try/catch on all Feign calls — partial failures return partial responses rather than crashing.
>
> Third, the ML training pipeline. The hotel dataset has only 45 hotels, which isn't enough to train a meaningful model. I generated 23,625 synthetic training samples by simulating different trip contexts for each hotel. The synthetic labels are computed from a realistic weighted formula, so the model learns real patterns rather than noise.

---

### What would you improve in V2?

> Several things.
>
> **Real data:** Replace the curated CSV datasets with a real hotel API like Booking.com or MakeMyTrip. The ML model would then train on real booking data with actual user feedback signals.
>
> **Redis caching:** Hotel recommendations for the same destination and budget don't change frequently. Caching them in Redis would reduce ML service calls significantly.
>
> **Async orchestration:** The trip creation flow currently makes 4 synchronous Feign calls sequentially. Using Spring WebFlux or a message queue like Kafka would allow parallel execution and reduce latency.
>
> **Better ML:** The current model uses synthetic training data. With real user feedback — which hotels users actually booked, which they rejected — you could build a proper collaborative filtering or learning-to-rank model.
>
> **CI/CD:** Add GitHub Actions for automated testing, Docker image building, and deployment to a cloud provider.
>
> **Kubernetes:** Replace Docker Compose with Kubernetes manifests for production-grade orchestration with auto-scaling and rolling deployments.
