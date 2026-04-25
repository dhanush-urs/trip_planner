# TripForge — 2-Minute Demo Script

Optimized for college evaluation. Every second counts.

---

## Setup (before evaluator arrives)

1. `make up` already running, all services healthy
2. Browser open at http://localhost:3000 (login page visible)
3. Eureka dashboard open in Tab 2: http://localhost:8761
4. ML docs open in Tab 3: http://localhost:8087/docs
5. Terminal ready with `make health` output visible

---

## The Script

---

### [0:00 – 0:15] Open with Architecture

Switch to Tab 2 (Eureka).

**Say:**
> "This is TripForge — a microservices trip planner with an AI recommendation engine. You can see 7 independent services registered in Eureka — auth, trip, hotel, route, budget, split, and the gateway. Each service has its own database schema and can be deployed independently."

**Emphasize:** The service count in Eureka. This proves the microservices architecture is real and running.

---

### [0:15 – 0:35] Register & Login

Switch to Tab 1 (frontend). Navigate to Register.

Fill in quickly:
- First: `Arjun` · Last: `Sharma` · Email: `arjun@demo.com` · Password: `Demo@12345`

Click **Create Account**.

**Say:**
> "Registration calls auth-service through the API gateway. The password is BCrypt-hashed, and a JWT token is returned and stored. The gateway validates this token on every subsequent request."

**Emphasize:** The redirect to dashboard and the name in the navbar.

---

### [0:35 – 1:00] Create Trip

Click **Plan New Trip**.

Fill in quickly:
- Destination: `Goa`
- Dates: next month, 4 days
- Budget: `50000`
- Travelers: `2`
- Interests: click `Beaches` + `Food`
- Hotel: `Standard`

Click **Generate My Trip Plan**.

**Say while loading:**
> "Trip-service is now orchestrating 4 parallel calls — hotel recommendations, itinerary generation, budget calculation, and expense split. Hotel-service calls our Python ML service to rank hotels using a trained GradientBoosting model."

---

### [1:00 – 1:30] Trip Result

**Point to each section quickly:**

**Hotel card:**
> "Top hotel ranked by our hybrid ML pipeline — 65% model score, 35% rule-based. R-squared of 0.90 on the test set."

**Itinerary:**
> "Route-service generated a 4-day itinerary. Attractions are matched to the user's interests and grouped geographically to minimize travel."

**Budget:**
> "Budget-service estimated costs across hotel, food, transport, and attractions. ₹39,900 out of ₹50,000 — ₹10,100 remaining."

**Split:**
> "Split-service: ₹19,950 per person."

---

### [1:30 – 1:50] Change Hotel

Click **Change Hotel**.

Select **Cheaper**. Click **Find Alternatives**.

**Say:**
> "The ML service re-ranks hotels with adjusted weights — price fit gets 45% weight instead of 25%. These are cheaper alternatives sorted by the new scoring."

Select the first alternative.

**Say:**
> "Trip replanned. Budget recalculated automatically."

---

### [1:50 – 2:00] Close Strong

Switch to Tab 3 (ML docs).

**Say:**
> "The ML service is a separate Python FastAPI microservice — completely independent from the Java stack. It exposes hotel ranking, feedback re-ranking, and trip style classification. The whole system runs with one command: `make up`."

**Final line:**
> "TripForge demonstrates microservices architecture, real ML integration, JWT security, and a full React frontend — all containerized with Docker Compose."

---

## What to Skip if Time is Short

If you only have 90 seconds, skip:
- The hotel change flow (go straight from trip result to close)
- The ML docs tab
- Explaining the split card

If you only have 60 seconds, show only:
- Eureka dashboard (10s)
- Register + dashboard (15s)
- Create trip + result page (35s)

---

## Key Phrases to Use

| Phrase | When to use |
|---|---|
| "7 independent microservices" | When showing Eureka |
| "JWT validated at the gateway" | When logging in |
| "4 parallel service calls" | During trip creation loading |
| "Hybrid ML pipeline — 65% model, 35% rule-based" | When showing hotel card |
| "R-squared of 0.90" | When mentioning ML |
| "Feedback-aware re-ranking" | During hotel change |
| "One command: make up" | At the end |

---

## What Evaluators Care About Most

1. **Does it actually run?** → Show `make ps` or Eureka with all services
2. **Is the ML real?** → Show R² metric, model loaded status, reason tags
3. **Is it microservices?** → Show Eureka with 7 services
4. **Is the frontend clean?** → The dark theme and responsive layout speak for themselves
5. **Can you explain it?** → Use the RESUME_VIVA.md answers
