# TripForge — API Contracts

All requests go through the API Gateway at `http://localhost:8080`.

**Response envelope** — all Java endpoints return:
```json
{
  "success": true,
  "message": "Success",
  "data": { ... actual payload ... },
  "timestamp": "2025-01-15T10:30:00"
}
```
The frontend API modules unwrap this automatically. Components receive the payload directly.

---

## Auth Flow

### POST /api/auth/register
**Public — no JWT required**

Request:
```json
{
  "email": "arjun@example.com",
  "password": "SecurePass@123",
  "firstName": "Arjun",
  "lastName": "Sharma",
  "phone": "9876543210"
}
```

Response `201`:
```json
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "arjun@example.com",
  "firstName": "Arjun",
  "lastName": "Sharma",
  "expiresIn": 86400000
}
```

Error `409` (duplicate email):
```json
{ "success": false, "message": "An account with email arjun@example.com already exists", "error": "USER_ALREADY_EXISTS" }
```

---

### POST /api/auth/login
**Public — no JWT required**

Request:
```json
{ "email": "arjun@example.com", "password": "SecurePass@123" }
```

Response `200`: Same shape as register response.

Error `401`:
```json
{ "success": false, "message": "Invalid email or password", "error": "INVALID_CREDENTIALS" }
```

---

### GET /api/users/profile
**JWT required**

Response `200`:
```json
{
  "id": 1,
  "email": "arjun@example.com",
  "firstName": "Arjun",
  "lastName": "Sharma",
  "phone": "9876543210",
  "role": "USER",
  "createdAt": "2025-01-15T10:30:00"
}
```

---

### PUT /api/users/preferences
**JWT required**

Request:
```json
{
  "interests": ["nature", "food", "beaches"],
  "hotelPreference": "STANDARD",
  "defaultBudget": 50000.00,
  "defaultTravelers": 2
}
```

Response `200`: Returns updated `UserPreferenceDto`.

---

## Trip Create Flow

### POST /api/trip/create
**JWT required**

Request:
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

