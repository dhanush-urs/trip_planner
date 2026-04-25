# TripForge — Final Release Checklist

Complete this checklist before submitting or presenting TripForge.

---

## Code & Build

- [ ] `make reset && make up` completes without errors
- [ ] `make health` shows all services UP after 3-4 minutes
- [ ] `make ps` shows all 11 containers running
- [ ] No compilation errors in any Java service (`mvn clean package -DskipTests` passes)
- [ ] No Python import errors (`python3 -m pytest ml-service/tests/ -v` — 41/41 pass)
- [ ] Frontend builds without errors (`cd frontend && npm run build`)

---

## Smoke Tests

- [ ] Register new user → HTTP 201, JWT returned
- [ ] Login → HTTP 200, JWT returned
- [ ] Get profile with JWT → HTTP 200
- [ ] Unauthenticated request → HTTP 401
- [ ] Create trip (Goa, 4 days, ₹50k) → HTTP 201, itinerary non-empty
- [ ] Get trip by ID → HTTP 200
- [ ] Get user trips → HTTP 200, array returned
- [ ] Change hotel (CHEAPER) → HTTP 200, alternatives returned
- [ ] Replan trip → HTTP 200, new hotel in response
- [ ] ML hotel rank → HTTP 200, scores in [0,1]
- [ ] ML trip classifier → HTTP 200, valid style returned

---

## Frontend

- [ ] Login page renders correctly
- [ ] Register page renders correctly
- [ ] Dashboard shows welcome message and CTA buttons
- [ ] Create Trip form validates all fields
- [ ] Trip result page renders hotel + itinerary + budget + split
- [ ] Hotel change modal opens, searches, and updates
- [ ] Trip history shows past trips
- [ ] Trip details page renders full trip
- [ ] No JavaScript console errors on any page
- [ ] Responsive layout works on 1280px+ screen

---

## Documentation

- [ ] `README.md` reviewed — accurate ports, commands, and descriptions
- [ ] `docs/SYSTEM_DESIGN.md` complete
- [ ] `docs/API_CONTRACTS.md` complete with examples
- [ ] `docs/DATABASE_SCHEMA.md` complete
- [ ] `docs/DEMO_SCRIPT.md` reviewed and rehearsed
- [ ] `docs/DIAGRAMS.md` Mermaid diagrams render correctly
- [ ] `docs/RESUME_VIVA.md` reviewed

---

## Configuration

- [ ] `.env.example` has all required variables
- [ ] `frontend/.env.example` has `VITE_API_BASE_URL`
- [ ] `.env` is NOT committed (in `.gitignore`)
- [ ] `JWT_SECRET` is the same in `.env.example` for gateway and auth-service
- [ ] All service ports in `.env.example` match `docker-compose.yml`

---

## Repository

- [ ] `.gitignore` covers: `.env`, `target/`, `node_modules/`, `__pycache__/`, `dist/`
- [ ] No secrets committed (check with `git log --all -p | grep -i "password\|secret\|token"`)
- [ ] `README.md` renders correctly on GitHub (check badges, code blocks, tables)
- [ ] All scripts are executable (`chmod +x scripts/*.sh`)
- [ ] `Makefile` works: `make up`, `make down`, `make health` all function correctly

---

## ML Models

- [ ] `ml-service/saved_models/hotel_ranker.pkl` exists
- [ ] `ml-service/saved_models/trip_style_classifier.pkl` exists
- [ ] `ml-service/saved_models/feature_metadata.json` exists
- [ ] ML health endpoint confirms both models loaded

---

## Screenshots

- [ ] Login page screenshot captured
- [ ] Trip result page screenshot captured
- [ ] Hotel change modal screenshot captured
- [ ] Eureka dashboard screenshot captured
- [ ] Docker containers screenshot captured
- [ ] Screenshots stored in `docs/screenshots/`
- [ ] At least 5 screenshots added to README.md

---

## Final Commit

- [ ] All changes staged: `git add .`
- [ ] Commit message written: `git commit -m "feat: TripForge v1.0.0 - Complete implementation"`
- [ ] Pushed to remote: `git push origin main`
- [ ] Optional: tag created: `git tag -a v1.0.0 -m "TripForge v1.0.0"`
- [ ] Optional: tag pushed: `git push origin --tags`
