"""
Train the trip style classifier.

Model: RandomForestClassifier (scikit-learn)
Task: Multi-class classification — predict trip style
Classes: BUDGET, FAMILY, NIGHTLIFE, ADVENTURE, LUXURY, CULTURAL
Features: 13 numeric features (see feature_builder.py)
Output: saved_models/trip_style_classifier.pkl

Run: python training/train_trip_classifier.py
"""
import sys
import os
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

import numpy as np
import pandas as pd
import joblib
from pathlib import Path
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split, cross_val_score
from sklearn.metrics import accuracy_score, classification_report
from sklearn.preprocessing import StandardScaler
from sklearn.pipeline import Pipeline

RANDOM_SEED = 42
np.random.seed(RANDOM_SEED)

DATA_PATH = Path(__file__).parent / "datasets" / "trip_style_training_data.csv"
MODEL_PATH = Path(__file__).parent.parent / "saved_models" / "trip_style_classifier.pkl"

FEATURE_COLS = [
    "budget",
    "budget_per_person",
    "travelers_count",
    "duration_days",
    "preference_encoded",
    "has_nightlife",
    "has_adventure",
    "has_nature",
    "has_food",
    "has_temples",
    "has_shopping",
    "has_beaches",
    "interest_count",
]
TARGET_COL = "trip_style"


def main():
    # ── Load data ─────────────────────────────────────────────────────────────
    if not DATA_PATH.exists():
        print(f"Training data not found at {DATA_PATH}")
        print("Run: python training/generate_trip_style_data.py")
        sys.exit(1)

    df = pd.read_csv(DATA_PATH)
    print(f"Loaded {len(df)} training samples")
    print(f"Class distribution:\n{df[TARGET_COL].value_counts()}\n")

    X = df[FEATURE_COLS].values
    y = df[TARGET_COL].values

    # ── Train/test split ──────────────────────────────────────────────────────
    X_train, X_test, y_train, y_test = train_test_split(
        X, y, test_size=0.2, random_state=RANDOM_SEED, stratify=y
    )
    print(f"Train: {len(X_train)} | Test: {len(X_test)}")

    # ── Build pipeline ────────────────────────────────────────────────────────
    pipeline = Pipeline([
        ("scaler", StandardScaler()),
        ("model", RandomForestClassifier(
            n_estimators=200,
            max_depth=12,
            min_samples_split=5,
            min_samples_leaf=2,
            class_weight="balanced",
            random_state=RANDOM_SEED,
            n_jobs=-1,
        )),
    ])

    # ── Train ─────────────────────────────────────────────────────────────────
    print("Training RandomForestClassifier...")
    pipeline.fit(X_train, y_train)

    # ── Evaluate ──────────────────────────────────────────────────────────────
    y_pred = pipeline.predict(X_test)
    acc = accuracy_score(y_test, y_pred)
    print(f"\nTest Accuracy: {acc:.4f}")
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred))

    # Cross-validation
    cv_scores = cross_val_score(pipeline, X, y, cv=5, scoring="accuracy")
    print(f"5-Fold CV Accuracy: {cv_scores.mean():.4f} ± {cv_scores.std():.4f}")

    # Feature importances
    rf = pipeline.named_steps["model"]
    feat_imp = sorted(
        zip(FEATURE_COLS, rf.feature_importances_), key=lambda x: x[1], reverse=True
    )
    print("\nTop 5 Feature Importances:")
    for fname, imp in feat_imp[:5]:
        print(f"  {fname:<30} {imp:.4f}")

    # ── Save model ────────────────────────────────────────────────────────────
    MODEL_PATH.parent.mkdir(parents=True, exist_ok=True)
    joblib.dump(pipeline, MODEL_PATH)
    print(f"\nModel saved → {MODEL_PATH}")


if __name__ == "__main__":
    main()
