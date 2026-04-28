# TripForge — Security Notes

## Authentication

**JWT-based stateless auth:**
- Tokens signed with HMAC-SHA256 (HS256) using a shared secret
- Token TTL: 24 hours (configurable via `JWT_EXPIRATION_MS`)
- Token stored in browser `localStorage` (acceptable for demo; use httpOnly cookies in production)
- No refresh token implemented (intentional simplification — add in production v2)
- All protected routes validated at API Gateway before reaching downstream services
- Gateway injects `X-User-Id` and `X-User-Email` headers — downstream services trust these

**Password hashing:**
- BCrypt with strength 12
- Passwords never stored in plaintext
- Passwords never logged

## API Key Handling

- All API keys injected via environment variables only
- No keys hardcoded in source code
- `.env` is in `.gitignore` — never committed
- `.env.example` contains only placeholder values
- Health endpoints report `configured: true/false` — never expose key values

## Payment Security

- Razorpay payment verification uses HMAC-SHA256 signature check
- Signature: `HMAC-SHA256(orderId + "|" + paymentId, keySecret)`
- Webhook signature verified before any state update
- Webhook endpoint is public but signature-verified
- All other payment endpoints require valid JWT
- Payment state never updated without verified signature
- Idempotency keys prevent double-charging on retries

## Rate Limiting

In-memory rate limiting at API Gateway:

| Endpoint | Limit | Window |
|---|---|---|
| `/api/auth/login` | 5 req | 60s per IP |
| `/api/auth/register` | 5 req | 60s per IP |
| `/api/payments/**` | 10 req | 60s per IP |
| `/api/external/**` | 20 req | 60s per IP |
| `/api/ai/**` | 20 req | 60s per IP |

Returns `429 Too Many Requests` with `Retry-After` header.

**Note:** In-memory rate limiting is per-instance. For multi-instance production, use Redis-backed rate limiting (Spring Cloud Gateway `RequestRateLimiter` filter).

## CORS

Current configuration (development):
- Allowed origins: `http://localhost:5173`, `http://localhost:3000`
- Credentials: allowed
- Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS

**Production:** Restrict `allowedOrigins` to your actual frontend domain. Remove wildcard if present.

## Correlation IDs

Every request gets a `X-Correlation-Id` header:
- Generated at gateway if not present
- Propagated to all downstream services via Feign interceptors
- Included in all log lines via MDC
- Returned in response headers
- Never contains sensitive data

## Known Limitations

1. No refresh token — users must re-login after 24h
2. In-memory rate limiting — not suitable for multi-instance without Redis
3. JWT stored in localStorage — vulnerable to XSS (use httpOnly cookies in production)
4. No HTTPS in local Docker setup — add TLS termination in production
5. Webhook secret not required for local dev — always configure in production
