#!/usr/bin/env bash
# =============================================================================
# TripForge — Pre-flight Setup Verification
# Run before docker-compose up to catch common issues early.
# Usage: bash scripts/verify-setup.sh
# =============================================================================

set -euo pipefail

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

PASS=0
WARN=0
FAIL=0

ok()   { echo -e "  ${GREEN}✓${NC} $1"; PASS=$((PASS+1)); }
warn() { echo -e "  ${YELLOW}⚠${NC} $1"; WARN=$((WARN+1)); }
fail() { echo -e "  ${RED}✗${NC} $1"; FAIL=$((FAIL+1)); }

ROOT="$(cd "$(dirname "$0")/.." && pwd)"

echo ""
echo -e "${CYAN}╔══════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║   TripForge — Pre-flight Verification    ║${NC}"
echo -e "${CYAN}╚══════════════════════════════════════════╝${NC}"
echo ""

# ── Tools ─────────────────────────────────────────────────────────────────────
echo -e "${CYAN}Required Tools:${NC}"
command -v docker      &>/dev/null && ok "Docker found"          || fail "Docker not found"
command -v docker-compose &>/dev/null || docker compose version &>/dev/null \
  && ok "Docker Compose found" || fail "Docker Compose not found"
command -v java        &>/dev/null && ok "Java found ($(java -version 2>&1 | head -1))" \
  || warn "Java not found (needed for local dev only)"
command -v python3     &>/dev/null && ok "Python3 found"         || warn "Python3 not found"
command -v node        &>/dev/null && ok "Node.js found"         || warn "Node.js not found"
command -v mvn         &>/dev/null && ok "Maven found"           || warn "Maven not found (Docker build will still work)"

# ── .env file ─────────────────────────────────────────────────────────────────
echo ""
echo -e "${CYAN}Configuration:${NC}"
if [ -f "$ROOT/.env" ]; then
  ok ".env file exists"
else
  warn ".env file missing — copy from .env.example"
  echo "     Run: cp .env.example .env"
fi

if [ -f "$ROOT/frontend/.env" ]; then
  ok "frontend/.env exists"
else
  warn "frontend/.env missing — copy from frontend/.env.example"
fi

# ── ML Models ─────────────────────────────────────────────────────────────────
echo ""
echo -e "${CYAN}ML Models:${NC}"
if [ -f "$ROOT/ml-service/saved_models/hotel_ranker.pkl" ]; then
  ok "hotel_ranker.pkl found"
else
  fail "hotel_ranker.pkl missing — run: bash scripts/train-ml-models.sh"
fi

if [ -f "$ROOT/ml-service/saved_models/trip_style_classifier.pkl" ]; then
  ok "trip_style_classifier.pkl found"
else
  fail "trip_style_classifier.pkl missing — run: bash scripts/train-ml-models.sh"
fi

if [ -f "$ROOT/ml-service/saved_models/feature_metadata.json" ]; then
  ok "feature_metadata.json found"
else
  warn "feature_metadata.json missing (non-critical)"
fi

# ── Datasets ──────────────────────────────────────────────────────────────────
echo ""
echo -e "${CYAN}Datasets:${NC}"
[ -f "$ROOT/hotel-service/src/main/resources/dataset/hotels.csv" ] \
  && ok "hotels.csv found" || fail "hotels.csv missing"
[ -f "$ROOT/route-service/src/main/resources/dataset/attractions.csv" ] \
  && ok "attractions.csv found" || fail "attractions.csv missing"
[ -f "$ROOT/ml-service/training/datasets/hotels_master.csv" ] \
  && ok "hotels_master.csv found" || warn "hotels_master.csv missing (needed for training)"

# ── DB Migrations ─────────────────────────────────────────────────────────────
echo ""
echo -e "${CYAN}Database Migrations:${NC}"
for svc in auth trip hotel route budget split; do
  migration="$ROOT/${svc}-service/src/main/resources/db/migration/V1__init_${svc}_schema.sql"
  [ -f "$migration" ] && ok "${svc}-service migration found" || fail "${svc}-service migration missing"
done

# ── Docker ────────────────────────────────────────────────────────────────────
echo ""
echo -e "${CYAN}Docker:${NC}"
if docker info &>/dev/null; then
  ok "Docker daemon is running"
else
  fail "Docker daemon is not running — start Docker Desktop"
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo "──────────────────────────────────────────"
echo -e "  ${GREEN}${PASS} passed${NC}  ${YELLOW}${WARN} warnings${NC}  ${RED}${FAIL} failed${NC}"
echo "──────────────────────────────────────────"
echo ""

if [ "$FAIL" -gt 0 ]; then
  echo -e "${RED}Fix the failures above before running docker-compose up.${NC}"
  exit 1
elif [ "$WARN" -gt 0 ]; then
  echo -e "${YELLOW}Warnings found but you can proceed. Run: docker-compose up --build${NC}"
else
  echo -e "${GREEN}All checks passed! Run: docker-compose up --build${NC}"
fi
echo ""
