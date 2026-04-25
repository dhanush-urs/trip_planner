"""
Pydantic schemas for the hotel ranking endpoint.
"""
from pydantic import BaseModel, Field
from typing import List, Optional


class HotelCandidate(BaseModel):
    """A hotel candidate passed in from hotel-service."""
    hotel_id: int = Field(..., alias="hotelId")
    name: str
    destination: str
    price_per_night: float = Field(..., alias="pricePerNight")
    rating: float
    distance_from_center_km: float = Field(..., alias="distanceFromCenterKm")
    category: str  # BUDGET | STANDARD | LUXURY
    amenities: List[str] = Field(default_factory=list)
    popularity_score: float = Field(..., alias="popularityScore")

    model_config = {"populate_by_name": True}


class HotelRankRequest(BaseModel):
    destination: str
    budget: float
    trip_duration_days: int = Field(..., alias="tripDurationDays")
    travelers_count: int = Field(..., alias="travelersCount")
    hotel_preference: str = Field(default="STANDARD", alias="hotelPreference")
    interests: Optional[List[str]] = Field(default_factory=list)
    candidate_hotels: List[HotelCandidate] = Field(..., alias="candidateHotels")

    model_config = {"populate_by_name": True}


class RankedHotel(BaseModel):
    hotel_id: int = Field(..., alias="hotelId")
    score: float
    rank: int
    reason_tags: List[str] = Field(default_factory=list, alias="reasonTags")

    model_config = {"populate_by_name": True, "populate_by_name": True}


class HotelRankResponse(BaseModel):
    ranked_hotels: List[RankedHotel] = Field(..., alias="rankedHotels")
    model_used: str = Field(default="hybrid", alias="modelUsed")
    destination: str

    model_config = {"populate_by_name": True}