Response `201` (full TripResponse):
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
  "itinerary": [
    {
      "dayNumber": 1,
      "date": "2025-02-01",
      "theme": "Beach & Relaxation",
      "places": [
        {
          "attractionId": 1,
          "name": "Baga Beach",
          "category": "beach",
          "visitTime": "09:00 AM",
          "avgVisitHours": 3.0,
          "ticketCost": 0,
          "notes": "Avg visit: 3.0 hrs | Category: beach",
          "visitOrder": 1
        }
      ]
    }
  ],
  "selectedHotel": {
    "id": 3,
    "name": "Novotel Goa Candolim",
    "destination": "Goa",
    "pricePerNight": 7500.0,
    "rating": 4.2,
    "distanceFromCenterKm": 1.8,
    "amenities": ["pool", "wifi", "restaurant", "gym"],
    "category": "STANDARD",
    "popularityScore": 8.1,
    "relevanceScore": 0.782
  },
  "alternativeHotels": [ ... ],
  "budgetBreakdown": {
    "tripId": 1,
    "hotelCost": 30000.00,
    "foodCost": 4800.00,
    "transportCost": 3200.00,
    "attractionCost": 0.00,
    "miscCost": 1900.00,
    "totalEstimated": 39900.00,
    "totalBudget": 50000.00,
    "remainingBudget": 10100.00,
    "overBudget": false
  },
  "splitResult": {
    "tripId": 1,
    "totalAmount": 39900.00,
    "travelers": 2,
    "perPersonAmount": 19950.00,
    "participants": [
      { "name": "Traveler 1", "amount": 19950.00, "percentage": 50.0 },
      { "name": "Traveler 2", "amount": 19950.00, "percentage": 50.0 }
    ]
  }
}
```

**Note:** `itinerary`, `selectedHotel`, `budgetBreakdown`, `splitResult` may be null/empty if downstream services are unavailable. The frontend handles all null cases.

---

### GET /api/trip/{tripId}
**JWT required**

Returns same `TripResponse` shape. Returns `404` if trip not found or belongs to another user.

---

### GET /api/trip/user/{userId}
**JWT required**

Returns `TripSummaryDto[]`:
```json
[
  {
    "tripId": 1,
    "destination": "Goa",
    "startDate": "2025-02-01",
    "endDate": "2025-02-05",
    "durationDays": 4,
    "totalBudget": 50000,
    "travelers": 2,
    "status": "PLANNED",
    "createdAt": "2025-01-15T10:30:00"
  }
]
```

---

### PUT /api/trip/replan
**JWT required**

Request:
```json
{ "tripId": 1, "newHotelId": 5, "changeReason": "CHEAPER" }
```

Returns updated `TripResponse`.

---

## Hotel Change Flow

### POST /api/hotels/change
**JWT required**

Request:
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

Response `200` — `HotelDto[]`:
```json
[
  {
    "id": 5,
    "name": "Ibis Styles Goa Calangute",
    "destination": "Goa",
    "pricePerNight": 3200.0,
    "rating": 3.8,
    "distanceFromCenterKm": 2.5,
    "amenities": ["wifi", "restaurant"],
    "category": "BUDGET",
    "popularityScore": 7.2,
    "relevanceScore": 0.634
  }
]
```

**Feedback reasons and their effect:**
| Reason | Weight Shift |
|---|---|
| `CHEAPER` | price_fit weight → 0.45 |
| `BETTER_RATING` | rating weight → 0.45 |
| `CLOSER` | distance weight → 0.45 |
| `PREMIUM` | category + amenities weight → 0.50 |

---

## ML Service Endpoints

### POST /api/ml/hotel-rank
Request:
```json
{
  "destination": "Goa",
  "budget": 50000,
  "tripDurationDays": 4,
  "travelersCount": 2,
  "hotelPreference": "STANDARD",
  "interests": ["beaches", "food"],
  "candidateHotels": [
    {
      "hotelId": 3,
      "name": "Novotel Goa Candolim",
      "destination": "Goa",
      "pricePerNight": 7500,
      "rating": 4.2,
      "distanceFromCenterKm": 1.8,
      "category": "STANDARD",
      "amenities": ["pool", "wifi", "restaurant", "gym"],
      "popularityScore": 8.1
    }
  ]
}
```

Response:
```json
{
  "rankedHotels": [
    {
      "hotelId": 3,
      "score": 0.7821,
      "rank": 1,
      "reasonTags": ["budget_fit", "high_rating", "close_to_center", "matches_preference"]
    }
  ],
  "modelUsed": "hybrid_gbr",
  "destination": "Goa"
}
```

---

### POST /api/ml/classify-trip-style
Request:
```json
{
  "budget": 80000,
  "travelersCount": 2,
  "tripDurationDays": 6,
  "interests": ["adventure", "nature"],
  "hotelPreference": "STANDARD"
}
```

Response:
```json
{
  "tripStyle": "ADVENTURE",
  "confidence": 0.87,
  "topSignals": [
    { "feature": "has_adventure", "value": "1.0", "contribution": "34.2%" },
    { "feature": "budget_per_person", "value": "40000.0", "contribution": "22.1%" }
  ]
}
```

---

## Payload Assumptions

| Assumption | Detail |
|---|---|
| All Java endpoints wrap in `ApiResponse<T>` | Frontend unwraps via `r.data.data` in API modules |
| `TripResponse.tripId` not `id` | `normalizeTrip()` handles both with `trip.tripId ?? trip.id` |
| `HotelDto` uses camelCase | `normalizeHotel()` handles snake_case fallback |
| List endpoints return `null` if empty | `tripApi` applies `?? []` guard |
| `selectedHotel` may be null | All hotel renders check for null |
| `budgetBreakdown` may be null | `BudgetBreakdownCard` checks for null |
| `splitResult` may be null | `SplitExpenseCard` checks for null |
| `itinerary` may be empty array | Renders "Itinerary not available" fallback |
