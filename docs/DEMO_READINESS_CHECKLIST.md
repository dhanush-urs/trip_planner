# TripForge — Demo Readiness Checklist

Run through this checklist before any demo, viva, or presentation.

---

## Pre-Demo Setup

- [ ] Docker Desktop is running with at least 4GB RAM allocated
- [ ] `cp .env.example .env` done
- [ ] `cp frontend/.env.example frontend/.env` done
- [ ] ML models trained: `bash scripts/train-ml-models.sh`
- [ ] Setup verified: `bash scripts/verify-setup.sh` — all green
- [ ] Stack started: `docker-compose up --build` (or `make up`)
- [ ] Waited ~3-4 minutes for full startup

---

## Infrastructure Health

- [ ] `bash scripts/check-health.sh` — all services UP
- [ ] Eureka dashboard at http://localhost:8761 shows 7 registered services:
  - [ ] AUTH-SERVICE
  - [ ] TRIP-SERVICE
  - [ ] HOTEL-SERVICE
  - [ ] ROUTE-SERVICE
  - [ ] BUDGET-SERVICE
  - [ ] SPLIT-SERVICE
  - [ ] API-GATEWAY
- [ ] ML service health: `curl http://localhost:8087/health` → `"status":"UP"`, both models loaded
- [ ] Frontend loads: http://localhost:3000 renders login page

---

## Auth Flow

- [ ] Register with new email → redirects to dashboard
- [ ] Navbar shows registered user's name
- [ ] Sign out → redirects to login
- [ ] Login with same credentials → redirects to dashboard
- [ ] Refresh page → stays logged in

---

## Trip Creation

- [ ] Navigate to Create Trip page
- [ ] Select destination: Goa
- [ ] Set dates (4-5 days in the future)
- [ ] Set budget: ₹50,000
- [ ] Set travelers: 2
- [ ] Select interests: Beaches + Food
- [ ] Select hotel preference: Standard
- [ ] Submit → loading spinner shows
- [ ] Trip result page renders with:
  - [ ] Trip summary card (destination, dates, budget, travelers)
  - [ ] Hotel recommendation card (name, price, rating, distance, amenities)
  - [ ] Day-wise itinerary (at least 3 days with attractions)
  - [ ] Budget breakdown (all 5 cost categories)
  - [ ] Split expense card (per-person amount)

---

## Hotel Change Flow

- [ ] "Change Hotel" button visible on trip result page
- [ ] Click opens modal with current hotel name shown
- [ ] All 4 reasons visible: Cheaper, Better Rating, Closer, More Premium
- [ ] Select "Cheaper" → click "Find Alternatives"
- [ ] Loading state shows during search
- [ ] Alternative hotels render in modal
- [ ] Click "Select This Hotel" on an alternative
- [ ] Modal closes
- [ ] Hotel card on result page updates to new hotel
- [ ] Budget numbers update (if hotel price changed)

---

## Trip History & Details

- [ ] Navigate to "My Trips"
- [ ] At least 1 trip card visible
- [ ] Card shows: destination, dates, budget, status badge
- [ ] Click trip card → navigates to `/trip/{id}`
- [ ] Details page renders full trip (hotel, itinerary, budget, split)
- [ ] "Change Hotel" button works on details page too

---

## ML Service

- [ ] http://localhost:8087/docs loads FastAPI Swagger UI
- [ ] `/health` endpoint shows both models loaded
- [ ] `/api/ml/hotel-rank` returns ranked hotels with scores and reason tags
- [ ] `/api/ml/classify-trip-style` returns a valid trip style with confidence

---

## Frontend UI Sanity

- [ ] Dark navy theme renders correctly
- [ ] No console errors in browser DevTools
- [ ] Responsive layout works on laptop screen
- [ ] Loading spinners show during API calls
- [ ] Error messages show on invalid form input
- [ ] Empty state shows on trip history for new users

---

## Screenshot Capture List

Take these screenshots for GitHub README and portfolio:

- [ ] Login page
- [ ] Register page
- [ ] Dashboard (with recent trips)
- [ ] Create Trip form (filled in)
- [ ] Trip Result page (full view)
- [ ] Hotel Change Modal (with alternatives)
- [ ] Budget Breakdown card (close-up)
- [ ] Trip History page
- [ ] Trip Details page
- [ ] Eureka Dashboard (showing all services)
- [ ] ML Service Swagger UI

---

## Common Last-Minute Fixes

| Problem | Quick Fix |
|---|---|
| Services not in Eureka | `docker-compose restart <service-name>` |
| ML models missing | `bash scripts/train-ml-models.sh` then rebuild |
| Frontend shows old build | `docker-compose build frontend && docker-compose up frontend` |
| JWT errors | Check `.env` has correct `JWT_SECRET` |
| Empty trip history | Wait 2 min, then refresh |
| DB migration failed | `docker-compose logs <service>` to see Flyway error |
