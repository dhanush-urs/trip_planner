"""
Alternative hotel re-ranking service.

Applies feedback-aware weight adjustments to the rule-based scoring,
then fuses with ML predictions using the adjusted weights.

Feedback weight profiles:
  CHEAPER       → prioritize price_fit, reduce rating weight
  BETTER_RATING → prioritize rating, relax price strictness
  CLOSER        → prioritize distance heavily
  PREMIUM       → prioritize category_match + amenities + rating
"""
import logging
from typing import List

import numpy as np

from app.config import settings
from app.models.model_loader import model_loader
from app.schemas.hotel_feedback import (
    AlternativeHotel,
    AlternativeHotelRequest,
    AlternativeHotelResponse,
)
from app.schemas.hotel_rank import HotelCandidate
from app.services.feature_builder import build_hotel_feature_matrix
from app.utils.scoring import (
    price_fit_score,
    rating_score,
    distance_score,
    category_match_score,
    amenities_match_score,
    popularity_score_normalized,
    generate_reason_tags,
)

logger = logging.getLogger(__name__)

# ── Feedback weight profiles ──────────────────────────────────────────────────
# Each profile defines weights for: price_fit, rating, distance, category_match,
# amenities_match, popularity
FEEDBACK_WEIGHTS = {
    "CHEAPER": {
        "price_fit": 0.45,
        "rating": 0.15,
        "distance": 0.15,
        "category_match": 0.10,
        "amenities_match": 0.05,
        "popularity": 0.10,
    },
    "BETTER_RATING": {
        "price_fit": 0.15,
        "rating": 0.45,
        "distance": 0.15,
        "category_match": 0.10,
        "amenities_match": 0.05,
        "popularity": 0.10,
    },
    "CLOSER": {
        "price_fit": 0.15,
        "rating": 0.20,
        "distance": 0.45,
        "category_match": 0.10,
        "amenities_match": 0.05,
        "popularity": 0.05,
    },
    "PREMIUM": {
        "price_fit": 0.05,
        "rating": 0.25,
        "distance": 0.10,
        "category_match": 0.30,
        "amenities_match": 0.20,
        "popularity": 0.10,
    },
}


def _compute_feedback_rule_score(
    hotel: HotelCandidate,
    budget: float,
    duration_days: int,
    preference: str,
    weights: dict,
) -> tuple[float, dict]:
    """
    Compute a feedback-weighted rule score for a single hotel.
    Returns (composite_score, sub_scores_dict).
    """
    pf = price_fit_score(hotel.price_per_night, budget, duration_days)
    rs = rating_score(hotel.rating)
    ds = distance_score(hotel.distance_from_center_km)
    cm = category_match_score(hotel.category, preference)
    am = amenities_match_score(hotel.amenities, preference)
    ps = popularity_score_normalized(hotel.popularity_score)

    composite = (
        pf * weights["price_fit"]
        + rs * weights["rating"]
        + ds * weights["distance"]
        + cm * weights["category_match"]
        + am * weights["amenities_match"]
        + ps * weights["popularity"]
    )

    sub = {
        "price_fit": round(pf, 4),
        "rating": round(rs, 4),
        "distance": round(ds, 4),
        "category_match": round(cm, 4),
        "amenities_match": round(am, 4),
        "popularity": round(ps, 4),
        "composite": round(composite, 4),
    }
    return composite, sub


def rerank_hotels(request: AlternativeHotelRequest) -> AlternativeHotelResponse:
    """
    Re-rank hotel candidates based on user feedback reason.
    Excludes the current hotel from results.
    """
    reason = request.feedback_reason.value
    weights = FEEDBACK_WEIGHTS.get(reason, FEEDBACK_WEIGHTS["BETTER_RATING"])

    # Exclude current hotel
    candidates = [
        h for h in request.candidate_hotels
        if h.hotel_id != request.current_hotel_id
    ]

    if not candidates:
        return AlternativeHotelResponse(
            alternatives=[],
            feedbackReason=reason,
            currentHotelId=request.current_hotel_id,
        )

    # ── Step 1: Feedback-weighted rule scores ─────────────────────────────────
    rule_scores = []
    sub_scores_list = []
    for h in candidates:
        score, sub = _compute_feedback_rule_score(
            hotel=h,
            budget=request.budget,
            duration_days=request.trip_duration_days,
            preference=request.hotel_preference,
            weights=weights,
        )
        rule_scores.append(score)
        sub_scores_list.append(sub)

    rule_scores_arr = np.array(rule_scores)

    # ── Step 2: ML prediction (with feedback-adjusted fusion weight) ──────────
    model_used = "rule_based_feedback"
    ml_scores_arr = rule_scores_arr.copy()

    if model_loader.hotel_ranker_loaded:
        try:
            X = build_hotel_feature_matrix(
                budget=request.budget,
                duration_days=request.trip_duration_days,
                travelers_count=request.travelers_count,
                destination=request.destination,
                preference=request.hotel_preference,
                hotels=candidates,
            )
            raw_preds = model_loader.hotel_ranker.predict(X)
            pred_min, pred_max = raw_preds.min(), raw_preds.max()
            if pred_max > pred_min:
                ml_scores_arr = (raw_preds - pred_min) / (pred_max - pred_min)
            else:
                ml_scores_arr = np.ones(len(candidates)) * 0.5
            model_used = "hybrid_feedback"
        except Exception as e:
            logger.warning("ML re-ranking failed, using feedback rule scores: %s", e)

    # For feedback re-ranking, give more weight to rule scores (they encode the feedback)
    ml_w = 0.45
    rule_w = 0.55
    final_scores = ml_w * ml_scores_arr + rule_w * rule_scores_arr

    # ── Build output ──────────────────────────────────────────────────────────
    indexed = sorted(enumerate(final_scores), key=lambda x: x[1], reverse=True)

    alternatives = []
    for rank, (idx, score) in enumerate(indexed, start=1):
        h = candidates[idx]
        tags = generate_reason_tags(sub_scores_list[idx], feedback_reason=reason)
        if h.category.upper() == "LUXURY":
            tags.append("premium_choice")

        alternatives.append(
            AlternativeHotel(
                hotelId=h.hotel_id,
                score=round(float(score), 4),
                rank=rank,
                reasonTags=tags,
                feedbackApplied=reason,
            )
        )

    logger.info(
        "Re-ranked %d alternatives for reason=%s using %s",
        len(alternatives), reason, model_used,
    )

    return AlternativeHotelResponse(
        alternatives=alternatives,
        feedbackReason=reason,
        currentHotelId=request.current_hotel_id,
    )
