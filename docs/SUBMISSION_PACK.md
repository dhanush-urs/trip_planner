# TripForge — Submission Pack Checklist

---

## GitHub Repository

- [ ] Repository is public (or shared with evaluators)
- [ ] Repository name: `tripforge-ai`
- [ ] Description set: "AI-Powered Smart Trip Planner — Java microservices + Python ML + React"
- [ ] Topics added: `java` `spring-boot` `microservices` `react` `python` `fastapi` `machine-learning` `docker` `postgresql`
- [ ] README.md renders correctly with all sections
- [ ] All source code committed
- [ ] `.env` NOT committed
- [ ] `node_modules/` NOT committed
- [ ] `target/` NOT committed
- [ ] At least 5 screenshots in `docs/screenshots/` and linked in README

---

## Project ZIP (if required)

Create a clean ZIP excluding generated files:

```bash
# From the tripforge-ai parent directory
zip -r tripforge-ai.zip tripforge-ai/ \
  --exclude "*/node_modules/*" \
  --exclude "*/target/*" \
  --exclude "*/__pycache__/*" \
  --exclude "*/.git/*" \
  --exclude "*/.env" \
  --exclude "*/dist/*"
```

ZIP should contain:
- [ ] All service source code
- [ ] All Dockerfiles
- [ ] `docker-compose.yml`
- [ ] `.env.example`
- [ ] `Makefile`
- [ ] `scripts/`
- [ ] `docs/`
- [ ] `README.md`
- [ ] ML training scripts and datasets
- [ ] `saved_models/*.pkl` (optional — include if evaluators won't run training)

---

## Presentation (PPT)

Recommended slide structure (10-12 slides):

| Slide | Content |
|---|---|
| 1 | Title: TripForge — AI-Powered Smart Trip Planner |
| 2 | Problem Statement — why trip planning is hard |
| 3 | Solution Overview — what TripForge does |
| 4 | Architecture Diagram (from docs/DIAGRAMS.md) |
| 5 | Microservices List — table of services and responsibilities |
| 6 | ML Pipeline — hybrid ranking diagram |
| 7 | Tech Stack — table |
| 8 | Demo Screenshots — trip result, hotel change, budget |
| 9 | Database Schema — ER diagram |
| 10 | Sequence Diagram — create trip flow |
| 11 | Challenges & Solutions |
| 12 | Future Enhancements + Resume Summary |

---

## Project Report (if required)

Recommended sections:

1. **Abstract** (200 words)
2. **Introduction** — problem, motivation, objectives
3. **System Architecture** — microservices overview, component diagram
4. **Technology Stack** — justification for each choice
5. **Module Design** — each service's responsibility and API
6. **Database Design** — ER diagram, schema descriptions
7. **ML Pipeline** — model selection, training, evaluation metrics
8. **Implementation** — key code snippets, design patterns used
9. **Testing** — smoke tests, unit tests, results
10. **Results & Screenshots** — UI screenshots with descriptions
11. **Challenges Faced** — and how they were resolved
12. **Future Enhancements**
13. **Conclusion**
14. **References**

---

## Demo Video (optional but recommended)

If recording a demo video:

- Length: 3-5 minutes
- Resolution: 1080p
- Audio: narrate each step clearly
- Flow: follow `docs/DEMO_SCRIPT.md`
- Show: Eureka dashboard, trip creation, hotel change, ML health
- Tool: OBS Studio (free) or Loom

Filename: `tripforge-demo.mp4`
Store in: `docs/` or link from README

---

## Architecture Diagrams

Already generated in `docs/DIAGRAMS.md` as Mermaid:
- [ ] System architecture diagram
- [ ] Create trip sequence diagram
- [ ] Change hotel sequence diagram
- [ ] ER diagram
- [ ] ML pipeline diagram

To render as images:
- Use https://mermaid.live to paste and export as PNG
- Or use VS Code Mermaid Preview extension
- Store exported PNGs in `docs/screenshots/`

---

## Final Submission Checklist

- [ ] GitHub repo URL ready to share
- [ ] Project ZIP created (if required)
- [ ] PPT prepared (10-12 slides)
- [ ] Project report written (if required)
- [ ] Demo video recorded (optional)
- [ ] Screenshots captured and in README
- [ ] `make up` tested on a clean machine
- [ ] Evaluator can run the project with: `make train && make up`
