#!/usr/bin/env bash
# =============================================================================
# TripForge — Print URLs after stack startup
# Called by: make up, make rebuild
# =============================================================================

CYAN='\033[0;36m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RESET='\033[0m'

echo ""
echo -e "${CYAN}  ╔══════════════════════════════════════════════════════╗${RESET}"
echo -e "${CYAN}  ║         ✦ TripForge Stack is Starting Up             ║${RESET}"
echo -e "${CYAN}  ╚══════════════════════════════════════════════════════╝${RESET}"
echo ""
echo -e "  ${GREEN}All containers have been started in the background.${RESET}"
echo -e "  ${YELLOW}Services take 3-4 minutes to become fully healthy.${RESET}"
echo ""
echo -e "  ${CYAN}Access Points:${RESET}"
echo -e "  ──────────────────────────────────────────────────────"
echo -e "  ${GREEN}Frontend App${RESET}       →  http://localhost:3000"
echo -e "  ${GREEN}API Gateway${RESET}        →  http://localhost:8080"
echo -e "  ${GREEN}Eureka Dashboard${RESET}   →  http://localhost:8761"
echo -e "  ${GREEN}ML Service Docs${RESET}    →  http://localhost:8087/docs"
echo -e "  ${GREEN}ML Health${RESET}          →  http://localhost:8087/health"
echo ""
echo -e "  ${CYAN}Service Health Endpoints:${RESET}"
echo -e "  ──────────────────────────────────────────────────────"
echo -e "  Gateway    →  http://localhost:8080/actuator/health"
echo -e "  Auth       →  http://localhost:8081/actuator/health"
echo -e "  Trip       →  http://localhost:8082/actuator/health"
echo -e "  Hotel      →  http://localhost:8083/actuator/health"
echo -e "  Route      →  http://localhost:8084/actuator/health"
echo -e "  Budget     →  http://localhost:8085/actuator/health"
echo -e "  Split      →  http://localhost:8086/actuator/health"
echo ""
echo -e "  ${CYAN}Useful Commands:${RESET}"
echo -e "  ──────────────────────────────────────────────────────"
echo -e "  Check health   →  ${YELLOW}make health${RESET}"
echo -e "  View logs      →  ${YELLOW}make logs${RESET}"
echo -e "  View status    →  ${YELLOW}make ps${RESET}"
echo -e "  Stop stack     →  ${YELLOW}make down${RESET}"
echo -e "  Full reset     →  ${YELLOW}make reset${RESET}"
echo ""
echo -e "  ${YELLOW}Tip: Run 'make health' after ~3 minutes to verify all services are UP.${RESET}"
echo ""
