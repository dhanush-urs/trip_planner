#!/usr/bin/env bash
# =============================================================================
# TripForge — ML Model Training Script
# Run this ONCE before docker-compose up to generate .pkl model files.
# Usage: bash scripts/train-ml-models.sh
# =============================================================================

set -euo pipefail

GREEN='\033[0;32m'
CYAN='\033[0;36m'
RED='\033[0;31m'
NC='\033[0m'

ML_DIR="$(cd "$(dirname "$0")/../ml-service" && pwd)"

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║     TripForge — ML Model Training        ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════╝${NC}"
echo ""

# Check Python
if ! command -v python3 &>/dev/null; then
  echo -e "${RED}Error: python3 not found. Install Python 3.11+${NC}"
  exit 1
fi

# Check dependencies
echo "Checking Python dependencies..."
if ! python3 -c "import sklearn, pandas, joblib, fastapi" 2>/dev/null; then
  echo "Installing dependencies..."
  pip3 install -r "$ML_DIR/requirements.txt" --quiet
fi

echo ""
echo -e "${CYAN}Step 1/4: Generating hotel training data...${NC}"
python3 "$ML_DIR/training/generate_hotel_training_data.py"

echo ""
echo -e "${CYAN}Step 2/4: Training hotel ranker model...${NC}"
python3 "$ML_DIR/training/train_hotel_ranker.py"

echo ""
echo -e "${CYAN}Step 3/4: Generating trip style training data...${NC}"
python3 "$ML_DIR/training/generate_trip_style_data.py"

echo ""
echo -e "${CYAN}Step 4/4: Training trip style classifier...${NC}"
python3 "$ML_DIR/training/train_trip_classifier.py"

echo ""
echo -e "${GREEN}✓ All models trained successfully!${NC}"
echo ""
echo "Saved models:"
ls -lh "$ML_DIR/saved_models/"
echo ""
echo -e "${CYAN}You can now run: docker-compose up --build${NC}"
echo ""
