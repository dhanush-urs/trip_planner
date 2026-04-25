"""
Tests for trip style classification service.
Tests the rule-based fallback (no model required) and output validity.
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import pytest
from app.schemas.trip_style import TripStyleRequest
from app.services.classifier_service import classify_trip_style
from app.utils.enums import TripStyle


def make_request(budget, travelers, days, interests, preference="STANDARD"):
    return TripStyleRequest(
        budget=budget,
        travelersCount=travelers,
        tripDurationDays=days,
        interests=interests,
        hotelPreference=preference,
    )


class TestClassifierService:

    def test_returns_valid_trip_style(self):
        """Output trip_style must be a valid TripStyle enum value."""
        request = make_request(50000, 2, 5, ["nature", "adventure"])
        response = classify_trip_style(request)
        assert response.trip_style in list(TripStyle)

    def test_confidence_in_range(self):
        """Confidence must be between 0 and 1."""
        request = make_request(50000, 2, 5, ["nature"])
        response = classify_trip_style(request)
        assert 0.0 <= response.confidence <= 1.0

    def test_top_signals_present(self):
        """top_signals must be a non-empty list."""
        request = make_request(50000, 2, 5, ["nature"])
        response = classify_trip_style(request)
        assert isinstance(response.top_signals, list)
        assert len(response.top_signals) > 0

    def test_luxury_classification(self):
        """High budget + LUXURY preference should classify as LUXURY."""
        request = make_request(200000, 2, 5, ["food", "beaches"], "LUXURY")
        response = classify_trip_style(request)
        assert response.trip_style == TripStyle.LUXURY

    def test_budget_classification(self):
        """Low budget + BUDGET preference should classify as BUDGET or CULTURAL
        (nature/food interests can overlap with CULTURAL at low budgets)."""
        request = make_request(10000, 1, 3, ["food", "nature"], "BUDGET")
        response = classify_trip_style(request)
        assert response.trip_style in (TripStyle.BUDGET, TripStyle.CULTURAL)

    def test_strict_budget_classification(self):
        """Very low budget with no cultural interests should classify as BUDGET."""
        request = make_request(8000, 1, 2, ["shopping"], "BUDGET")
        response = classify_trip_style(request)
        assert response.trip_style == TripStyle.BUDGET

    def test_nightlife_classification(self):
        """Nightlife interest should classify as NIGHTLIFE."""
        request = make_request(40000, 3, 4, ["nightlife", "beaches"], "STANDARD")
        response = classify_trip_style(request)
        assert response.trip_style == TripStyle.NIGHTLIFE

    def test_adventure_classification(self):
        """Adventure interest with longer trip should classify as ADVENTURE."""
        request = make_request(50000, 2, 7, ["adventure", "nature"], "STANDARD")
        response = classify_trip_style(request)
        assert response.trip_style == TripStyle.ADVENTURE

    def test_family_classification(self):
        """Large group should classify as FAMILY."""
        request = make_request(80000, 5, 6, ["nature", "temples"], "STANDARD")
        response = classify_trip_style(request)
        assert response.trip_style == TripStyle.FAMILY

    def test_cultural_classification(self):
        """Temples + nature interest should classify as CULTURAL."""
        request = make_request(25000, 2, 4, ["temples", "nature"], "BUDGET")
        response = classify_trip_style(request)
        assert response.trip_style == TripStyle.CULTURAL

    def test_top_signals_have_required_fields(self):
        """Each signal must have feature, value, and contribution fields."""
        request = make_request(50000, 2, 5, ["adventure"])
        response = classify_trip_style(request)
        for signal in response.top_signals:
            assert hasattr(signal, "feature")
            assert hasattr(signal, "value")
            assert hasattr(signal, "contribution")

    def test_empty_interests(self):
        """Empty interests list should not crash."""
        request = make_request(30000, 2, 4, [])
        response = classify_trip_style(request)
        assert response.trip_style in list(TripStyle)
