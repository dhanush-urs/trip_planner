# TripForge ML Service

FastAPI-based ML microservice for hotel ranking, alternative re-ranking, and trip style classification.

## Purpose

Provides three ML-powered endpoints consumed by the TripForge hotel-service:

| Endpoint | Description |
|---|---|
| `POST /api/ml/hotel-rank` | Rank hotel candidates using hybrid ML + rule-based scoring |
| `POST /api/ml/recommend-alternative-hotel` | Re-rank hotels based on user feedback reason |
| `POST /api/ml/classify-trip-style` | Classify trip style from user inputs |
| `GET /health` | Service and model health status |

## ML Pipeline

```
Input (hotel candidates + trip context)
    │
    ▼
Step 1: Rule-based sub-scores
    price_fit, rating, distance, category_match, amenities_match, popularity
    │
    ▼
Step 2: GradientBoostingRegressor prediction
    (trained on synthetic relevance labels)
    │
    ▼
Step 3: Hybrid fusion
    final_score = 0.65 × ml_score + 0.35 × rule_score
    │
    ▼
Output: ranked hotels with scores + reason tags
```

## Training Steps

Run these in order from the `ml-service/` directory:

```bash
# 1. Generate hotel ranking training data (~15,750 samples)
python training/generate_hotel_training_data.py

# 2. Train hotel ranker (GradientBoostingRegressor)
python training/train_hotel_ranker.py

# 3. Generate trip style training data (~2,400 samples)
python training/generate_trip_style_data.py

# 4. Train trip style classifier (RandomForestClassifier)
python training/train_trip_classifier.py
```

Models are saved to `saved_models/`.

## Run Locally

```bash
cd ml-service

# Install dependencies
pip install -r requirements.txt

# Train models (required before first run)
python training/generate_hotel_training_data.py
python training/train_hotel_ranker.py
python training/generate_trip_style_data.py
python training/train_trip_classifier.py

# Start the service
uvicorn app.main:app --reload --port 8087
```

API docs: http://localhost:8087/docs

## Run with Docker

```bash
# From ml-service/ directory
# First train models locally to generate saved_models/*.pkl
python training/generate_hotel_training_data.py && python training/train_hotel_ranker.py
python training/generate_trip_style_data.py && python training/train_trip_classifier.py

# Build and run
docker build -t tripforge-ml .
docker run -p 8087:8087 tripforge-ml
```

## Run Tests

```bash
cd ml-service
python -m pytest tests/ -v
```

## Example Requests

### Hotel Rank

```bash
curl -X POST http://localhost:8087/api/ml/hotel-rank \
  -H "Content-Type: application/json" \
  -d '{
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
      },
      {
        "hotelId": 5,
        "name": "Ibis Styles Goa",
        "destination": "Goa",
        "pricePerNight": 3200,
        "rating": 3.8,
        "distanceFromCenterKm": 2.5,
        "category": "BUDGET",
        "amenities": ["wifi", "restaurant"],
        "popularityScore": 7.2
      }
    ]
  }'
```

**Response:**
```json
{
  "rankedHotels": [
    {
      "hotelId": 3,
      "score": 0.7821,
      "rank": 1,
      "reasonTags": ["budget_fit", "high_rating", "close_to_center", "matches_preference"]
    },
    {
      "hotelId": 5,
      "score": 0.6134,
      "rank": 2,
      "reasonTags": ["budget_fit", "close_to_center"]
    }
  ],
  "modelUsed": "hybrid_gbr",
  "destination": "Goa"
}
```

### Alternative Hotel Re-Rank

```bash
curl -X POST http://localhost:8087/api/ml/recommend-alternative-hotel \
  -H "Content-Type: application/json" \
  -d '{
    "currentHotelId": 3,
    "feedbackReason": "CHEAPER",
    "destination": "Goa",
    "budget": 50000,
    "tripDurationDays": 4,
    "travelersCount": 2,
    "hotelPreference": "STANDARD",
    "candidateHotels": [...]
  }'
```

**Response:**
```json
{
  "alternatives": [
    {
      "hotelId": 5,
      "score": 0.7234,
      "rank": 1,
      "reasonTags": ["budget_fit", "feedback_cheaper"],
      "feedbackApplied": "CHEAPER"
    }
  ],
  "feedbackReason": "CHEAPER",
  "currentHotelId": 3
}
```

### Trip Style Classification

```bash
curl -X POST http://localhost:8087/api/ml/classify-trip-style \
  -H "Content-Type: application/json" \
  -d '{
    "budget": 80000,
    "travelersCount": 2,
    "tripDurationDays": 6,
    "interests": ["adventure", "nature"],
    "hotelPreference": "STANDARD"
  }'
```

**Response:**
```json
{
  "tripStyle": "ADVENTURE",
  "confidence": 0.87,
  "topSignals": [
    {"feature": "has_adventure", "value": "1.0", "contribution": "34.2%"},
    {"feature": "budget_per_person", "value": "40000.0", "contribution": "22.1%"},
    {"feature": "preference_encoded", "value": "1.0", "contribution": "18.5%"}
  ]
}
```

## Health Check

```bash
curl http://localhost:8087/health
```

```json
{
  "status": "UP",
  "service": "TripForge ML Service",
  "version": "1.0.0",
  "models": {
    "hotel_ranker": {"loaded": true, "status": "ready"},
    "trip_classifier": {"loaded": true, "status": "ready"}
  }
}
```
