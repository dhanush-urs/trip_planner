# TripForge — Final Smoke Test Plan

Zero-state test sequence. Run this after a clean `make reset` + `make up`.

---

## Phase 0: Clean Start

```bash
# Full clean slate
make reset

# Train ML models (if not already done)
make train

# Start everything
make up

# Wait 3-4 minutes, then verify
make health
```

**Expected after `make health`:** All 10+ checks show ✓ UP or ✓ LOADED.

---

## Phase 1: Infrastructure Verification

### 1.1 Eureka Dashboard
Open http://localhost:8761

**Expected:** Dashboard shows these registered services:
- AUTH-SERVICE
- TRIP-SERVICE
- HOTEL-SERVICE
- ROUTE-SERVICE
- BUDGET-SERVICE
- SPLIT-SERVICE
- API-GATEWAY

### 1.2 Gateway Health
```bash
curl -s http://localhost:8080/actuator/health | python3 -m json.tool
```
**Expected:** `{"status": "UP"}`

### 1.3 ML Service Health
```bash
curl -s http://localhost:8087/health | python3 -m json.tool
```
**Expected:**
```json
{
  "status": "UP",
  "models": {
    "hotel_ranker": { "loaded": true, "status": "ready" },
    "trip_classifier": { "loaded": true, "status": "ready" }
  }
}
```

---

## Phase 2: Auth Flow

### 2.1 Register
```bash
curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "demo@tripforge.com",
    "password": "Demo@12345",
    "firstName": "Demo",
    "lastName": "User"
  }' | python3 -m json.tool
```

**Expected:**
```json
{
  "success": true,
  "message": "Registration successful",
  "data": {
    "token": "eyJhbGci...",
    "userId": 1,
    "email": "demo@tripforge.com",
    "firstName": "Demo",
    "lastName": "User"
  }
}
```

**Save the token:**
```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"email":"smoke@tripforge.com","password":"Demo@12345","firstName":"Smoke","lastName":"Test"}' \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['token'])")
echo "TOKEN: $TOKEN"
```

### 2.2 Login
```bash
curl -s -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email": "smoke@tripforge.com", "password": "Demo@12345"}' \
  | python3 -m json.tool
```
**Expected:** HTTP 200, new token returned.

### 2.3 Get Profile
```bash
curl -s http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```
**Expected:** `{"id": 1, "email": "smoke@tripforge.com", ...}`

### 2.4 Unauthenticated Request (should fail)
```bash
curl -s -o /dev/null -w "%{http_code}" http://localhost:8080/api/users/profile
```
**Expected:** `401`

---

## Phase 3: Trip Creation

### 3.1 Create Trip
```bash
TRIP_RESPONSE=$(curl -s -X POST http://localhost:8080/api/trip/create \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "Goa",
    "startDate": "2025-08-01",
    "endDate": "2025-08-05",
    "totalBudget": 50000,
    "travelers": 2,
    "interests": ["beaches", "food"],
    "hotelPreference": "STANDARD"
  }')

echo $TRIP_RESPONSE | python3 -m json.tool
```

**Expected:**
- HTTP 201
- `data.tripId` is a number
- `data.destination` = "Goa"
- `data.itinerary` is a non-empty array
- `data.selectedHotel` is non-null (has `name`, `pricePerNight`, `rating`)
- `data.budgetBreakdown` is non-null (has `totalEstimated`, `overBudget`)
- `data.splitResult` is non-null (has `perPersonAmount`)

**Save trip ID:**
```bash
TRIP_ID=$(echo $TRIP_RESPONSE | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['tripId'])")
HOTEL_ID=$(echo $TRIP_RESPONSE | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['selectedHotel']['id'])")
echo "TRIP_ID: $TRIP_ID  |  HOTEL_ID: $HOTEL_ID"
```

### 3.2 Verify Itinerary
```bash
echo $TRIP_RESPONSE | python3 -c "
import sys, json
data = json.load(sys.stdin)['data']
days = data.get('itinerary', [])
print(f'Itinerary days: {len(days)}')
for d in days:
    print(f'  Day {d[\"dayNumber\"]}: {d[\"theme\"]} — {len(d[\"places\"])} places')
"
```
**Expected:** 4 days, each with 2-3 places.

### 3.3 Verify Budget
```bash
echo $TRIP_RESPONSE | python3 -c "
import sys, json
b = json.load(sys.stdin)['data']['budgetBreakdown']
print(f'Hotel: {b[\"hotelCost\"]}')
print(f'Food:  {b[\"foodCost\"]}')
print(f'Total: {b[\"totalEstimated\"]}')
print(f'Over budget: {b[\"overBudget\"]}')
"
```
**Expected:** All values > 0, `overBudget` = false for ₹50,000 budget.

---

## Phase 4: Get Trip

### 4.1 Fetch by ID
```bash
curl -s http://localhost:8080/api/trip/$TRIP_ID \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```
**Expected:** Same TripResponse shape as creation.

