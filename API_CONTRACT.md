# TripForge — API Contract Reference

This document maps frontend API calls to backend endpoints and documents
the DTO field names that must remain stable across services.

---

## Auth Service (`/api/auth/**`, `/api/users/**`)

### POST /api/auth/register
**Frontend sends:**
```json
{ "email": "string", "password": "string", "firstName": "string", "lastName": "string" }
```
**Backend returns (`AuthResponse`):**
```json
{
  "token": "string",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "string",
  "firstName": "string",
  "lastName": "string",
  "expiresIn": 86400000
}
```
**Frontend reads:** `res.data.token`, `res.data.userId`, `res.data.firstName`, `res.data.lastName`

---

### POST /api/auth/login
Same request/response shape as register.

---

### GET /api/users/profile
**Header:** `Authorization: Bearer <token>` (gateway forwards `X-User-Id`)
**Returns (`UserProfileDto`):**
```json
{ "id": 1, "email": "string", "firstName": "string", "lastName": "string", "role": "USER" }
```

---

### PUT /api/users/preferences
**Frontend sends:**
```json
{ "interests": ["nature","food"], "hotelPreference": "STANDARD", "defaultBudget": 50000, "defaultTravelers": 2 }
```

---

## Trip Service (`/api/trip/**`)

### POST /api/trip/create
**Frontend sends (`TripCreateRequest`):**
```json
{
  "destination": "Goa",
  "startDate": "2025-02-01",
  "endDate": "2025-02-05",
  "totalBudget": 50000,
  "travelers": 2,
  "interests": ["beaches", "food"],
  "hotelPreference": "STANDARD"
}
```
**Backend returns (`TripResponse`):**
```json
{
  "tripId": 1,
  "userId": 1,
  "destination": "Goa",
  "startDate": "2025-02-01",
  "endDate": "2025-02-05",
  "durationDays": 4,
  "totalBudget": 50000,
  "travelers": 2,
  "interests": ["beaches", "food"],
  "hotelPreference": "STANDARD",
  "status": "PLANNED",
  "createdAt": "2025-01-15T10:30:00",
  "itinerary": [...],
  "selectedHotel": {...},
  "alternativeHotels": [...],
  "budgetBreakdown": {...},
  "splitResult": {...}
}
```
**⚠ Critical fields frontend reads:**
- `res.data.tripId` (not `id`)
- `res.data.selectedHotel` (may be null if hotel-service is down)
- `res.data.itinerary` (may be empty array if route-service is down)
- `res.data.budgetBreakdown` (may be null)
- `res.data.splitResult` (may be null)

---

### GET /api/trip/{tripId}
Returns same `TripResponse` shape.

### GET /api/trip/user/{userId}
Returns `List<TripSummaryDto>`:
```json
[{
  "tripId": 1,
  "destination": "Goa",
  "startDate": "2025-02-01",
  "endDate": "2025-02-05",
  "durationDays": 4,
  "totalBudget": 50000,
  "travelers": 2,
  "status": "PLANNED",
  "createdAt": "..."
}]
```

### PUT /api/trip/replan
**Frontend sends:**
```json
{ "tripId": 1, "newHotelId": 5, "changeReason": "CHEAPER" }
```
Returns updated `TripResponse`.

---

## Hotel Service (`/api/hotels/**`)

### POST /api/hotels/change
**Frontend sends:**
```json
{
  "tripId": 1,
  "currentHotelId": 3,
  "reason": "CHEAPER",
  "destination": "Goa",
  "budget": 50000,
  "durationDays": 4,
  "travelers": 2
}
```
**Returns `List<HotelDto>`:**
```json
[{
  "id": 5,
  "name": "Hotel Name",
  "destination": "Goa",
  "pricePerNight": 3200.0,
  "rating": 3.8,
  "distanceFromCenterKm": 2.5,
  "amenities": ["wifi", "restaurant"],
  "category": "BUDGET",
  "popularityScore": 7.2,
  "relevanceScore": 0.72
}]
```
**⚠ Frontend normalizes via `normalizeHotel()` in formatters.js**

---

## ML Service (`/api/ml/**`)

### POST /api/ml/hotel-rank
Called by hotel-service internally (not directly by frontend).

### POST /api/ml/recommend-alternative-hotel
Called by hotel-service internally.

### POST /api/ml/classify-trip-style
Can be called directly for trip style display.

---

## Known Field Name Risks

| Risk | Location | Mitigation |
|---|---|---|
| `tripId` vs `id` in trip response | TripResponse.java | Frontend uses `trip.tripId ?? trip.id` in normalizeTrip() |
| `pricePerNight` vs `price_per_night` | HotelDto | normalizeHotel() handles both |
| `distanceFromCenterKm` vs `distance_from_center_km` | HotelDto | normalizeHotel() handles both |
| `itinerary` may be null | TripResponse | normalizeTrip() defaults to `[]` |
| `budgetBreakdown` may be null | TripResponse | BudgetBreakdownCard checks for null |
| `splitResult` may be null | TripResponse | SplitExpenseCard checks for null |
| `selectedHotel` may be null | TripResponse | HotelRecommendationCard checks for null |
| `res.data` wrapping | All endpoints | All API calls use `res.data` (ApiResponse wrapper) |

---

## ApiResponse Wrapper

All Java endpoints return:
```json
{
  "success": true,
  "message": "Success",
  "data": { ... actual payload ... },
  "timestamp": "2025-01-15T10:30:00"
}
```
Frontend always reads `res.data` (the Axios response), then `.data` for the payload.
So actual data is at `res.data.data` — **this is the most common source of bugs**.

**Frontend API calls use:** `axiosClient.post(...).then(r => r.data)` → returns `ApiResponse`
**Then components use:** `res.data` → the actual payload object.
