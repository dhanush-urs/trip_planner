"""
TripForge ML Service — FastAPI application entry point.

Startup sequence:
  1. Load trained models from saved_models/
  2. Register API routes
  3. Expose health endpoint

The service degrades gracefully if models are missing —
health endpoint reports DEGRADED status and rule-based
fallbacks are used automatically.
"""
import logging
from contextlib import asynccontextmanager

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.api.health import router as health_router
from app.api.routes import router as ml_router
from app.config import settings
from app.models.model_loader import model_loader
from app.middleware.correlation import CorrelationIdMiddleware

# ── Logging setup ─────────────────────────────────────────────────────────────
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s | %(levelname)-8s | %(name)s | %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S",
)
logger = logging.getLogger(__name__)


# ── Lifespan (replaces deprecated @app.on_event) ─────────────────────────────
@asynccontextmanager
async def lifespan(app: FastAPI):
    """Load models on startup, clean up on shutdown."""
    logger.info("Starting %s v%s", settings.app_name, settings.app_version)
    model_loader.load_all()
    status = model_loader.status()
    logger.info("Model status: %s", status)
    yield
    logger.info("Shutting down %s", settings.app_name)


# ── FastAPI app ───────────────────────────────────────────────────────────────
app = FastAPI(
    title=settings.app_name,
    version=settings.app_version,
    description=(
        "TripForge ML Service — Hybrid hotel ranking, "
        "feedback-aware re-ranking, and trip style classification."
    ),
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

# ── CORS (allow Java microservices and frontend) ──────────────────────────────
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],  # Restrict in production
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Correlation ID middleware
app.add_middleware(CorrelationIdMiddleware)

# ── Register routers ──────────────────────────────────────────────────────────
app.include_router(health_router)
app.include_router(ml_router)


@app.get("/", include_in_schema=False)
def root():
    return {
        "service": settings.app_name,
        "version": settings.app_version,
        "docs": "/docs",
        "health": "/health",
    }
