"""
Shared enumerations used across schemas and services.
"""
from enum import Enum


class HotelCategory(str, Enum):
    BUDGET = "BUDGET"
    STANDARD = "STANDARD"
    LUXURY = "LUXURY"


class HotelPreference(str, Enum):
    BUDGET = "BUDGET"
    STANDARD = "STANDARD"
    LUXURY = "LUXURY"


class FeedbackReason(str, Enum):
    CHEAPER = "CHEAPER"
    BETTER_RATING = "BETTER_RATING"
    CLOSER = "CLOSER"
    PREMIUM = "PREMIUM"


class TripStyle(str, Enum):
    BUDGET = "BUDGET"
    FAMILY = "FAMILY"
    NIGHTLIFE = "NIGHTLIFE"
    ADVENTURE = "ADVENTURE"
    LUXURY = "LUXURY"
    CULTURAL = "CULTURAL"


# Destination encoding map (used in feature building)
DESTINATION_ENCODING: dict[str, int] = {
    "goa": 0,
    "mysore": 1,
    "bangalore": 2,
    "ooty": 3,
    "manali": 4,
}

# Hotel preference encoding
PREFERENCE_ENCODING: dict[str, int] = {
    "BUDGET": 0,
    "STANDARD": 1,
    "LUXURY": 2,
}

# Category encoding
CATEGORY_ENCODING: dict[str, int] = {
    "BUDGET": 0,
    "STANDARD": 1,
    "LUXURY": 2,
}

# Known amenities for match scoring
KNOWN_AMENITIES = [
    "pool", "wifi", "spa", "restaurant", "gym",
    "beach_access", "parking", "fireplace", "garden",
    "common_kitchen", "bar", "ski_access", "adventure_activities",
]
