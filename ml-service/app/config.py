"""
Application configuration loaded from environment variables.
"""
from pydantic_settings import BaseSettings
from pathlib import Path

# Resolve project root (ml-service/)
BASE_DIR = Path(__file__).resolve().parent.parent


class Settings(BaseSettings):
    app_name: str = "TripForge ML Service"
    app_version: str = "1.0.0"
    debug: bool = False

    # Model paths (relative to BASE_DIR)
    hotel_ranker_path: str = str(BASE_DIR / "saved_models" / "hotel_ranker.pkl")
    trip_classifier_path: str = str(BASE_DIR / "saved_models" / "trip_style_classifier.pkl")
    feature_metadata_path: str = str(BASE_DIR / "saved_models" / "feature_metadata.json")

    # Hybrid scoring weights
    ml_weight: float = 0.65
    rule_weight: float = 0.35

    class Config:
        env_file = ".env"


settings = Settings()
