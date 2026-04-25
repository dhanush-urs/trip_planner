"""
Feature builder — converts raw request data into numeric feature vectors
for the ML models.

Feature order must match the training pipeline exactly.
The canonical feature order is stored in saved_models/feature_metadata.json.
"""
import numpy as np
from typing import List

from app.utils.enums import DESTINATION_ENCODING, PREFERENCE_ENCODING, CATEGORY_ENCODING
from app.utils.scoring import (
    price_fit_score,
    rating_score,
    distance_score,
    category_match_score,
    amenities_match_score,
    popularity_score_normalized,
)


# ── Hotel Ranker Features ─────────────────────────────────────────────────────

HOTEL_FEATURE_NAMES = [
    "budget_per_day",
    "duration_days",
    "travelers_count",
    "price_per_night",
    "rating",
    "distance_from_center_km",
    "destination_encoded",
    "preference_encoded",
    "category_encoded",
    "popularity_score",
    "price_fit_score",
    "rating_score",
    "distance_score",
    "category_match_score",
    "amenities_match_score",
    "price_fit_ratio",
]


def build_hotel_features(
    budget: float,
    duration_days: int,
    travelers_count: int,
    price_per_night: float,
    rating: float,
    distance_km: float,
    destination: str,
    preference: str,
    category: str,
    amenities: List[str],
    popularity: float,
) -> np.ndarray:
    """
    Build a 1D feature vector for a single hotel candidate.
    Returns shape (16,) matching HOTEL_FEATURE_NAMES.
    """
    dest_enc = DESTINATION_ENCODING.get(destination.lower(), -1)
    pref_enc = PREFERENCE_ENCODING.get(preference.upper(), 1)
    cat_enc = CATEGORY_ENCODING.get(category.upper(), 1)

    budget_per_day = budget / max(duration_days, 1)
    price_fit = price_fit_score(price_per_night, budget, duration_days)
    r_score = rating_score(rating)
    d_score = distance_score(distance_km)
    cm_score = category_match_score(category, preference)
    am_score = amenities_match_score(amenities, preference)
    pop_norm = popularity_score_normalized(popularity)

    # Price fit ratio: how much of daily budget goes to hotel
    price_fit_ratio = price_per_night / max(budget_per_day, 1.0)

    features = np.array([
        budget_per_day,
        float(duration_days),
        float(travelers_count),
        price_per_night,
        rating,
        distance_km,
        float(dest_enc),
        float(pref_enc),
        float(cat_enc),
        popularity,
        price_fit,
        r_score,
        d_score,
        cm_score,
        am_score,
        price_fit_ratio,
    ], dtype=np.float64)

    return features


def build_hotel_feature_matrix(
    budget: float,
    duration_days: int,
    travelers_count: int,
    destination: str,
    preference: str,
    hotels: list,  # list of HotelCandidate
) -> np.ndarray:
    """
    Build a 2D feature matrix for a list of hotel candidates.
    Returns shape (n_hotels, 16).
    """
    rows = []
    for h in hotels:
        row = build_hotel_features(
            budget=budget,
            duration_days=duration_days,
            travelers_count=travelers_count,
            price_per_night=h.price_per_night,
            rating=h.rating,
            distance_km=h.distance_from_center_km,
            destination=destination,
            preference=preference,
            category=h.category,
            amenities=h.amenities,
            popularity=h.popularity_score,
        )
        rows.append(row)
    return np.vstack(rows)


# ── Trip Style Classifier Features ───────────────────────────────────────────

TRIP_STYLE_FEATURE_NAMES = [
    "budget",
    "budget_per_person",
    "travelers_count",
    "duration_days",
    "preference_encoded",
    "has_nightlife",
    "has_adventure",
    "has_nature",
    "has_food",
    "has_temples",
    "has_shopping",
    "has_beaches",
    "interest_count",
]

ALL_INTERESTS = ["nightlife", "adventure", "nature", "food", "temples", "shopping", "beaches"]


def build_trip_style_features(
    budget: float,
    travelers_count: int,
    duration_days: int,
    interests: List[str],
    hotel_preference: str,
) -> np.ndarray:
    """
    Build a 1D feature vector for trip style classification.
    Returns shape (13,) matching TRIP_STYLE_FEATURE_NAMES.
    """
    pref_enc = PREFERENCE_ENCODING.get(hotel_preference.upper(), 1)
    budget_per_person = budget / max(travelers_count, 1)
    interests_lower = [i.lower() for i in interests]

    interest_flags = [1.0 if i in interests_lower else 0.0 for i in ALL_INTERESTS]

    features = np.array([
        budget,
        budget_per_person,
        float(travelers_count),
        float(duration_days),
        float(pref_enc),
        *interest_flags,
        float(len(interests)),
    ], dtype=np.float64)

    return features
