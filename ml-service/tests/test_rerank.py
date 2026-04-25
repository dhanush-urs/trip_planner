"""
Tests for feedback-aware hotel re-ranking service.
Validates that each feedback reason produces the expected ordering behavior.
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pytest
from app.schemas.hotel_rank import HotelCandidate
from app.schemas.hotel_feedback import AlternativeHotelRequest
from app.services.rerank_service import rerank_hotels
from app.utils.enums import FeedbackReason


def make_hotel(hotel_id, price, rating, distance, category, popularity, amenities=None):
    return HotelCandidate(
        hotelId=hotel_id,
        name=f"Hotel {hotel_id}",
        destination="Goa",
        pricePerNight=price,
        rating=rating,
        distanceFromCenterKm=distance,
        category=category,
        amenities=amenities or ["wifi"],
        popularityScore=popularity,
    )


def make_rerank_request(hotels, current_id, reason, budget=50000, days=4):
    return AlternativeHotelRequest(
        currentHotelId=current_id,
        feedbackReason=FeedbackReason(reason),
        destination="Goa",
        budget=budget,
        tripDurationDays=days,
        travelersCount=2,
        hotelPreference="STANDARD",
        candidateHotels=hotels,
    )


class TestRerankService:

    def test_current_hotel_excluded(self):
        """The current hotel must not appear in alternatives."""
        hotels = [
            make_hotel(1, 5000, 4.0, 2.0, "STANDARD", 7.5),
            make_hotel(2, 3000, 3.8, 3.0, "BUDGET", 6.5),
            make_hotel(3, 8000, 4.5, 1.0, "STANDARD", 8.5),
        ]
        request = make_rerank_request(hotels, current_id=1, reason="CHEAPER")
        response = rerank_hotels(request)
        ids = [a.hotel_id for a in response.alternatives]
        assert 1 not in ids

    def test_cheaper_reason_prefers_lower_price(self):
        """CHEAPER feedback should rank the cheapest hotel first."""
        hotels = [
            make_hotel(1, 10000, 4.5, 1.0, "LUXURY", 9.0),   # current
            make_hotel(2, 1500, 3.6, 3.0, "BUDGET", 6.5),    # cheapest
            make_hotel(3, 5000, 4.0, 2.0, "STANDARD", 7.5),  # mid
        ]
        request = make_rerank_request(hotels, current_id=1, reason="CHEAPER", budget=30000)
        response = rerank_hotels(request)
        # Hotel 2 (cheapest) should rank first
        assert response.alternatives[0].hotel_id == 2

    def test_better_rating_reason_prefers_high_rating(self):
        """BETTER_RATING feedback should rank the highest-rated hotel first."""
        hotels = [
            make_hotel(1, 5000, 3.5, 2.0, "STANDARD", 7.0),  # current
            make_hotel(2, 8000, 4.9, 1.5, "LUXURY", 9.5),    # best rating
            make_hotel(3, 4000, 3.8, 2.5, "STANDARD", 7.2),  # mid rating
        ]
        request = make_rerank_request(hotels, current_id=1, reason="BETTER_RATING")
        response = rerank_hotels(request)
        assert response.alternatives[0].hotel_id == 2

    def test_closer_reason_prefers_low_distance(self):
        """CLOSER feedback should rank the nearest hotel first."""
        hotels = [
            make_hotel(1, 5000, 4.0, 5.0, "STANDARD", 7.5),  # current
            make_hotel(2, 6000, 4.1, 0.3, "STANDARD", 8.0),  # closest
            make_hotel(3, 4500, 3.9, 3.0, "STANDARD", 7.0),  # mid distance
        ]
        request = make_rerank_request(hotels, current_id=1, reason="CLOSER")
        response = rerank_hotels(request)
        assert response.alternatives[0].hotel_id == 2

    def test_premium_reason_prefers_luxury(self):
        """PREMIUM feedback should rank luxury hotels first."""
        hotels = [
            make_hotel(1, 5000, 4.0, 2.0, "STANDARD", 7.5),  # current
            make_hotel(2, 18000, 4.8, 1.5, "LUXURY", 9.5,
                       ["pool", "wifi", "spa", "restaurant", "gym", "beach_access"]),
            make_hotel(3, 2000, 3.5, 3.0, "BUDGET", 6.0),
        ]
        request = make_rerank_request(hotels, current_id=1, reason="PREMIUM")
        response = rerank_hotels(request)
        assert response.alternatives[0].hotel_id == 2

    def test_feedback_tag_present(self):
        """Each alternative should have the feedback tag in reason_tags."""
        hotels = [
            make_hotel(1, 5000, 4.0, 2.0, "STANDARD", 7.5),
            make_hotel(2, 3000, 3.8, 3.0, "BUDGET", 6.5),
        ]
        request = make_rerank_request(hotels, current_id=1, reason="CHEAPER")
        response = rerank_hotels(request)
        for alt in response.alternatives:
            assert "feedback_cheaper" in alt.reason_tags

    def test_feedback_applied_field(self):
        """feedbackApplied field must match the request reason."""
        hotels = [
            make_hotel(1, 5000, 4.0, 2.0, "STANDARD", 7.5),
            make_hotel(2, 3000, 3.8, 3.0, "BUDGET", 6.5),
        ]
        request = make_rerank_request(hotels, current_id=1, reason="BETTER_RATING")
        response = rerank_hotels(request)
        for alt in response.alternatives:
            assert alt.feedback_applied == "BETTER_RATING"

    def test_empty_candidates_after_exclusion(self):
        """If only the current hotel is in the list, return empty alternatives."""
        hotels = [make_hotel(1, 5000, 4.0, 2.0, "STANDARD", 7.5)]
        request = make_rerank_request(hotels, current_id=1, reason="CHEAPER")
        response = rerank_hotels(request)
        assert response.alternatives == []

    def test_ranks_are_sequential(self):
        hotels = [
            make_hotel(1, 5000, 4.0, 2.0, "STANDARD", 7.5),  # current
            make_hotel(2, 3000, 3.8, 3.0, "BUDGET", 6.5),
            make_hotel(3, 7000, 4.3, 1.5, "STANDARD", 8.2),
            make_hotel(4, 12000, 4.7, 1.0, "LUXURY", 9.0),
        ]
        request = make_rerank_request(hotels, current_id=1, reason="BETTER_RATING")
        response = rerank_hotels(request)
        ranks = sorted([a.rank for a in response.alternatives])
        assert ranks == list(range(1, len(response.alternatives) + 1))
