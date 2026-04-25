"""
Health check endpoint.
Returns service status and model load status.
"""
from fastapi import APIRouter
from app.models.model_loader import model_loader
from app.config import settings

router = APIRouter()


@router.get("/health", tags=["Health"])
def health_check():
    """
    Returns the health status of the ML service and loaded models.
    Used by Docker HEALTHCHECK and Spring Boot Actuator-style monitoring.
    """
    model_status = model_loader.status()
    all_healthy = model_status["hotel_ranker_loaded"]  # minimum requirement

    return {
        "status": "UP" if all_healthy else "DEGRADED",
        "service": settings.app_name,
        "version": settings.app_version,
        "models": {
            "hotel_ranker": {
                "loaded": model_status["hotel_ranker_loaded"],
                "status": "ready" if model_status["hotel_ranker_loaded"] else "missing",
            },
            "trip_classifier": {
                "loaded": model_status["trip_classifier_loaded"],
                "status": "ready" if model_status["trip_classifier_loaded"] else "missing",
            },
        },
        "note": (
            "Run training scripts to load models."
            if not all_healthy else "All models operational."
        ),
    }
