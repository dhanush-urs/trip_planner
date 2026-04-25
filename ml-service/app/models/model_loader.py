"""
Model loader — loads trained .pkl models and feature metadata on startup.
Exposes clean getters used by service layer.
Fails gracefully with clear error messages if models are missing.
"""
import json
import logging
from pathlib import Path
from typing import Any, Optional

import joblib

from app.config import settings

logger = logging.getLogger(__name__)


class ModelLoader:
    """Singleton-style model registry loaded once at app startup."""

    def __init__(self):
        self._hotel_ranker: Optional[Any] = None
        self._trip_classifier: Optional[Any] = None
        self._feature_metadata: dict = {}
        self._hotel_ranker_loaded: bool = False
        self._trip_classifier_loaded: bool = False

    def load_all(self) -> None:
        """Load all models. Called during FastAPI startup event."""
        self._load_hotel_ranker()
        self._load_trip_classifier()
        self._load_feature_metadata()

    def _load_hotel_ranker(self) -> None:
        path = Path(settings.hotel_ranker_path)
        if not path.exists():
            logger.warning(
                "Hotel ranker model not found at %s. "
                "Run: python training/train_hotel_ranker.py",
                path,
            )
            return
        try:
            self._hotel_ranker = joblib.load(path)
            self._hotel_ranker_loaded = True
            logger.info("Hotel ranker loaded from %s", path)
        except Exception as e:
            logger.error("Failed to load hotel ranker: %s", e)

    def _load_trip_classifier(self) -> None:
        path = Path(settings.trip_classifier_path)
        if not path.exists():
            logger.warning(
                "Trip classifier model not found at %s. "
                "Run: python training/train_trip_classifier.py",
                path,
            )
            return
        try:
            self._trip_classifier = joblib.load(path)
            self._trip_classifier_loaded = True
            logger.info("Trip classifier loaded from %s", path)
        except Exception as e:
            logger.error("Failed to load trip classifier: %s", e)

    def _load_feature_metadata(self) -> None:
        path = Path(settings.feature_metadata_path)
        if not path.exists():
            logger.warning("Feature metadata not found at %s", path)
            return
        try:
            with open(path, "r") as f:
                self._feature_metadata = json.load(f)
            logger.info("Feature metadata loaded from %s", path)
        except Exception as e:
            logger.error("Failed to load feature metadata: %s", e)

    # ── Public getters ────────────────────────────────────────────────────────

    @property
    def hotel_ranker(self) -> Any:
        if not self._hotel_ranker_loaded:
            raise RuntimeError(
                "Hotel ranker model is not loaded. "
                "Run training/train_hotel_ranker.py first."
            )
        return self._hotel_ranker

    @property
    def trip_classifier(self) -> Any:
        if not self._trip_classifier_loaded:
            raise RuntimeError(
                "Trip classifier model is not loaded. "
                "Run training/train_trip_classifier.py first."
            )
        return self._trip_classifier

    @property
    def feature_metadata(self) -> dict:
        return self._feature_metadata

    @property
    def hotel_ranker_loaded(self) -> bool:
        return self._hotel_ranker_loaded

    @property
    def trip_classifier_loaded(self) -> bool:
        return self._trip_classifier_loaded

    def status(self) -> dict:
        return {
            "hotel_ranker_loaded": self._hotel_ranker_loaded,
            "trip_classifier_loaded": self._trip_classifier_loaded,
            "feature_metadata_loaded": bool(self._feature_metadata),
        }


# Module-level singleton — imported by services
model_loader = ModelLoader()
