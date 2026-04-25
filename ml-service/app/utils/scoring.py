"""
Rule-based interpretable scoring helpers.

Each function returns a normalized score in [0, 1].
These are used both for the hybrid ranking and for generating reasonTags.
"""
from typing import List


def price_fit_score(price_per_night: float, budget: float, duration_days: int) -> float:
    """
    How well does the hotel price fit within the trip budget?
    Assumes hotel should consume at most 35% of total budget.
    Score = 1.0 when price is well within budget, decays toward 0 as it exceeds.
    """
    if duration_days <= 0 or budget <= 0:
        return 0.0
    hotel_budget = budget * 0.35
    total_hotel_cost = price_per_night * duration_days
    if total_hotel_cost <= 0:
        return 1.0
    ratio = hotel_budget / total_hotel_cost
    # Clamp to [0, 1]
    return min(1.0, max(0.0, ratio))


def rating_score(rating: float, max_rating: float = 5.0) -> float:
    """Normalize hotel rating to [0, 1]."""
    if max_rating <= 0:
        return 0.0
    return min(1.0, max(0.0, rating / max_rating))


def distance_score(distance_km: float, max_distance_km: float = 10.0) -> float:
    """
    Inverse distance score — closer is better.
    Score = 1.0 at 0 km, decays to 0 at max_distance_km.
    """
    if distance_km < 0:
        return 0.0
    return max(0.0, 1.0 - (distance_km / max_distance_km))


def category_match_score(hotel_category: str, preference: str) -> float:
    """
    How well does the hotel category match the user's preference?
    Exact match = 1.0, adjacent = 0.5, opposite = 0.0
    """
    order = {"BUDGET": 0, "STANDARD": 1, "LUXURY": 2}
    h = order.get(hotel_category.upper(), 1)
    p = order.get(preference.upper(), 1)
    diff = abs(h - p)
    if diff == 0:
        return 1.0
    elif diff == 1:
        return 0.5
    else:
        return 0.0


def amenities_match_score(hotel_amenities: List[str], preference: str) -> float:
    """
    Score based on amenity richness relative to preference tier.
    LUXURY expects more amenities than BUDGET.
    """
    count = len(hotel_amenities)
    if preference.upper() == "LUXURY":
        return min(1.0, count / 6.0)
    elif preference.upper() == "STANDARD":
        return min(1.0, count / 4.0)
    else:
        return min(1.0, count / 2.0)


def popularity_score_normalized(popularity: float, max_popularity: float = 10.0) -> float:
    """Normalize popularity score to [0, 1]."""
    return min(1.0, max(0.0, popularity / max_popularity))


def compute_rule_score(
    price_per_night: float,
    rating: float,
    distance_km: float,
    category: str,
    amenities: List[str],
    popularity: float,
    budget: float,
    duration_days: int,
    preference: str,
) -> dict:
    """
    Compute all rule-based sub-scores and return as a dict.
    Also returns the weighted composite rule score.
    """
    pf = price_fit_score(price_per_night, budget, duration_days)
    rs = rating_score(rating)
    ds = distance_score(distance_km)
    cm = category_match_score(category, preference)
    am = amenities_match_score(amenities, preference)
    ps = popularity_score_normalized(popularity)

    # Weighted composite
    composite = (
        pf * 0.25
        + rs * 0.30
        + ds * 0.15
        + cm * 0.15
        + am * 0.05
        + ps * 0.10
    )

    return {
        "price_fit": round(pf, 4),
        "rating": round(rs, 4),
        "distance": round(ds, 4),
        "category_match": round(cm, 4),
        "amenities_match": round(am, 4),
        "popularity": round(ps, 4),
        "composite": round(composite, 4),
    }


def generate_reason_tags(
    sub_scores: dict,
    feedback_reason: str | None = None,
    thresholds: dict | None = None,
) -> List[str]:
    """
    Generate human-readable reason tags based on sub-score values.
    Tags are deterministic — same scores always produce same tags.
    """
    if thresholds is None:
        thresholds = {
            "price_fit": 0.70,
            "rating": 0.80,
            "distance": 0.75,
            "category_match": 0.90,
            "amenities_match": 0.60,
            "popularity": 0.75,
        }

    tags = []

    if sub_scores.get("price_fit", 0) >= thresholds["price_fit"]:
        tags.append("budget_fit")
    if sub_scores.get("rating", 0) >= thresholds["rating"]:
        tags.append("high_rating")
    if sub_scores.get("distance", 0) >= thresholds["distance"]:
        tags.append("close_to_center")
    if sub_scores.get("category_match", 0) >= thresholds["category_match"]:
        tags.append("matches_preference")
    if sub_scores.get("amenities_match", 0) >= thresholds["amenities_match"]:
        tags.append("well_equipped")
    if sub_scores.get("popularity", 0) >= thresholds["popularity"]:
        tags.append("strong_popularity")

    # Category-based tags
    # These are added by the caller based on hotel category
    # Feedback tags
    if feedback_reason:
        tag_map = {
            "CHEAPER": "feedback_cheaper",
            "BETTER_RATING": "feedback_better_rating",
            "CLOSER": "feedback_closer",
            "PREMIUM": "feedback_premium",
        }
        if feedback_reason.upper() in tag_map:
            tags.append(tag_map[feedback_reason.upper()])

    return tags
