# TripForge — Test Plan

---

## 1. Service Health Smoke Tests

Run after `docker-compose up`:

```bash
bash scripts/check-health.sh
```

| Check | Expected | Pass Criteria |
|---|---|---|
| PostgreSQL | pg_isready returns 0 | Exit code 0 |
| discovery-server | `{"status":"UP"}` | HTTP 200 |
| api-gateway | `{"status":"UP"}` | HTTP 200 |
| auth-service | `{"status":"UP"}` | HTTP 200 |
| trip-service | `{"status":"UP"}` | HTTP 200 |
| hotel-service | `{"status":"UP"}` | HTTP 200 |
| route-service | `{"status":"UP"}` | HTTP 200 |
| budget-service | `{"status":"UP"}` | HTTP 200 |
| split-service | `{"status":"UP"}` | HTTP 200 |
| ml-service | `{"status":"UP","models":{"hotel_ranker":{"loaded":true}}}` | HTTP 200 |
| frontend | HTTP 200 | Status 200 |
| Eureka registry | 7+ services registered | Count ≥ 7 |

---

## 2. Auth Service Tests

### 2.1 Register — Happy Path
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"test@tripforge.com","password":"Test@12345","firstName":"Test","lastName":"User"}'
```
**Expected:** HTTP 201, response contains `token`, `userId`, `email`

### 2.2 Register — Duplicate Email
Register same email twice.
**Expected:** HTTP 409, `error: "USER_ALREADY_EXISTS"`

### 2.3 Register — Validation Failure
```bash
-d '{"email":"not-an-email","password":"short"}'
```
**Expected:** HTTP 400, validation errors for `email` and `password`

### 2.4 Login — Happy Path
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"test@tripforge.com","password":"Test@12345"}'
```
**Expected:** HTTP 200, new JWT token returned

### 2.5 Login — Wrong Password
**Expected:** HTTP 401, `error: "INVALID_CREDENTIALS"`

### 2.6 Get Profile — With Valid JWT
```bash
curl http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer <TOKEN>"
```
**Expected:** HTTP 200, user profile data

### 2.7 Get Profile — Without JWT
**Expected:** HTTP 401 from gateway

### 2.8 Update Preferences
```bash
curl -X PUT http://localhost:8080/api/users/preferences \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"interests":["nature","food"],"hotelPreference":"STANDARD","defaultBudget":50000,"defaultTravelers":2}'
```
**Expected:** HTTP 200, updated preferences returned

---

## 3. Trip Service Tests

### 3.1 Create Trip — Happy Path
```bash
curl -X POST http://localhost:8080/api/trip/create \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{
    "destination":"Goa",
    "startDate":"2025-06-01",
    "endDate":"2025-06-05",
    "totalBudget":50000,
    "travelers":2,
    "interests":["beaches","food"],
    "hotelPreference":"STANDARD"
  }'
```
**Expected:** HTTP 201, `tripId` present, `itinerary` non-empty, `selectedHotel` non-null

### 3.2 Create Trip — Partial Response (downstream down)
Stop hotel-service, create trip.
**Expected:** HTTP 201, `selectedHotel: null`, `itinerary` still populated from route-service

### 3.3 Get Trip by ID
```bash
curl http://localhost:8080/api/trip/1 -H "Authorization: Bearer <TOKEN>"
```
**Expected:** HTTP 200, full TripResponse

### 3.4 Get Trip — Wrong User
Use a different user's token to access trip ID 1.
**Expected:** HTTP 404

### 3.5 Get User Trips
```bash
curl http://localhost:8080/api/trip/user/1 -H "Authorization: Bearer <TOKEN>"
```
**Expected:** HTTP 200, array of TripSummaryDto

### 3.6 Replan Trip
```bash
curl -X PUT http://localhost:8080/api/trip/replan \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"tripId":1,"newHotelId":5,"changeReason":"CHEAPER"}'
```
**Expected:** HTTP 200, updated TripResponse with new hotel

---

## 4. Hotel Service Tests

### 4.1 Get Recommendations
```bash
curl "http://localhost:8080/api/hotels/recommend?destination=Goa&budget=50000&durationDays=4&travelers=2&hotelPreference=STANDARD" \
  -H "Authorization: Bearer <TOKEN>"
```
**Expected:** HTTP 200, array of HotelDto sorted by relevanceScore descending

