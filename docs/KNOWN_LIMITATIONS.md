# TripForge — Known Limitations & Future Roadmap

## Intentional Simplifications

These are deliberate tradeoffs for a portfolio/demo project:

| Limitation | Why Acceptable | Production Fix |
|---|---|---|
| No JWT refresh token | Simplifies auth flow | Add refresh token endpoint + httpOnly cookie |
| In-memory rate limiting | No Redis dependency for rate limiting | Redis-backed `RequestRateLimiter` |
| JWT in localStorage | Simpler frontend | httpOnly cookie with CSRF protection |
| No HTTPS in Docker | Local dev only | TLS termination at load balancer |
| Single PostgreSQL instance | Simpler local setup | Managed PostgreSQL with read replicas |
| No message queue | Synchronous Feign calls | Kafka/RabbitMQ for async event processing |
| No distributed tracing | Correlation IDs only | Zipkin/Jaeger with Spring Sleuth |
| CSV hotel/attraction data | No billing required | Real hotel API (Booking.com, Amadeus) |
| Heuristic route fallback | No Google billing required | Always-on Google Directions |
| Internal payment links | Razorpay Payment Links API requires extra setup | Full Razorpay Payment Links integration |
| No email notifications | Out of scope | SendGrid/SES for booking confirmations |
| No user-uploaded photos | Out of scope | S3 + CloudFront |

## What Would Be Different in Production

### Infrastructure
- Kubernetes with HPA for auto-scaling
- Managed PostgreSQL (RDS/Cloud SQL) with automated backups
- Managed Redis (ElastiCache/Upstash)
- CDN for frontend static assets
- TLS everywhere

### Security
- JWT refresh tokens with rotation
- httpOnly cookies instead of localStorage
- Redis-backed rate limiting
- WAF for API Gateway
- Secrets in Vault or AWS Secrets Manager
- Regular dependency vulnerability scanning

### Observability
- Distributed tracing with Zipkin or Jaeger
- Metrics with Prometheus + Grafana
- Centralized logging with ELK or Datadog
- Alerting on error rates and latency

### Data
- Real hotel/attraction data from Booking.com or Amadeus API
- User feedback loop for ML model retraining
- A/B testing for recommendation algorithms
- Analytics pipeline for trip patterns

### Payments
- Full Razorpay Payment Links API
- Stripe for international payments
- Refund flow
- Payment dispute handling
- PCI DSS compliance review

## What Was Intentionally NOT Built

- Admin dashboard (out of scope for MVP)
- Mobile app (React Native would be Phase 10)
- Social features (trip sharing, reviews)
- Real-time collaboration (WebSockets)
- Offline mode (PWA)
- Multi-language support (i18n)
- Accessibility audit (WCAG 2.1 AA)
