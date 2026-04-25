# =============================================================================
# TripForge — Makefile
# Usage: make <target>
# =============================================================================

.PHONY: help up down logs ps rebuild reset health train verify \
        up-infra up-backend up-frontend run-auth run-ml run-frontend

# ── Colours ───────────────────────────────────────────────────────────────────
CYAN  := \033[0;36m
GREEN := \033[0;32m
YELLOW:= \033[1;33m
RED   := \033[0;31m
RESET := \033[0m

# ── Default target ────────────────────────────────────────────────────────────
help:
	@echo ""
	@echo "$(CYAN)  ✦ TripForge — Available Commands$(RESET)"
	@echo "  ─────────────────────────────────────────────────────"
	@echo "  $(GREEN)make up$(RESET)        Start the full stack (build + detach)"
	@echo "  $(GREEN)make down$(RESET)      Stop and remove containers"
	@echo "  $(GREEN)make reset$(RESET)     Stop, remove containers AND volumes (clean slate)"
	@echo "  $(GREEN)make rebuild$(RESET)   Full stop → rebuild → start"
	@echo "  $(GREEN)make logs$(RESET)      Tail logs from all services"
	@echo "  $(GREEN)make ps$(RESET)        Show running container status"
	@echo "  $(GREEN)make health$(RESET)    Check health of all services"
	@echo "  $(GREEN)make train$(RESET)     Train ML models (run once before first build)"
	@echo "  $(GREEN)make verify$(RESET)    Pre-flight checks before startup"
	@echo "  ─────────────────────────────────────────────────────"
	@echo "  $(CYAN)Partial startup:$(RESET)"
	@echo "  make up-infra      Start postgres + discovery only"
	@echo "  make up-backend    Start all backend services"
	@echo "  make up-frontend   Start frontend only"
	@echo "  ─────────────────────────────────────────────────────"
	@echo "  $(CYAN)Local dev (no Docker):$(RESET)"
	@echo "  make run-auth      Run auth-service locally (mvn)"
	@echo "  make run-ml        Run ml-service locally (uvicorn)"
	@echo "  make run-frontend  Run frontend locally (npm run dev)"
	@echo ""

# ── Core commands ─────────────────────────────────────────────────────────────

## Start the full TripForge stack
up:
	@echo ""
	@echo "$(CYAN)  ✦ TripForge — Starting Full Stack$(RESET)"
	@echo "  ─────────────────────────────────────────────────────"
	@# Copy .env if it doesn't exist yet
	@test -f .env || (cp .env.example .env && echo "  $(YELLOW)Created .env from .env.example$(RESET)")
	@test -f frontend/.env || (cp frontend/.env.example frontend/.env && echo "  $(YELLOW)Created frontend/.env from .env.example$(RESET)")
	@echo ""
	@echo "  Building and starting all containers..."
	@echo "  This may take 3-5 minutes on first run."
	@echo ""
	docker compose up --build -d
	@echo ""
	@bash scripts/print-urls.sh

## Stop and remove containers (keeps volumes)
down:
	@echo ""
	@echo "$(CYAN)  ✦ TripForge — Stopping Stack$(RESET)"
	@echo "  ─────────────────────────────────────────────────────"
	docker compose down
	@echo ""
	@echo "  $(GREEN)✓ Stack stopped. Volumes preserved.$(RESET)"
	@echo "  Run $(CYAN)make up$(RESET) to restart."
	@echo ""

## Stop, remove containers AND volumes (clean slate)
reset:
	@echo ""
	@echo "$(YELLOW)  ✦ TripForge — Full Reset (removes volumes + data)$(RESET)"
	@echo "  ─────────────────────────────────────────────────────"
	@echo "  $(YELLOW)Warning: This will delete all database data.$(RESET)"
	@echo "  Press Ctrl+C within 3 seconds to cancel..."
	@sleep 3
	docker compose down -v
	@echo ""
	@echo "  $(GREEN)✓ Stack reset. All containers and volumes removed.$(RESET)"
	@echo "  Run $(CYAN)make up$(RESET) to start fresh."
	@echo ""

## Rebuild all images and restart
rebuild:
	@echo ""
	@echo "$(CYAN)  ✦ TripForge — Rebuilding Stack$(RESET)"
	@echo "  ─────────────────────────────────────────────────────"
	docker compose down
	docker compose up --build -d
	@echo ""
	@bash scripts/print-urls.sh

## Tail logs from all services
logs:
	@echo "$(CYAN)  Tailing logs (Ctrl+C to stop)...$(RESET)"
	docker compose logs -f --tail=50

## Tail logs from a specific service: make logs-auth
logs-%:
	docker compose logs -f --tail=100 $*

## Show container status
ps:
	@echo ""
	@echo "$(CYAN)  ✦ TripForge — Container Status$(RESET)"
	@echo "  ─────────────────────────────────────────────────────"
	docker compose ps
	@echo ""

## Check health of all services
health:
	@bash scripts/check-health.sh

# ── Setup helpers ─────────────────────────────────────────────────────────────

## Train ML models (required before first docker build)
train:
	@bash scripts/train-ml-models.sh

## Pre-flight verification
verify:
	@bash scripts/verify-setup.sh

# ── Partial startup ───────────────────────────────────────────────────────────

## Start only infrastructure (postgres + discovery)
up-infra:
	@echo "$(CYAN)  Starting infrastructure services...$(RESET)"
	docker compose up -d postgres discovery-server
	@echo "  $(GREEN)✓ postgres and discovery-server started$(RESET)"

## Start all backend services (no frontend)
up-backend:
	@echo "$(CYAN)  Starting backend services...$(RESET)"
	docker compose up -d postgres discovery-server api-gateway \
	  auth-service trip-service hotel-service route-service \
	  budget-service split-service ml-service
	@echo "  $(GREEN)✓ Backend services started$(RESET)"

## Start frontend only
up-frontend:
	@echo "$(CYAN)  Starting frontend...$(RESET)"
	docker compose up -d frontend
	@echo "  $(GREEN)✓ Frontend started at http://localhost:3000$(RESET)"

# ── Local dev (no Docker) ─────────────────────────────────────────────────────

## Run auth-service locally
run-auth:
	@echo "$(CYAN)  Starting auth-service on :8081...$(RESET)"
	cd auth-service && mvn spring-boot:run

## Run ml-service locally
run-ml:
	@echo "$(CYAN)  Starting ml-service on :8087...$(RESET)"
	cd ml-service && uvicorn app.main:app --reload --port 8087

## Run frontend dev server locally
run-frontend:
	@echo "$(CYAN)  Starting frontend dev server on :5173...$(RESET)"
	cd frontend && npm run dev