### 4.2 Trip History
```bash
USER_ID=$(curl -s http://localhost:8080/api/users/profile \
  -H "Authorization: Bearer $TOKEN" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")

curl -s http://localhost:8080/api/trip/user/$USER_ID \
  -H "Authorization: Bearer $TOKEN" | python3 -m json.tool
```
**Expected:** Array with at least 1 trip summary.

---

## Phase 5: Hotel Change

### 5.1 Get Alternatives (CHEAPER)
```bash
curl -s -X POST http://localhost:8080/api/hotels/change \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"tripId\": $TRIP_ID,
    \"currentHotelId\": $HOTEL_ID,
    \"reason\": \"CHEAPER\",
    \"destination\": \"Goa\",
    \"budget\": 50000,
    \"durationDays\": 4,
    \"travelers\": 2
  }" | python3 -m json.tool
```
**Expected:** Array of HotelDto, each with `pricePerNight` < current hotel's price.

### 5.2 Replan with New Hotel
```bash
# Get the cheapest alternative ID
NEW_HOTEL_ID=$(curl -s -X POST http://localhost:8080/api/hotels/change \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"tripId\":$TRIP_ID,\"currentHotelId\":$HOTEL_ID,\"reason\":\"CHEAPER\",\"destination\":\"Goa\",\"budget\":50000,\"durationDays\":4,\"travelers\":2}" \
  | python3 -c "import sys,json; hotels=json.load(sys.stdin)['data']; print(hotels[0]['id']) if hotels else print($HOTEL_ID)")

curl -s -X PUT http://localhost:8080/api/trip/replan \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{\"tripId\": $TRIP_ID, \"newHotelId\": $NEW_HOTEL_ID, \"changeReason\": \"CHEAPER\"}" \
  | python3 -m json.tool
```
**Expected:** Updated TripResponse with new `selectedHotel.id` = `$NEW_HOTEL_ID`.

---

## Phase 6: ML Service Direct Tests

### 6.1 Hotel Rank
```bash
curl -s -X POST http://localhost:8087/api/ml/hotel-rank \
  -H "Content-Type: application/json" \
  -d '{
    "destination": "Goa",
    "budget": 50000,
    "tripDurationDays": 4,
    "travelersCount": 2,
    "hotelPreference": "STANDARD",
    "candidateHotels": [
      {"hotelId": 3, "name": "Novotel Goa", "destination": "Goa",
       "pricePerNight": 7500, "rating": 4.2, "distanceFromCenterKm": 1.8,
       "category": "STANDARD", "amenities": ["pool","wifi"], "popularityScore": 8.1},
      {"hotelId": 5, "name": "Ibis Goa", "destination": "Goa",
       "pricePerNight": 3200, "rating": 3.8, "distanceFromCenterKm": 2.5,
       "category": "BUDGET", "amenities": ["wifi"], "popularityScore": 7.2}
    ]
  }' | python3 -m json.tool
```
**Expected:** `rankedHotels` array, each with `score` in [0,1], `rank` sequential, `reasonTags` non-empty.

### 6.2 Trip Style Classifier
```bash
curl -s -X POST http://localhost:8087/api/ml/classify-trip-style \
  -H "Content-Type: application/json" \
  -d '{"budget": 200000, "travelersCount": 2, "tripDurationDays": 5, "interests": ["food","beaches"], "hotelPreference": "LUXURY"}' \
  | python3 -m json.tool
```
**Expected:** `tripStyle: "LUXURY"`, `confidence` > 0.7.

---

## Phase 7: Frontend Smoke Test (Manual)

1. Open http://localhost:3000
2. Register with `ui-test@tripforge.com` / `Demo@12345`
3. Verify redirect to dashboard, name shown in navbar
4. Click "Plan New Trip" → fill form → submit
5. Verify trip result page renders (hotel + itinerary + budget + split)
6. Click "Change Hotel" → select CHEAPER → find alternatives → select one
7. Verify hotel card updates
8. Click "My Trips" → verify trip appears
9. Click trip card → verify details page renders

---

## Recommended Test Data

| Field | Value | Reason |
|---|---|---|
| Destination | Goa | Most data (10 hotels, 10 attractions) |
| Duration | 4 days | Shows multi-day itinerary |
| Budget | ₹50,000 | Fits STANDARD hotels, shows remaining budget |
| Travelers | 2 | Clean split (50% each) |
| Interests | beaches + food | Strong Goa data for both |
| Hotel Preference | STANDARD | Best for change demo (can go cheaper or premium) |

---

## Pass Criteria Summary

| Test | Pass Criteria |
|---|---|
| All health checks | ≥ 9 services UP |
| Register | HTTP 201, token returned |
| Login | HTTP 200, token returned |
| Profile | HTTP 200, user data returned |
| Unauthenticated | HTTP 401 |
| Create trip | HTTP 201, itinerary non-empty, hotel non-null |
| Get trip | HTTP 200, same shape |
| Trip history | HTTP 200, array with ≥ 1 item |
| Hotel change | HTTP 200, alternatives array non-empty |
| Replan | HTTP 200, new hotel ID in response |
| ML hotel rank | HTTP 200, scores in [0,1] |
| ML classifier | HTTP 200, valid TripStyle enum |
| Frontend | Page loads, no console errors |
