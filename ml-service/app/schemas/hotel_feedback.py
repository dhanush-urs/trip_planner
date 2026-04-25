"""
Pydantic schemas for the alternative hotel re-ranking endpoint.
"""
from pydantic import BaseModel, Field
from typing import List, Optional
from app.utils.enums import FeedbackReason
from app.schemas.hotel_rank import HotelCandidate


class AlternativeHotelRequest(BaseModel):
    current_hotel_id: int = Field(..., alias="currentHotelId")
    feedback_reason: FeedbackReason = Field(..., alias="feedbackReason")
    destination: str
    budget: float
    trip_duration_days: int = Field(..., alias="tripDurationDays")
    travelers_count: int = Field(..., alias="travelersCount")
    hotel_preference: str = Field(default="STANDARD", alias="hotelPreference")
    candidate_hotels: List[HotelCandidate] = Field(..., alias="candidateHotels")

    model_config = {"populate_by_name": True}


class AlternativeHotel(BaseModel):
    hotel_id: int = Field(..., alias="hotelId")
    score: float
    rank: int
    reason_tags: List[str] = Field(default_factory=list, alias="reasonTags")
    feedback_applied: str = Field(..., alias="feedbackApplied")

    model_config = {"populate_by_name": True}


class AlternativeHotelResponse(BaseModel):
    alternatives: List[AlternativeHotel]
    feedback_reason: str = Field(..., alias="feedbackReason")
    current_hotel_id: int = Field(..., alias="currentHotelId")

    model_config = {"populate_by_name": True}
