"""
Generate synthetic hotel ranking training data.

Strategy:
  - For each hotel in hotels_master.csv, simulate multiple trip contexts
    (varying budget, duration, travelers, preference)
  - Compute a realistic relevance label using a weighted rule formula
  - Add controlled noise to prevent overfitting
  - Save to training/datasets/hotel_training_data.csv

Run: python training/generate_hotel_training_data.py
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import numpy as np
import pandas as pd
from pathlib import Path

# Deterministic seed for reproducibility
RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)

OUTPUT_PATH = Path(__file__).parent / "datasets" / "hotel_training_data.csv"
MASTER_PATH = Path(__file__).parent / "datasets" / "hotels_master.csv"

# Trip context simulation parameters
BUDGETS = [15000, 25000, 40000, 60000, 80000, 120000, 200000]
DURATIONS = [2, 3, 4, 5, 7]
TRAVELERS = [1, 2, 3, 4, 6]
PREFERENCES = ["BUDGET", "STANDARD", "LUXURY"]

DESTINATION_ENCODING = {
    "goa": 0, "mysore": 1, "bangalore": 2, "ooty": 3, "manali": 4
}
PREFERENCE_ENCODING = {"BUDGET": 0, "STANDARD": 1, "LUXURY": 2}
CATEGORY_ENCODING = {"BUDGET": 0, "STANDARD": 1, "LUXURY": 2}


def price_fit(price, budget, days):
    hotel_budget = budget * 0.35
    total = price * days
    return min(1.0, max(0.0, hotel_budget / max(total, 1)))


def rating_norm(r): return r / 5.0


def dist_score(d): return max(0.0, 1.0 - d / 10.0)


def cat_match(cat, pref):
    order = {"BUDGET": 0, "STANDARD": 1, "LUXURY": 2}
    diff = abs(order.get(cat, 1) - order.get(pref, 1))
    return [1.0, 0.5, 0.0][diff]


def amenity_count(amenities_str):
    return len(amenities_str.split(",")) if amenities_str else 0


def compute_label(row, budget, days, pref):
    """
    Compute a realistic relevance label in [0, 1].
    This is the ground truth the model learns to predict.
    """
    pf = price_fit(row["price_per_night"], budget, days)
    rs = rating_norm(row["rating"])
    ds = dist_score(row["distance_from_center_km"])
    cm = cat_match(row["category"], pref)
    am = min(1.0, amenity_count(row["amenities"]) / 6.0)
    ps = row["popularity_score"] / 10.0

    label = (
        pf * 0.25
        + rs * 0.30
        + ds * 0.15
        + cm * 0.15
        + am * 0.05
        + ps * 0.10
    )
    # Add small Gaussian noise to simulate real-world variance
    noise = np.random.normal(0, 0.03)
    return float(np.clip(label + noise, 0.0, 1.0))


def main():
    hotels = pd.read_csv(MASTER_PATH)
    records = []

    for _, hotel in hotels.iterrows():
        for budget in BUDGETS:
            for days in DURATIONS:
                for travelers in TRAVELERS:
                    for pref in PREFERENCES:
                        budget_per_day = budget / days
                        pf = price_fit(hotel["price_per_night"], budget, days)
                        rs = rating_norm(hotel["rating"])
                        ds = dist_score(hotel["distance_from_center_km"])
                        cm = cat_match(hotel["category"], pref)
                        am = min(1.0, amenity_count(hotel["amenities"]) / 6.0)
                        ps = hotel["popularity_score"] / 10.0
                        price_fit_ratio = hotel["price_per_night"] / max(budget_per_day, 1)

                        label = compute_label(hotel, budget, days, pref)

                        records.append({
                            "hotel_id": hotel["hotel_id"],
                            "destination": hotel["destination"],
                            "budget_per_day": budget_per_day,
                            "duration_days": days,
                            "travelers_count": travelers,
                            "price_per_night": hotel["price_per_night"],
                            "rating": hotel["rating"],
                            "distance_from_center_km": hotel["distance_from_center_km"],
                            "destination_encoded": DESTINATION_ENCODING.get(
                                hotel["destination"].lower(), -1),
                            "preference_encoded": PREFERENCE_ENCODING.get(pref, 1),
                            "category_encoded": CATEGORY_ENCODING.get(hotel["category"], 1),
                            "popularity_score": hotel["popularity_score"],
                            "price_fit_score": round(pf, 4),
                            "rating_score": round(rs, 4),
                            "distance_score": round(ds, 4),
                            "category_match_score": round(cm, 4),
                            "amenities_match_score": round(am, 4),
                            "price_fit_ratio": round(price_fit_ratio, 4),
                            "relevance_label": round(label, 4),
                        })

    df = pd.DataFrame(records)
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(OUTPUT_PATH, index=False)
    print(f"Generated {len(df)} training samples → {OUTPUT_PATH}")
    print(f"Label stats:\n{df['relevance_label'].describe()}")


if __name__ == "__main__":
    main()
