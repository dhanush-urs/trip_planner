# TripForge — Deployment Targets

## Current State

TripForge is designed for local Docker Compose deployment.
All services are containerized and production-ready at the image level.

---

## Platform Suitability

### Railway / Render (Easiest)

**Suitable for:** Demo deployment, portfolio hosting

**What works:**
- Each service can be deployed as a separate Railway/Render service
- PostgreSQL and Redis available as managed add-ons
- Environment variables set via dashboard

**What needs change:**
- Remove `discovery-server` (Eureka) — use direct service URLs or Railway's internal DNS
- Update Feign clients to use direct URLs instead of `lb://service-name`
- Update gateway routes to use direct service URLs

**Estimated effort:** 2-3 days

---

### AWS EC2 / DigitalOcean Droplet

**Suitable for:** Single-server demo deployment

**What works:**
- `docker-compose up` works as-is on any Linux server with Docker
- Add Nginx reverse proxy for HTTPS

**What needs change:**
- Add TLS (Let's Encrypt via Certbot)
- Set `VITE_API_BASE_URL` to your domain
- Restrict CORS to your domain
- Use strong secrets in `.env`

**Estimated effort:** 1 day

---

### Fly.io

**Suitable for:** Multi-region deployment

**What works:**
- Dockerfile-based deployment
- Managed PostgreSQL and Redis available

**What needs change:**
- One `fly.toml` per service
- Replace Eureka with Fly.io internal DNS
- Update Feign clients

**Estimated effort:** 3-4 days

---

### Kubernetes (EKS/GKE/AKS)

**Suitable for:** Production-grade deployment

**What works:**
- All Dockerfiles are production-ready
- Health checks are properly configured
- Non-root users in all containers

**What needs change:**
- Kubernetes manifests (Deployment, Service, ConfigMap, Secret) per service
- Replace Eureka with K8s DNS
- Ingress controller for gateway
- Horizontal Pod Autoscaler for trip-service and hotel-service
- Persistent Volume Claims for PostgreSQL and Redis

**Estimated effort:** 1-2 weeks

---

## Dockerfile Readiness Checklist

All Java services:
- ✅ Multi-stage build (Maven builder + JRE runtime)
- ✅ Non-root user (`tripforge`)
- ✅ Health check on `/actuator/health`
- ✅ `eclipse-temurin:17-jre` (multi-arch, arm64 compatible)

ML service:
- ✅ `python:3.11-slim`
- ✅ Non-root user
- ✅ Health check on `/health`

Frontend:
- ✅ Node 20 builder + Nginx 1.25 runtime
- ✅ SPA routing configured
- ✅ Static asset caching

---

## Production Checklist

Before deploying to production:

- [ ] Change `JWT_SECRET` to a cryptographically random 256-bit value
- [ ] Change `DB_PASSWORD` to a strong random password
- [ ] Set `RAZORPAY_WEBHOOK_SECRET` to a strong random value
- [ ] Restrict CORS to production frontend domain
- [ ] Enable HTTPS (TLS termination)
- [ ] Set `GEMINI_ENABLED=true` only if you have a billing account
- [ ] Configure log aggregation (CloudWatch, Datadog, etc.)
- [ ] Set up database backups
- [ ] Configure Redis persistence
- [ ] Review rate limiting limits for production traffic
