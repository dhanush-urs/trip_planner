"""
ML service API routes.

All endpoints are thin — they validate input via Pydantic,
delegate to service layer, and return structured responses.
"""
import logging

from fastapi import APIRouter, HTTPException

from app.schemas.hotel_rank import HotelRankRequest, HotelRankResponse
from app.schemas.hotel_feedback import AlternativeHotelRequest, AlternativeHotelResponse
from app.schemas.trip_style import TripStyleRequest, TripStyleResponse
from app.services.ranking_service import rank_hotels
from app.services.rerank_service import rerank_hotels
from app.services.classifier_service import classify_trip_style

logger = logging.getLogger(__name__)

router = APIRouter(prefix="/api/ml", tags=["ML"])


@router.post(
    "/hotel-rank",
    response_model=HotelRankResponse,
    summary="Rank hotel candidates using hybrid ML + rule-based scoring",
)
def hotel_rank(request: HotelRankRequest):
    """
    Ranks a list of hotel candidates for a given trip context.

    Uses a 3-step hybrid pipeline:
    1. Rule-based sub-scores (price fit, rating, distance, category match)
    2. GradientBoostingRegressor ML prediction
    3. Fused final score: 0.65 × ML + 0.35 × rule

    Sample request:
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
          "hotelId": 1,
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
    """
    try:
        logger.info(
            "hotel-rank request: destination=%s, hotels=%d",
            request.destination,
            len(request.candidate_hotels),
        )
        return rank_hotels(request)
    except Exception as e:
        logger.error("hotel-rank error: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.post(
    "/recommend-alternative-hotel",
    response_model=AlternativeHotelResponse,
    summary="Re-rank hotels based on user feedback reason",
)
def recommend_alternative(request: AlternativeHotelRequest):
    """
    Re-ranks hotel candidates based on user feedback.

    Feedback reasons and their effect:
    - CHEAPER: Prioritizes price fit (weight 0.45)
    - BETTER_RATING: Prioritizes rating (weight 0.45)
    - CLOSER: Prioritizes distance to center (weight 0.45)
    - PREMIUM: Prioritizes category + amenities + rating

    Sample request:
    ```json
    {
      "currentHotelId": 3,
      "feedbackReason": "CHEAPER",
      "destination": "Goa",
      "budget": 50000,
      "tripDurationDays": 4,
      "travelersCount": 2,
      "hotelPreference": "STANDARD",
      "candidateHotels": [...]
    }
    ```
    """
    try:
        logger.info(
            "recommend-alternative request: reason=%s, candidates=%d",
            request.feedback_reason,
            len(request.candidate_hotels),
        )
        return rerank_hotels(request)
    except Exception as e:
        logger.error("recommend-alternative error: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))


@router.post(
    "/classify-trip-style",
    response_model=TripStyleResponse,
    summary="Classify the trip style from user inputs",
)
def classify_trip(request: TripStyleRequest):
    """
    Classifies the trip style into one of:
    BUDGET, FAMILY, NIGHTLIFE, ADVENTURE, LUXURY, CULTURAL

    Sample request:
    ```json
    {
      "budget": 80000,
      "travelersCount": 2,
      "tripDurationDays": 5,
      "interests": ["adventure", "nature"],
      "hotelPreference": "STANDARD"
    }
    ```

    Sample response:
    ```json
    {
      "tripStyle": "ADVENTURE",
      "confidence": 0.87,
      "topSignals": [
        {"feature": "has_adventure", "value": "1.0", "contribution": "34.2%"},
        {"feature": "budget_per_person", "value": "40000.0", "contribution": "22.1%"}
      ]
    }
    ```
    """
    try:
        logger.info(
            "classify-trip-style request: budget=%.0f, travelers=%d",
            request.budget,
            request.travelers_count,
        )
        return classify_trip_style(request)
    except Exception as e:
        logger.error("classify-trip-style error: %s", e, exc_info=True)
        raise HTTPException(status_code=500, detail=str(e))
