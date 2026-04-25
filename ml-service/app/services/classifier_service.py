"""
Trip style classification service.

Uses a trained RandomForestClassifier to predict the trip style
from budget, traveler count, duration, interests, and hotel preference.

Also generates top_signals for explainability — which features most
influenced the classification.
"""
import logging
from typing import List

import numpy as np

from app.models.model_loader import model_loader
from app.schemas.trip_style import TopSignal, TripStyleRequest, TripStyleResponse
from app.services.feature_builder import (
    ALL_INTERESTS,
    TRIP_STYLE_FEATURE_NAMES,
    build_trip_style_features,
)
from app.utils.enums import TripStyle

logger = logging.getLogger(__name__)

# Fallback rule-based classification when model is unavailable
def _rule_based_classify(
    budget: float,
    travelers_count: int,
    duration_days: int,
    interests: List[str],
    hotel_preference: str,
) -> tuple[str, float]:
    """Simple rule-based fallback classifier."""
    interests_lower = [i.lower() for i in interests]
    budget_per_person = budget / max(travelers_count, 1)

    if hotel_preference.upper() == "LUXURY" and budget_per_person > 30000:
        return "LUXURY", 0.82
    if "nightlife" in interests_lower:
        return "NIGHTLIFE", 0.78
    if "adventure" in interests_lower and ("manali" in interests_lower or duration_days >= 5):
        return "ADVENTURE", 0.75
    if travelers_count >= 4:
        return "FAMILY", 0.72
    if "temples" in interests_lower or "nature" in interests_lower:
        return "CULTURAL", 0.70
    if budget_per_person < 10000:
        return "BUDGET", 0.68
    return "FAMILY", 0.60


def classify_trip_style(request: TripStyleRequest) -> TripStyleResponse:
    """
    Classify the trip style using the trained model (or rule-based fallback).
    Returns the predicted style, confidence, and top signals.
    """
    interests_lower = [i.lower() for i in request.interests]

    if not model_loader.trip_classifier_loaded:
        logger.info("Trip classifier not loaded — using rule-based fallback")
        style, confidence = _rule_based_classify(
            budget=request.budget,
            travelers_count=request.travelers_count,
            duration_days=request.trip_duration_days,
            interests=request.interests,
            hotel_preference=request.hotel_preference,
        )
        signals = _build_signals_rule_based(request)
        return TripStyleResponse(
            tripStyle=TripStyle(style),
            confidence=round(confidence, 3),
            topSignals=signals,
        )

    # ── ML classification ─────────────────────────────────────────────────────
    features = build_trip_style_features(
        budget=request.budget,
        travelers_count=request.travelers_count,
        duration_days=request.trip_duration_days,
        interests=request.interests,
        hotel_preference=request.hotel_preference,
    ).reshape(1, -1)

    clf = model_loader.trip_classifier
    pred_label = clf.predict(features)[0]
    proba = clf.predict_proba(features)[0]
    confidence = float(proba.max())

    # Map numeric label back to TripStyle enum
    classes = clf.classes_
    style = str(pred_label)

    # ── Top signals (feature importances × feature values) ────────────────────
    signals = _build_signals_ml(clf, features[0], request)

    logger.info("Classified trip style as %s (confidence=%.3f)", style, confidence)

    return TripStyleResponse(
        tripStyle=TripStyle(style),
        confidence=round(confidence, 3),
        topSignals=signals,
    )


def _build_signals_ml(clf, feature_vector: np.ndarray, request: TripStyleRequest) -> List[TopSignal]:
    """Generate top signals from feature importances."""
    signals = []
    try:
        importances = clf.feature_importances_
        top_indices = np.argsort(importances)[::-1][:4]
        for idx in top_indices:
            if idx < len(TRIP_STYLE_FEATURE_NAMES):
                fname = TRIP_STYLE_FEATURE_NAMES[idx]
                fval = feature_vector[idx]
                signals.append(TopSignal(
                    feature=fname,
                    value=str(round(float(fval), 2)),
                    contribution=f"{round(float(importances[idx]) * 100, 1)}%",
                ))
    except Exception:
        signals = _build_signals_rule_based(request)
    return signals


def _build_signals_rule_based(request: TripStyleRequest) -> List[TopSignal]:
    """Generate signals from raw request values for explainability."""
    budget_per_person = request.budget / max(request.travelers_count, 1)
    signals = [
        TopSignal(
            feature="budget_per_person",
            value=f"₹{budget_per_person:,.0f}",
            contribution="primary",
        ),
        TopSignal(
            feature="hotel_preference",
            value=request.hotel_preference,
            contribution="secondary",
        ),
        TopSignal(
            feature="travelers_count",
            value=str(request.travelers_count),
            contribution="secondary",
        ),
    ]
    if request.interests:
        signals.append(TopSignal(
            feature="top_interest",
            value=request.interests[0],
            contribution="supporting",
        ))
    return signals
