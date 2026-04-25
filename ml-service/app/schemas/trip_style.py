"""
Pydantic schemas for the trip style classification endpoint.
"""
from pydantic import BaseModel, Field
from typing import List, Optional
from app.utils.enums import TripStyle


class TripStyleRequest(BaseModel):
    budget: float
    travelers_count: int = Field(..., alias="travelersCount")
    trip_duration_days: int = Field(..., alias="tripDurationDays")
    interests: List[str] = Field(default_factory=list)
    hotel_preference: str = Field(default="STANDARD", alias="hotelPreference")

    model_config = {"populate_by_name": True}


class TopSignal(BaseModel):
    feature: str
    value: str
    contribution: str


class TripStyleResponse(BaseModel):
    trip_style: TripStyle = Field(..., alias="tripStyle")
    confidence: float
    top_signals: List[TopSignal] = Field(default_factory=list, alias="topSignals")

    model_config = {"populate_by_name": True}