### 4.2 Change Hotel — CHEAPER
```bash
curl -X POST http://localhost:8080/api/hotels/change \
  -H "Authorization: Bearer <TOKEN>" \
  -H "Content-Type: application/json" \
  -d '{"tripId":1,"currentHotelId":3,"reason":"CHEAPER","destination":"Goa","budget":50000,"durationDays":4,"travelers":2}'
```
**Expected:** HTTP 200, alternatives sorted by price ascending

### 4.3 Change Hotel — BETTER_RATING
Same as above with `"reason":"BETTER_RATING"`.
**Expected:** Alternatives sorted by rating descending

### 4.4 Change Hotel — CLOSER
**Expected:** Alternatives sorted by distanceFromCenterKm ascending

### 4.5 Change Hotel — PREMIUM
**Expected:** Alternatives are LUXURY category hotels

---

## 5. ML Service Tests

### 5.1 Health Check
```bash
curl http://localhost:8087/health
```
**Expected:** `{"status":"UP","models":{"hotel_ranker":{"loaded":true},"trip_classifier":{"loaded":true}}}`

### 5.2 Hotel Rank
```bash
curl -X POST http://localhost:8087/api/ml/hotel-rank \
  -H "Content-Type: application/json" \
  -d '{
    "destination":"Goa","budget":50000,"tripDurationDays":4,
    "travelersCount":2,"hotelPreference":"STANDARD",
    "candidateHotels":[
      {"hotelId":3,"name":"Novotel","destination":"Goa","pricePerNight":7500,
       "rating":4.2,"distanceFromCenterKm":1.8,"category":"STANDARD",
       "amenities":["pool","wifi"],"popularityScore":8.1}
    ]
  }'
```
**Expected:** `rankedHotels[0].score` between 0 and 1, `reasonTags` non-empty

### 5.3 Trip Style Classification
```bash
curl -X POST http://localhost:8087/api/ml/classify-trip-style \
  -H "Content-Type: application/json" \
  -d '{"budget":200000,"travelersCount":2,"tripDurationDays":5,"interests":["food","beaches"],"hotelPreference":"LUXURY"}'
```
**Expected:** `tripStyle: "LUXURY"`, `confidence` > 0.7

### 5.4 ML Unit Tests
```bash
cd ml-service && python3 -m pytest tests/ -v
```
**Expected:** 41/41 tests pass

---

## 6. Frontend Tests (Manual)

### 6.1 Auth Flow
- [ ] Register with new email → redirects to dashboard
- [ ] Navbar shows user name
- [ ] Refresh page → stays logged in (token restored)
- [ ] Sign out → redirects to login
- [ ] Access `/dashboard` without token → redirects to `/login`

### 6.2 Create Trip Flow
- [ ] Form validates empty destination
- [ ] Form validates end date before start date
- [ ] Form validates budget < 1000
- [ ] Interest chips toggle on/off
- [ ] Hotel preference selection highlights correctly
- [ ] Submit shows loading spinner
- [ ] Result page renders with all sections

### 6.3 Hotel Change Flow
- [ ] "Change Hotel" button opens modal
- [ ] All 4 reasons are selectable
- [ ] "Find Alternatives" shows loading state
- [ ] Alternatives render with hotel cards
- [ ] "Select This Hotel" closes modal and updates page

### 6.4 Trip History
- [ ] History page shows all created trips
- [ ] Each card shows destination, dates, budget, status badge
- [ ] Clicking card navigates to details page
- [ ] Details page renders full trip

---

## 7. Common Failure Scenarios

| Scenario | Expected Behavior |
|---|---|
| ml-service down during hotel recommendation | hotel-service falls back to rule-based scoring; trip still created |
| route-service down during trip create | `itinerary: []` in response; trip saved; no crash |
| budget-service down during trip create | `budgetBreakdown: null` in response; trip saved |
| split-service down during trip create | `splitResult: null` in response; trip saved |
| JWT expired | Gateway returns 401; frontend redirects to login |
| Wrong JWT_SECRET | All protected routes return 401 |
| Destination not in dataset | Empty hotel/attraction results; no crash |
| PostgreSQL down | Services fail health check; Flyway migration fails on startup |
| ML models missing | ml-service starts in DEGRADED mode; hotel ranking uses rule-based fallback |
