# TripForge — Integration Validation Checklist

Run through this checklist after `docker-compose up --build` to validate the full stack.

---

## Infrastructure

- [ ] PostgreSQL container is healthy (`docker-compose ps postgres`)
- [ ] Eureka dashboard is reachable at http://localhost:8761
- [ ] All 7 Java services appear in Eureka registry (check dashboard)
- [ ] API Gateway is healthy at http://localhost:8080/actuator/health
- [ ] ML Service is healthy at http://localhost:8087/health
- [ ] Frontend loads at http://localhost:3000

---

## Auth Flow

- [ ] **Register** — `POST /api/auth/register` returns 201 with JWT token
  ```bash
  curl -X POST http://localhost:8080/api/auth/register \
    -H "Content-Type: application/json" \
    -d '{"email":"test@tripforge.com","password":"Test@12345","firstName":"Test","lastName":"User"}'
  ```
- [ ] **Login** — `POST /api/auth/login` returns 200 with JWT token
  ```bash
  curl -X POST http://localhost:8080/api/auth/login \
    -H "Content-Type: application/json" \
    -d '{"email":"test@tripforge.com","password":"Test@12345"}'
  ```
- [ ] **Profile** — `GET /api/users/profile` returns user data with valid JWT
- [ ] **Preferences** — `PUT /api/users/preferences` updates and returns preferences
- [ ] **Invalid login** returns 401 with error message
- [ ] **Missing JWT** on protected route returns 401

---

## Frontend Auth

- [ ] Register page submits and redirects to dashboard
- [ ] Login page submits and redirects to dashboard
- [ ] Navbar shows user name after login
- [ ] Sign out clears token and redirects to login
- [ ] Refreshing page restores auth state (token in localStorage)
- [ ] Accessing `/dashboard` without token redirects to `/login`

---

## Trip Creation (End-to-End)

- [ ] Create Trip form loads at `/trip/create`
- [ ] All destinations appear in dropdown (Goa, Mysore, Bangalore, Ooty, Manali)
- [ ] Interest chips toggle correctly
- [ ] Hotel preference selection works
- [ ] Date validation prevents end date before start date
- [ ] Budget validation rejects values below ₹1000
- [ ] **Submit** — `POST /api/trip/create` returns full trip response
  ```bash
  curl -X POST http://localhost:8080/api/trip/create \
    -H "Authorization: Bearer <TOKEN>" \
    -H "Content-Type: application/json" \
    -d '{
      "destination":"Goa","startDate":"2025-02-01","endDate":"2025-02-05",
      "totalBudget":50000,"travelers":2,
      "interests":["beaches","food"],"hotelPreference":"STANDARD"
    }'
  ```
- [ ] Trip result page renders after creation
- [ ] Trip summary card shows destination, dates, budget, travelers

---

## Trip Result Page

- [ ] Hotel recommendation card renders with name, price, rating, distance
- [ ] "Change Hotel" button opens the hotel change modal
- [ ] Day-wise itinerary renders (at least 1 day with attractions)
- [ ] Budget breakdown card shows all cost categories
- [ ] Split expense card shows per-person amount
- [ ] Over-budget warning appears when estimated > budget

---

## Hotel Change Flow

- [ ] Hotel change modal opens on button click
- [ ] All 4 reasons are selectable (CHEAPER, BETTER_RATING, CLOSER, PREMIUM)
- [ ] "Find Alternatives" calls `POST /api/hotels/change`
- [ ] Alternative hotels render in modal
- [ ] Selecting a hotel calls `PUT /api/trip/replan`
- [ ] Modal closes and selected hotel updates on result page

---

## Trip History

- [ ] `/trip/history` loads and shows past trips
- [ ] Each trip card shows destination, dates, budget, status
- [ ] Clicking a trip navigates to `/trip/:id`
- [ ] Trip details page fetches and renders full trip data

---

## ML Service

- [ ] `GET http://localhost:8087/health` returns `{"status":"UP"}`
- [ ] `hotel_ranker_loaded: true` in health response
- [ ] `trip_classifier_loaded: true` in health response
- [ ] `POST /api/ml/hotel-rank` returns ranked hotels with scores
- [ ] `POST /api/ml/recommend-alternative-hotel` returns alternatives
- [ ] `POST /api/ml/classify-trip-style` returns a valid trip style

---

## Data Validation

- [ ] Hotel service loaded hotels from CSV (check logs: "Loaded X hotels")
- [ ] Route service loaded attractions from CSV (check logs: "Loaded X attractions")
- [ ] All 6 DB schemas created: auth, trip, hotel, route, budget, split_schema
- [ ] Flyway migrations ran successfully (check service logs)

---

## API Contract Notes

See `API_CONTRACT.md` for detailed field mapping between frontend and backend.
