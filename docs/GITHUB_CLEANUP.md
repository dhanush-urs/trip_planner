# TripForge — GitHub Repo Cleanup Guide

## Final Folder Structure (what to commit)

```
tripforge-ai/
├── discovery-server/          ✅ commit
├── api-gateway/               ✅ commit
├── auth-service/              ✅ commit
├── trip-service/              ✅ commit
├── hotel-service/             ✅ commit
├── route-service/             ✅ commit
├── budget-service/            ✅ commit
├── split-service/             ✅ commit
├── ml-service/
│   ├── app/                   ✅ commit
│   ├── training/
│   │   ├── datasets/
│   │   │   ├── hotels_master.csv          ✅ commit (source data)
│   │   │   ├── hotel_training_data.csv    ⚠️  optional (generated)
│   │   │   └── trip_style_training_data.csv ⚠️ optional (generated)
│   │   ├── generate_hotel_training_data.py ✅ commit
│   │   ├── generate_trip_style_data.py    ✅ commit
│   │   ├── train_hotel_ranker.py          ✅ commit
│   │   └── train_trip_classifier.py       ✅ commit
│   ├── saved_models/
│   │   ├── hotel_ranker.pkl               ⚠️  see note below
│   │   ├── trip_style_classifier.pkl      ⚠️  see note below
│   │   └── feature_metadata.json          ✅ commit
│   ├── tests/                 ✅ commit
│   ├── requirements.txt       ✅ commit
│   ├── Dockerfile             ✅ commit
│   └── README.md              ✅ commit
├── frontend/
│   ├── src/                   ✅ commit
│   ├── public/                ✅ commit
│   ├── package.json           ✅ commit
│   ├── vite.config.js         ✅ commit
│   ├── tailwind.config.js     ✅ commit
│   ├── postcss.config.js      ✅ commit
│   ├── nginx.conf             ✅ commit
│   ├── Dockerfile             ✅ commit
│   ├── .env.example           ✅ commit
│   └── README.md              ✅ commit
│   ── node_modules/           ❌ never commit
│   └── dist/                  ❌ never commit
├── scripts/                   ✅ commit
├── docs/                      ✅ commit all
├── docker-compose.yml         ✅ commit
├── .env.example               ✅ commit
├── .env                       ❌ NEVER commit (contains secrets)
├── .gitignore                 ✅ commit
├── Makefile                   ✅ commit
├── README.md                  ✅ commit
├── API_CONTRACT.md            ✅ commit
└── INTEGRATION_CHECKLIST.md  ✅ commit
```

---

## ML Model Files — Decision

**Option A: Commit the .pkl files** (recommended for demo repos)
- Pros: `docker-compose up` works immediately, no training step needed
- Cons: Binary files in git, ~5-10MB each
- Add to git: `git add ml-service/saved_models/*.pkl`

**Option B: Don't commit .pkl files** (recommended for clean repos)
- Pros: Clean git history, reproducible from source
- Cons: Users must run `bash scripts/train-ml-models.sh` before first run
- Add to .gitignore: `ml-service/saved_models/*.pkl`
- Document clearly in README

**Recommendation:** Commit `feature_metadata.json` always. Commit `.pkl` files for the demo repo. Add a note in README.

---

## Files That Must NEVER Be Committed

| File | Reason |
|---|---|
| `.env` | Contains JWT_SECRET, DB_PASSWORD |
| `frontend/.env` | Contains API base URL (may vary per environment) |
| `**/target/` | Maven build output |
| `**/node_modules/` | npm packages |
| `frontend/dist/` | Vite build output |
| `**/__pycache__/` | Python bytecode |
| `.idea/`, `.vscode/` | IDE config |

---

## Where Screenshots Should Live

```
tripforge-ai/
└── docs/
    └── screenshots/
        ├── 01-login.png
        ├── 02-register.png
        ├── 03-dashboard.png
        ├── 04-create-trip-form.png
        ├── 05-trip-result.png
        ├── 06-hotel-change-modal.png
        ├── 07-budget-breakdown.png
        ├── 08-trip-history.png
        ├── 09-trip-details.png
        └── 10-eureka-dashboard.png
```

Reference in README.md:
```markdown
## Screenshots
![Trip Result](docs/screenshots/05-trip-result.png)
```

---

## Sample Payload JSON Files

Add these for API testing and documentation:

```
tripforge-ai/
└── docs/
    └── sample-payloads/
        ├── register-request.json
        ├── login-request.json
        ├── create-trip-request.json
        ├── create-trip-response.json
        ├── change-hotel-request.json
        ├── hotel-rank-ml-request.json
        └── trip-style-request.json
```

---

## Recommended Git Commit Structure

```bash
# Initial commit
git add .
git commit -m "feat: initial TripForge microservices implementation

- 10-service architecture: discovery, gateway, auth, trip, hotel, route, budget, split, ml, frontend
- Hybrid ML hotel ranking pipeline (GBR R²=0.90)
- React 18 + Tailwind CSS frontend
- Docker Compose orchestration
- PostgreSQL with 6 logical schemas + Flyway migrations"

# Tag the release
git tag -a v1.0.0 -m "TripForge v1.0.0 - Complete implementation"
git push origin main --tags
```

---

## GitHub Repository Settings

- **Description:** AI-Powered Smart Trip Planner — Java microservices + Python ML + React
- **Topics:** `java` `spring-boot` `microservices` `react` `python` `fastapi` `machine-learning` `docker` `postgresql` `eureka`
- **README:** Already polished — will render well on GitHub
- **License:** MIT
- **Branch protection:** Protect `main`, require PR reviews for team projects
