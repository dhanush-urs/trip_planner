# TripForge — Live Demo Script

## Pre-Demo Checklist (5 minutes before)

- [ ] `docker-compose up --build` completed
- [ ] `bash scripts/check-health.sh` shows all green
- [ ] Browser open at http://localhost:3000
- [ ] Eureka dashboard open at http://localhost:8761 (second tab)
- [ ] ML docs open at http://localhost:8087/docs (third tab)
- [ ] Terminal ready for log tailing if needed

---

## Demo Flow (~8 minutes)

---

### Step 1: Show Architecture (30 seconds)

Open Eureka dashboard at http://localhost:8761.

**Say:**
> "This is the Eureka service registry. You can see all 7 Java microservices have registered — auth, trip, hotel, route, budget, split, and the gateway. Each service is independently deployable and communicates through the gateway."

Point to the registered instances list.

---

### Step 2: Register (45 seconds)

Navigate to http://localhost:3000/register.

**Input values:**
- First Name: `Arjun`
- Last Name: `Sharma`
- Email: `arjun@tripforge.com`
- Password: `Demo@12345`
- Confirm Password: `Demo@12345`

Click **Create Account**.

**Say:**
> "Registration calls auth-service through the gateway. The password is hashed with BCrypt, and a JWT token is returned. The token is stored in localStorage and attached to every subsequent request."

Expected: Redirect to dashboard, navbar shows "Arjun Sharma".

---

### Step 3: Dashboard (20 seconds)

**Say:**
> "The dashboard shows recent trips and quick actions. Since this is a new account, there are no trips yet. Let's create one."

Click **Plan New Trip**.

---

### Step 4: Create Trip (90 seconds)

**Input values:**
- Destination: `Goa`
- Start Date: 2 weeks from today
- End Date: 4 days after start date
- Budget: `50000`
- Travelers: `2`
- Interests: click `Beaches` and `Food`
- Hotel Preference: `Standard`

**Say:**
> "The form validates all inputs. The duration is calculated automatically — 4 days. The interests drive the itinerary generation, and the hotel preference filters the ML ranking."

Click **Generate My Trip Plan**.

**Say while loading:**
> "Behind the scenes, trip-service is orchestrating 4 parallel calls — hotel-service for recommendations, route-service for the itinerary, budget-service for cost estimation, and split-service for the expense split. Hotel-service calls the ML service to rank hotels using a trained GradientBoosting model."

Expected: Trip result page renders in ~2-3 seconds.

---

### Step 5: Trip Result Page (90 seconds)

**Point to each section:**

**Hotel card:**
> "The top hotel is ranked by our hybrid ML pipeline — 65% ML score, 35% rule-based score. You can see the rating, distance from city center, amenities, and a relevance score."

**Itinerary:**
> "Route-service generated a 4-day itinerary. Attractions are grouped by geographic cluster to minimize travel time, and matched to the user's interests — beaches and food. Each day has a theme and time slots."

**Budget breakdown:**
> "Budget-service calculated costs across hotel, food, transport, attractions, and a 5% miscellaneous buffer. The total is ₹39,900 against a ₹50,000 budget — ₹10,100 remaining."

**Split card:**
> "Split-service divided the total equally — ₹19,950 per person for 2 travelers."

---

### Step 6: Change Hotel (60 seconds)

Click **Change Hotel**.

**Say:**
> "The user isn't happy with the current hotel. They want something cheaper."

Select **Cheaper** reason.

Click **Find Alternatives**.

**Say:**
> "Hotel-service calls the ML service again with adjusted weights — price fit gets a weight of 0.45 instead of 0.25. The re-ranking returns hotels sorted by affordability."

Select the cheapest alternative.

**Say:**
> "Trip-service replans the trip with the new hotel. Budget and split are recalculated automatically."

Expected: Modal closes, hotel card updates, budget numbers change.

---

### Step 7: Trip History (30 seconds)

Click **My Trips** in the navbar.

**Say:**
> "All trips are saved. The history page shows destination, dates, budget, and status. Clicking any trip loads the full details."

Click the Goa trip card.

**Say:**
> "The details page fetches the full trip by ID — same data, same layout."

---

### Step 8: ML Service (30 seconds)

Open http://localhost:8087/docs.

**Say:**
> "The ML service is a separate Python FastAPI microservice. It exposes three endpoints — hotel ranking, alternative re-ranking, and trip style classification. The hotel ranker is a GradientBoostingRegressor trained on 23,625 samples with R² of 0.90."

Show the `/health` endpoint response.

---

## Recommended Demo Input Values

| Field | Value | Why |
|---|---|---|
| Destination | Goa | Most hotels and attractions in dataset |
| Duration | 4 days | Shows multi-day itinerary clearly |
| Budget | ₹50,000 | Fits STANDARD hotels, shows remaining budget |
| Travelers | 2 | Clean split numbers |
| Interests | Beaches + Food | Goa has strong data for both |
| Hotel Preference | Standard | Shows mid-range options, good for change demo |

---

## Fallback Plans

### If trip creation returns empty itinerary
**Say:** "Route-service is still warming up. Let me show you a previously created trip."
Navigate to `/trip/history` and open an existing trip.

### If hotel change returns no alternatives
**Say:** "Let me try a different reason." Switch from CHEAPER to BETTER_RATING.

### If ML service is in DEGRADED mode
**Say:** "The ML service is running in rule-based fallback mode — hotels are still ranked, just using the interpretable scoring formula instead of the trained model. The system is designed to degrade gracefully."

### If any service is down
Open Eureka dashboard and show which services are registered.
**Say:** "This is the service registry. In a production system, we'd have multiple instances of each service for high availability."

### If frontend shows blank page
Check `VITE_API_BASE_URL` — rebuild frontend with correct gateway URL.

---

## What NOT to Demo

- Do not demo with a destination outside Goa/Mysore/Bangalore/Ooty/Manali — no data exists
- Do not try to register the same email twice in the same demo session
- Do not demo on a machine with less than 4GB RAM allocated to Docker
