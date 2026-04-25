"""
Train the hotel ranking model.

Model: GradientBoostingRegressor (scikit-learn)
Task: Regression — predict hotel relevance score in [0, 1]
Features: 16 numeric features (see feature_builder.py)
Output: saved_models/hotel_ranker.pkl + feature_metadata.json

Run: python training/train_hotel_ranker.py
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import json
import numpy as np
import pandas as pd
import joblib
from pathlib import Path
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.model_selection import train_test_split
from sklearn.metrics import mean_absolute_error, r2_score
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline

RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)

DATA_PATH = Path(__file__).parent / "datasets" / "hotel_training_data.csv"
MODEL_PATH = Path(__file__).parent.parent / "saved_models" / "hotel_ranker.pkl"
METADATA_PATH = Path(__file__).parent.parent / "saved_models" / "feature_metadata.json"

FEATURE_COLS = [
    "budget_per_day",
    "duration_days",
    "travelers_count",
    "price_per_night",
    "rating",
    "distance_from_center_km",
    "destination_encoded",
    "preference_encoded",
    "category_encoded",
    "popularity_score",
    "price_fit_score",
    "rating_score",
    "distance_score",
    "category_match_score",
    "amenities_match_score",
    "price_fit_ratio",
]
TARGET_COL = "relevance_label"


def main():
    # ── Load data ─────────────────────────────────────────────────────────────
    if not DATA_PATH.exists():
        print(f"Training data not found at {DATA_PATH}")
        print("Run: python training/generate_hotel_training_data.py")
        sys.exit(1)

    df = pd.read_csv(DATA_PATH)
    print(f"Loaded {len(df)} training samples")

    X = df[FEATURE_COLS].values
    y = df[TARGET_COL].values

    # ── Train/test split ──────────────────────────────────────────────────────
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=RANDOM_SEED
    )
    print(f"Train: {len(X_train)} | Test: {len(X_test)}")

    # ── Build pipeline ────────────────────────────────────────────────────────
    # StandardScaler + GradientBoostingRegressor
    pipeline = Pipeline([
        ("scaler", StandardScaler()),
        ("model", GradientBoostingRegressor(
            n_estimators=200,
            learning_rate=0.08,
            max_depth=4,
            min_samples_split=10,
            min_samples_leaf=5,
            subsample=0.85,
            random_state=RANDOM_SEED,
            verbose=0,
        )),
    ])

    # ── Train ─────────────────────────────────────────────────────────────────
    print("Training GradientBoostingRegressor...")
    pipeline.fit(X_train, y_train)

    # ── Evaluate ──────────────────────────────────────────────────────────────
    y_pred = pipeline.predict(X_test)
    mae = mean_absolute_error(y_test, y_pred)
    r2 = r2_score(y_test, y_pred)
    print(f"\nTest Metrics:")
    print(f"  MAE : {mae:.4f}")
    print(f"  R²  : {r2:.4f}")

    # Feature importances (from the GBR inside the pipeline)
    gbr = pipeline.named_steps["model"]
    importances = gbr.feature_importances_
    feat_imp = sorted(
        zip(FEATURE_COLS, importances), key=lambda x: x[1], reverse=True
    )
    print("\nTop 5 Feature Importances:")
    for fname, imp in feat_imp[:5]:
        print(f"  {fname:<35} {imp:.4f}")

    # ── Save model ────────────────────────────────────────────────────────────
    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(pipeline, MODEL_PATH)
    print(f"\nModel saved → {MODEL_PATH}")

    # ── Save metadata ─────────────────────────────────────────────────────────
    metadata = {
        "feature_names": FEATURE_COLS,
        "target": TARGET_COL,
        "model_type": "GradientBoostingRegressor",
        "n_features": len(FEATURE_COLS),
        "train_samples": len(X_train),
        "test_mae": round(mae, 4),
        "test_r2": round(r2, 4),
        "feature_importances": {k: round(float(v), 4) for k, v in feat_imp},
        "random_seed": RANDOM_SEED,
    }
    with open(METADATA_PATH, "w") as f:
        json.dump(metadata, f, indent=2)
    print(f"Metadata saved → {METADATA_PATH}")


if __name__ == "__main__":
    main()
