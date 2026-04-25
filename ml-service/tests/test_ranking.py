"""
Tests for hotel ranking service.
Tests the hybrid scoring logic without requiring trained models.
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pytest
from app.schemas.hotel_rank import HotelCandidate, HotelRankRequest
from app.services.ranking_service import rank_hotels
from app.utils.scoring import (
    price_fit_score,
    rating_score,
    distance_score,
    category_match_score,
    compute_rule_score,
    generate_reason_tags,
)


# ── Fixtures ──────────────────────────────────────────────────────────────────

def make_hotel(hotel_id, price, rating, distance, category, popularity, amenities=None):
    return HotelCandidate(
        hotelId=hotel_id,
        name=f"Hotel {hotel_id}",
        destination="Goa",
        pricePerNight=price,
        rating=rating,
        distanceFromCenterKm=distance,
        category=category,
        amenities=amenities or ["wifi", "pool"],
        popularityScore=popularity,
    )


def make_request(hotels, budget=50000, days=4, travelers=2, preference="STANDARD"):
    return HotelRankRequest(
        destination="Goa",
        budget=budget,
        tripDurationDays=days,
        travelersCount=travelers,
        hotelPreference=preference,
        interests=["beaches", "food"],
        candidateHotels=hotels,
    )


# ── Unit tests: scoring functions ─────────────────────────────────────────────

class TestScoringFunctions:

    def test_price_fit_within_budget(self):
        # Hotel at ₹3000/night for 4 days = ₹12000 total
        # 35% of ₹50000 = ₹17500 → well within budget
        score = price_fit_score(3000, 50000, 4)
        assert score == 1.0

    def test_price_fit_over_budget(self):
        # Hotel at ₹20000/night for 4 days = ₹80000
        # 35% of ₹50000 = ₹17500 → over budget
        score = price_fit_score(20000, 50000, 4)
        assert score < 1.0
        assert score >= 0.0

    def test_rating_score_max(self):
        assert rating_score(5.0) == 1.0

    def test_rating_score_mid(self):
        score = rating_score(4.0)
        assert abs(score - 0.8) < 0.001

    def test_distance_score_zero(self):
        assert distance_score(0.0) == 1.0

    def test_distance_score_far(self):
        score = distance_score(10.0)
        assert score == 0.0

    def test_category_match_exact(self):
        assert category_match_score("STANDARD", "STANDARD") == 1.0

    def test_category_match_adjacent(self):
        assert category_match_score("BUDGET", "STANDARD") == 0.5

    def test_category_match_opposite(self):
        assert category_match_score("BUDGET", "LUXURY") == 0.0

    def test_rule_score_returns_dict(self):
        sub = compute_rule_score(
            price_per_night=5000,
            rating=4.2,
            distance_km=2.0,
            category="STANDARD",
            amenities=["wifi", "pool", "restaurant"],
            popularity=8.0,
            budget=50000,
            duration_days=4,
            preference="STANDARD",
        )
        assert "composite" in sub
        assert 0.0 <= sub["composite"] <= 1.0

    def test_reason_tags_high_rating(self):
        sub = {"price_fit": 0.8, "rating": 0.9, "distance": 0.5,
               "category_match": 0.5, "amenities_match": 0.5, "popularity": 0.5}
        tags = generate_reason_tags(sub)
        assert "high_rating" in tags
        assert "budget_fit" in tags

    def test_reason_tags_feedback(self):
        sub = {"price_fit": 0.5, "rating": 0.5, "distance": 0.5,
               "category_match": 0.5, "amenities_match": 0.5, "popularity": 0.5}
        tags = generate_reason_tags(sub, feedback_reason="CHEAPER")
        assert "feedback_cheaper" in tags


# ── Integration tests: ranking service ───────────────────────────────────────

class TestRankingService:

    def test_empty_candidates_returns_empty(self):
        request = make_request([])
        response = rank_hotels(request)
        assert response.ranked_hotels == []

    def test_single_hotel_ranked_first(self):
        hotels = [make_hotel(1, 5000, 4.2, 1.5, "STANDARD", 8.0)]
        request = make_request(hotels)
        response = rank_hotels(request)
        assert len(response.ranked_hotels) == 1
        assert response.ranked_hotels[0].hotel_id == 1
        assert response.ranked_hotels[0].rank == 1

    def test_better_hotel_ranks_higher(self):
        """A hotel with better rating and closer distance should rank higher."""
        good = make_hotel(1, 5000, 4.8, 0.5, "STANDARD", 9.0, ["wifi", "pool", "spa", "restaurant"])
        bad = make_hotel(2, 5000, 3.2, 8.0, "STANDARD", 5.0, ["wifi"])
        request = make_request([bad, good])  # bad first in input
        response = rank_hotels(request)
        # Good hotel should be rank 1
        rank_map = {r.hotel_id: r.rank for r in response.ranked_hotels}
        assert rank_map[1] < rank_map[2]

    def test_cheaper_hotel_ranks_higher_for_budget_preference(self):
        """For BUDGET preference, cheaper hotel should score better."""
        cheap = make_hotel(1, 1500, 3.8, 2.0, "BUDGET", 7.0)
        expensive = make_hotel(2, 15000, 4.5, 1.0, "LUXURY", 9.0)
        request = make_request([cheap, expensive], budget=20000, preference="BUDGET")
        response = rank_hotels(request)
        rank_map = {r.hotel_id: r.rank for r in response.ranked_hotels}
        # Cheap hotel should rank better for budget preference
        assert rank_map[1] <= rank_map[2]

    def test_scores_are_normalized(self):
        hotels = [
            make_hotel(1, 5000, 4.2, 1.5, "STANDARD", 8.0),
            make_hotel(2, 8000, 4.5, 2.0, "STANDARD", 8.5),
            make_hotel(3, 2000, 3.5, 3.0, "BUDGET", 6.0),
        ]
        request = make_request(hotels)
        response = rank_hotels(request)
        for r in response.ranked_hotels:
            assert 0.0 <= r.score <= 1.0

    def test_ranks_are_sequential(self):
        hotels = [make_hotel(i, 5000 + i * 1000, 4.0, float(i), "STANDARD", 8.0)
                  for i in range(1, 5)]
        request = make_request(hotels)
        response = rank_hotels(request)
        ranks = sorted([r.rank for r in response.ranked_hotels])
        assert ranks == list(range(1, len(hotels) + 1))

    def test_reason_tags_present(self):
        hotels = [make_hotel(1, 3000, 4.8, 0.5, "STANDARD", 9.0,
                             ["wifi", "pool", "spa", "restaurant", "gym"])]
        request = make_request(hotels)
        response = rank_hotels(request)
        assert isinstance(response.ranked_hotels[0].reason_tags, list)

    def test_destination_in_response(self):
        hotels = [make_hotel(1, 5000, 4.0, 2.0, "STANDARD", 7.5)]
        request = make_request(hotels)
        response = rank_hotels(request)
        assert response.destination == "Goa"
