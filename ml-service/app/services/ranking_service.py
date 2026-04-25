"""
Hotel ranking service.

Implements the 3-step hybrid pipeline:
  Step 1: Rule-based sub-scores (interpretable, always computed)
  Step 2: ML model prediction (GradientBoostingRegressor)
  Step 3: Final score fusion: 0.65 * ml_score + 0.35 * rule_score

Falls back to pure rule-based scoring if the ML model is unavailable.
"""
import logging
from typing import List

import numpy as np

from app.config import settings
from app.models.model_loader import model_loader
from app.schemas.hotel_rank import HotelCandidate, HotelRankRequest, HotelRankResponse, RankedHotel
from app.services.feature_builder import build_hotel_feature_matrix
from app.utils.scoring import compute_rule_score, generate_reason_tags

logger = logging.getLogger(__name__)


def rank_hotels(request: HotelRankRequest) -> HotelRankResponse:
    """
    Rank hotel candidates using the hybrid ML + rule-based pipeline.
    Returns hotels sorted by final_score descending.
    """
    hotels = request.candidate_hotels
    if not hotels:
        return HotelRankResponse(
            rankedHotels=[],
            modelUsed="none",
            destination=request.destination,
        )

    # ── Step 1: Rule-based sub-scores ─────────────────────────────────────────
    rule_scores = []
    sub_scores_list = []
    for h in hotels:
        sub = compute_rule_score(
            price_per_night=h.price_per_night,
            rating=h.rating,
            distance_km=h.distance_from_center_km,
            category=h.category,
            amenities=h.amenities,
            popularity=h.popularity_score,
            budget=request.budget,
            duration_days=request.trip_duration_days,
            preference=request.hotel_preference,
        )
        rule_scores.append(sub["composite"])
        sub_scores_list.append(sub)

    rule_scores_arr = np.array(rule_scores)

    # ── Step 2: ML prediction ─────────────────────────────────────────────────
    model_used = "rule_based"
    ml_scores_arr = rule_scores_arr.copy()  # default fallback

    if model_loader.hotel_ranker_loaded:
        try:
            X = build_hotel_feature_matrix(
                budget=request.budget,
                duration_days=request.trip_duration_days,
                travelers_count=request.travelers_count,
                destination=request.destination,
                preference=request.hotel_preference,
                hotels=hotels,
            )
            raw_preds = model_loader.hotel_ranker.predict(X)
            # Normalize ML predictions to [0, 1]
            pred_min, pred_max = raw_preds.min(), raw_preds.max()
            if pred_max > pred_min:
                ml_scores_arr = (raw_preds - pred_min) / (pred_max - pred_min)
            else:
                ml_scores_arr = np.ones(len(hotels)) * 0.5
            model_used = "hybrid_gbr"
        except Exception as e:
            logger.warning("ML prediction failed, using rule-based fallback: %s", e)
    else:
        logger.info("Hotel ranker not loaded — using rule-based scoring only")

    # ── Step 3: Fuse scores ───────────────────────────────────────────────────
    final_scores = (
        settings.ml_weight * ml_scores_arr
        + settings.rule_weight * rule_scores_arr
    )

    # ── Build ranked output ───────────────────────────────────────────────────
    indexed = sorted(
        enumerate(final_scores), key=lambda x: x[1], reverse=True
    )

    ranked = []
    for rank, (idx, score) in enumerate(indexed, start=1):
        h = hotels[idx]
        tags = generate_reason_tags(sub_scores_list[idx])
        if h.category.upper() == "LUXURY":
            tags.append("premium_choice")

        ranked.append(
            RankedHotel(
                hotelId=h.hotel_id,
                score=round(float(score), 4),
                rank=rank,
                reasonTags=tags,
            )
        )

    logger.info(
        "Ranked %d hotels for %s using %s", len(ranked), request.destination, model_used
    )

    return HotelRankResponse(
        rankedHotels=ranked,
        modelUsed=model_used,
        destination=request.destination,
    )
