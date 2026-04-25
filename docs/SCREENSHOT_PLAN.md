# TripForge — Screenshot Capture Plan

Store all screenshots under `docs/screenshots/`.

---

## Capture Checklist

### UI Screenshots

| # | Filename | What to Capture | Notes |
|---|---|---|---|
| 01 | `01-login.png` | Login page | Clean, no errors, brand visible |
| 02 | `02-register.png` | Register page | Form filled with demo data |
| 03 | `03-dashboard.png` | Dashboard | After login, shows welcome + recent trips |
| 04 | `04-create-trip-form.png` | Create Trip form | Filled: Goa, 4 days, ₹50k, beaches+food |
| 05 | `05-trip-result-full.png` | Full trip result page | Scroll to show all sections |
| 06 | `06-trip-result-hotel.png` | Hotel card close-up | Shows name, price, rating, amenities |
| 07 | `07-trip-result-itinerary.png` | Itinerary section | Shows day cards with attractions |
| 08 | `08-trip-result-budget.png` | Budget breakdown card | Shows all cost categories + progress bars |
| 09 | `09-trip-result-split.png` | Split expense card | Shows per-person amount |
| 10 | `10-hotel-change-modal.png` | Hotel change modal | Reason buttons visible |
| 11 | `11-hotel-change-alternatives.png` | Modal with alternatives | After clicking "Find Alternatives" |
| 12 | `12-trip-history.png` | Trip history page | Shows trip cards with status badges |
| 13 | `13-trip-details.png` | Trip details page | Full details view |

### Infrastructure Screenshots

| # | Filename | What to Capture | Notes |
|---|---|---|---|
| 14 | `14-eureka-dashboard.png` | Eureka at :8761 | All 7 services registered |
| 15 | `15-docker-containers.png` | `docker compose ps` output | All containers running |
| 16 | `16-ml-health.png` | ML health endpoint | Both models loaded |
| 17 | `17-ml-swagger.png` | FastAPI Swagger UI at :8087/docs | Shows 3 endpoints |

### API / Postman Screenshots

| # | Filename | What to Capture | Notes |
|---|---|---|---|
| 18 | `18-postman-register.png` | Register API call | Request + 201 response |
| 19 | `19-postman-login.png` | Login API call | Request + JWT token in response |
| 20 | `20-postman-create-trip.png` | Create trip API call | Full TripResponse |
| 21 | `21-postman-hotel-change.png` | Hotel change API call | Alternatives response |
| 22 | `22-postman-ml-rank.png` | ML hotel rank call | Ranked hotels with scores |

---

## How to Capture

### Browser Screenshots
- Use browser full-page screenshot (Firefox: right-click → Take Screenshot → Save Full Page)
- Or use a browser extension like "Full Page Screen Capture"
- Recommended resolution: 1440×900 or higher
- Use dark mode (already the default theme)

### Terminal Screenshots
```bash
# Docker container status
docker compose ps

# Take terminal screenshot or copy output to a text file
docker compose ps > docs/screenshots/docker-ps-output.txt
```

### Postman
- Import the sample payloads from `docs/sample-payloads/`
- Set `Authorization: Bearer <token>` header
- Capture request + response in the same frame

---

## README Integration

After capturing, add to README.md:

```markdown
## Screenshots

### Trip Result Page
![Trip Result](docs/screenshots/05-trip-result-full.png)

### Hotel Change Modal
![Hotel Change](docs/screenshots/10-hotel-change-modal.png)

### Eureka Dashboard
![Eureka](docs/screenshots/14-eureka-dashboard.png)
```

---

## Priority Order (if time is limited)

Capture these first — they show the most value:

1. `05-trip-result-full.png` — the main feature
2. `14-eureka-dashboard.png` — proves microservices
3. `10-hotel-change-modal.png` — shows ML re-ranking
4. `08-trip-result-budget.png` — shows budget feature
5. `15-docker-containers.png` — proves Docker setup
