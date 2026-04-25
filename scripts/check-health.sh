#!/usr/bin/env bash
# =============================================================================
# TripForge — Health Check Script
# Checks all service health endpoints and reports status.
# Does NOT hard-fail on warm-up — services may still be starting.
# Usage: bash scripts/check-health.sh
#        make health
# =============================================================================

GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
RESET='\033[0m'

PASS=0
FAIL=0
WARN=0

# check <display-name> <url> <grep-pattern>
check() {
  local name="$1"
  local url="$2"
  local pattern="${3:-UP}"

  printf "  %-28s" "$name"

  local response
  response=$(curl -sf --max-time 5 "$url" 2>/dev/null)
  local exit_code=$?

  if [ $exit_code -ne 0 ]; then
    echo -e "${RED}✗ UNREACHABLE${RESET}  (not responding — may still be starting)"
    FAIL=$((FAIL + 1))
    return
  fi

  if [ "$pattern" = "ANY" ]; then
    echo -e "${GREEN}✓ UP${RESET}"
    PASS=$((PASS + 1))
  elif echo "$response" | grep -q "$pattern" 2>/dev/null; then
    echo -e "${GREEN}✓ UP${RESET}"
    PASS=$((PASS + 1))
  else
    echo -e "${YELLOW}⚠ DEGRADED${RESET}  (responded but pattern '${pattern}' not found)"
    WARN=$((WARN + 1))
  fi
}

echo ""
echo -e "${CYAN}  ╔══════════════════════════════════════════════════════╗${RESET}"
echo -e "${CYAN}  ║         ✦ TripForge — Health Check Report            ║${RESET}"
echo -e "${CYAN}  ╚══════════════════════════════════════════════════════╝${RESET}"
echo ""

# ── Infrastructure ────────────────────────────────────────────────────────────
echo -e "  ${CYAN}Infrastructure:${RESET}"
check "PostgreSQL (via gateway)" "http://localhost:8080/actuator/health" "UP"

echo ""
echo -e "  ${CYAN}Java Microservices:${RESET}"
check "Discovery Server"   "http://localhost:8761/actuator/health"  "UP"
check "API Gateway"        "http://localhost:8080/actuator/health"  "UP"
check "Auth Service"       "http://localhost:8081/actuator/health"  "UP"
check "Trip Service"       "http://localhost:8082/actuator/health"  "UP"
check "Hotel Service"      "http://localhost:8083/actuator/health"  "UP"
check "Route Service"      "http://localhost:8084/actuator/health"  "UP"
check "Budget Service"     "http://localhost:8085/actuator/health"  "UP"
check "Split Service"      "http://localhost:8086/actuator/health"  "UP"

echo ""
echo -e "  ${CYAN}ML Service:${RESET}"
check "ML Service"         "http://localhost:8087/health"           "UP"

# ML model status
printf "  %-28s" "Hotel Ranker Model"
ml_resp=$(curl -sf --max-time 5 "http://localhost:8087/health" 2>/dev/null)
if echo "$ml_resp" | grep -q '"hotel_ranker_loaded": true\|"loaded": true' 2>/dev/null; then
  echo -e "${GREEN}✓ LOADED${RESET}"
  PASS=$((PASS + 1))
elif [ -n "$ml_resp" ]; then
  echo -e "${YELLOW}⚠ DEGRADED${RESET}  (ML service up but model not loaded — run: make train)"
  WARN=$((WARN + 1))
else
  echo -e "${RED}✗ UNREACHABLE${RESET}"
  FAIL=$((FAIL + 1))
fi

echo ""
echo -e "  ${CYAN}Frontend:${RESET}"
check "Frontend (Nginx)"   "http://localhost:3000"                  "ANY"

echo ""
echo -e "  ${CYAN}Eureka Registry:${RESET}"
printf "  %-28s" "Registered Services"
eureka_resp=$(curl -sf --max-time 5 \
  -H "Accept: application/json" \
  "http://localhost:8761/eureka/apps" 2>/dev/null)
if [ -n "$eureka_resp" ]; then
  # Count <name> tags as a proxy for registered service count
  count=$(echo "$eureka_resp" | grep -o '"name"' | wc -l | tr -d ' ')
  if [ "$count" -ge 6 ]; then
    echo -e "${GREEN}✓ ${count} service(s) registered${RESET}"
    PASS=$((PASS + 1))
  else
    echo -e "${YELLOW}⚠ Only ${count} service(s) registered (expected 7+)${RESET}"
    WARN=$((WARN + 1))
  fi
else
  echo -e "${RED}✗ Cannot reach Eureka${RESET}"
  FAIL=$((FAIL + 1))
fi

# ── Summary ───────────────────────────────────────────────────────────────────
echo ""
echo -e "  ──────────────────────────────────────────────────────"
echo -e "  Results:  ${GREEN}${PASS} passed${RESET}  ${YELLOW}${WARN} degraded${RESET}  ${RED}${FAIL} failed${RESET}"
echo -e "  ──────────────────────────────────────────────────────"
echo ""

if [ "$FAIL" -gt 0 ]; then
  echo -e "  ${YELLOW}Some services are not responding yet.${RESET}"
  echo -e "  If the stack just started, wait 2-3 more minutes and run again."
  echo -e "  To view logs: ${CYAN}make logs${RESET}"
  echo -e "  To view logs for a specific service: ${CYAN}make logs-auth${RESET}"
  echo ""
elif [ "$WARN" -gt 0 ]; then
  echo -e "  ${YELLOW}Stack is running with warnings. Check degraded services above.${RESET}"
  echo ""
else
  echo -e "  ${GREEN}✓ All services are healthy. TripForge is ready!${RESET}"
  echo -e "  Open: ${CYAN}http://localhost:3000${RESET}"
  echo ""
fi
