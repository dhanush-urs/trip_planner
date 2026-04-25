"""
Generate synthetic trip style classification training data.

Classes: BUDGET, FAMILY, NIGHTLIFE, ADVENTURE, LUXURY, CULTURAL

Strategy:
  - Define clear decision rules for each class
  - Sample parameters from realistic ranges per class
  - Add noise to prevent perfect separation
  - Save to training/datasets/trip_style_training_data.csv

Run: python training/generate_trip_style_data.py
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import numpy as np
import pandas as pd
from pathlib import Path

RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)

OUTPUT_PATH = Path(__file__).parent / "datasets" / "trip_style_training_data.csv"
SAMPLES_PER_CLASS = 400

ALL_INTERESTS = ["nightlife", "adventure", "nature", "food", "temples", "shopping", "beaches"]
PREFERENCE_ENCODING = {"BUDGET": 0, "STANDARD": 1, "LUXURY": 2}


def sample_interests(primary: list, secondary: list, n_secondary: int = 1) -> list:
    """Sample interests with primary ones always included."""
    chosen = list(primary)
    extras = np.random.choice(secondary, size=min(n_secondary, len(secondary)), replace=False)
    chosen.extend(extras.tolist())
    return list(set(chosen))


def build_features(budget, travelers, days, interests, pref):
    interests_lower = [i.lower() for i in interests]
    pref_enc = PREFERENCE_ENCODING.get(pref, 1)
    budget_per_person = budget / max(travelers, 1)
    flags = [1.0 if i in interests_lower else 0.0 for i in ALL_INTERESTS]
    return {
        "budget": budget,
        "budget_per_person": budget_per_person,
        "travelers_count": travelers,
        "duration_days": days,
        "preference_encoded": pref_enc,
        "has_nightlife": flags[0],
        "has_adventure": flags[1],
        "has_nature": flags[2],
        "has_food": flags[3],
        "has_temples": flags[4],
        "has_shopping": flags[5],
        "has_beaches": flags[6],
        "interest_count": float(len(interests)),
        "trip_style": None,  # filled below
    }


def generate_class(label: str, n: int) -> list:
    records = []
    for _ in range(n):
        if label == "BUDGET":
            budget = np.random.uniform(8000, 20000)
            travelers = np.random.randint(1, 3)
            days = np.random.randint(2, 5)
            pref = "BUDGET"
            interests = sample_interests(["food", "nature"], ["shopping", "temples"], 1)

        elif label == "FAMILY":
            budget = np.random.uniform(30000, 80000)
            travelers = np.random.randint(3, 7)
            days = np.random.randint(4, 8)
            pref = np.random.choice(["STANDARD", "STANDARD", "LUXURY"])
            interests = sample_interests(["nature", "temples"], ["food", "shopping"], 2)

        elif label == "NIGHTLIFE":
            budget = np.random.uniform(20000, 60000)
            travelers = np.random.randint(2, 5)
            days = np.random.randint(3, 6)
            pref = np.random.choice(["STANDARD", "LUXURY"])
            interests = sample_interests(["nightlife", "beaches"], ["food", "shopping"], 1)

        elif label == "ADVENTURE":
            budget = np.random.uniform(25000, 70000)
            travelers = np.random.randint(2, 5)
            days = np.random.randint(4, 9)
            pref = np.random.choice(["BUDGET", "STANDARD"])
            interests = sample_interests(["adventure", "nature"], ["food"], 1)

        elif label == "LUXURY":
            budget = np.random.uniform(80000, 250000)
            travelers = np.random.randint(1, 4)
            days = np.random.randint(3, 8)
            pref = "LUXURY"
            interests = sample_interests(["food", "beaches"], ["shopping", "nature"], 2)

        elif label == "CULTURAL":
            budget = np.random.uniform(15000, 50000)
            travelers = np.random.randint(1, 4)
            days = np.random.randint(3, 7)
            pref = np.random.choice(["BUDGET", "STANDARD"])
            interests = sample_interests(["temples", "nature"], ["food", "shopping"], 2)

        else:
            continue

        row = build_features(budget, travelers, days, interests, pref)
        row["trip_style"] = label
        records.append(row)
    return records


def main():
    all_records = []
    for label in ["BUDGET", "FAMILY", "NIGHTLIFE", "ADVENTURE", "LUXURY", "CULTURAL"]:
        records = generate_class(label, SAMPLES_PER_CLASS)
        all_records.extend(records)
        print(f"Generated {len(records)} samples for class: {label}")

    df = pd.DataFrame(all_records)
    # Shuffle
    df = df.sample(frac=1, random_state=RANDOM_SEED).reset_index(drop=True)

    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    df.to_csv(OUTPUT_PATH, index=False)
    print(f"\nTotal: {len(df)} samples → {OUTPUT_PATH}")
    print(f"Class distribution:\n{df['trip_style'].value_counts()}")


if __name__ == "__main__":
    main()
