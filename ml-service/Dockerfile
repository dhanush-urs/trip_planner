# =============================================================================
# TripForge ML Service Dockerfile
#
# IMPORTANT: Train models before building:
#   cd ml-service
#   python3 training/generate_hotel_training_data.py
#   python3 training/train_hotel_ranker.py
#   python3 training/generate_trip_style_data.py
#   python3 training/train_trip_classifier.py
#
# The saved_models/ directory must contain .pkl files before docker build.
# =============================================================================

# ── Build stage: install dependencies ────────────────────────────────────────
FROM python:3.11-slim AS builder
WORKDIR /install

RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc g++ \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --no-cache-dir --prefix=/install/packages -r requirements.txt

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM python:3.11-slim
WORKDIR /app

# Copy installed packages
COPY --from=builder /install/packages /usr/local

# Copy application
COPY app/       ./app/
COPY training/  ./training/

# Copy saved models (must exist — run training scripts first)
# If missing, service starts in DEGRADED mode with rule-based fallback
COPY saved_models/ ./saved_models/

# Non-root user
RUN useradd -m -u 1001 tripforge && chown -R tripforge:tripforge /app
USER tripforge

EXPOSE 8087

HEALTHCHECK --interval=30s --timeout=10s --start-period=30s --retries=3 \
  CMD python3 -c "import urllib.request; urllib.request.urlopen('http://localhost:8087/health')" || exit 1

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8087", "--workers", "1"]
