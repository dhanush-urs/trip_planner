# TripForge — Provider Fallback Strategy

## Overview

TripForge uses a layered fallback strategy for all external data.
The system **never hard-fails** due to a provider being unavailable.
Every response includes metadata indicating which provider served the data.

---

## Fallback Chains

### Hotels

```
1. Google Places API (type=lodging)   → PRIMARY
   ↓ if unavailable or quota exceeded
2. Local CSV dataset (hotel-service)  → FALLBACK
   ↓ if CSV also empty
3. Empty list with warning            → DEGRADED
```

### Attractions / POIs

```
1. Google Places API (type=tourist_attraction) → PRIMARY
   ↓ if unavailable
2. OpenTripMap API                             → FALLBACK
   ↓ if unavailable
3. Local CSV dataset (route-service)           → FALLBACK
   ↓ if CSV also empty
4. Empty list with warning                     → DEGRADED
```

### Route Optimization

```
1. Google Directions API (with waypoint optimization) → PRIMARY
   ↓ if unavailable
2. Heuristic nearest-neighbor (no external calls)     → FALLBACK (always succeeds)
```

### Currency Exchange Rates

```
1. Frankfurter API (ECB rates, no API key required) → PRIMARY
   ↓ if unreachable
2. Redis-cached rate (from previous successful call) → CACHED FALLBACK
   ↓ if no cached rate
3. In-memory last-known rate (hardcoded approximations) → LAST RESORT
```

### AI / LLM (Phase 9D)

```
1. Gemini API                          → PRIMARY
   ↓ if unavailable or quota exceeded
2. Deterministic template response     → FALLBACK (always succeeds)
```

---

## Response Metadata

Every response from `external-data-service` includes:

```json
{
  "data": { ... },
  "sourceProvider": "google_places",
  "fallbackUsed": false,
  "degradedMode": false,
  "warnings": []
}
```

| Field | Description |
|---|---|
| `sourceProvider` | Which provider actually served the data |
| `fallbackUsed` | `true` if a fallback provider was used |
| `degradedMode` | `true` if data quality is reduced (stale cache, approximations) |
| `warnings` | Human-readable notes about data quality |

---

## Running Without API Keys

The system works without any API keys configured.

| Feature | Without Keys | With Keys |
|---|---|---|
| Hotel recommendations | CSV dataset (45 hotels, 5 cities) | Live Google Places data |
| Attractions | CSV dataset (50 attractions, 5 cities) | Live Google Places + OpenTripMap |
| Route optimization | Heuristic nearest-neighbor | Google Directions (real travel times) |
| Currency conversion | Hardcoded approximate rates | Live ECB rates via Frankfurter |
| AI explanations | Template responses | Gemini-generated explanations |

---

## Provider Health Check

```bash
curl http://localhost:8088/api/external/providers/health
```

Response shows which providers are configured and reachable:

```json
{
  "providers": {
    "google_places":      { "configured": false, "status": "NOT_CONFIGURED", "role": "PRIMARY" },
    "google_directions":  { "configured": false, "status": "NOT_CONFIGURED", "role": "PRIMARY" },
    "opentripmap":        { "configured": false, "status": "NOT_CONFIGURED", "role": "FALLBACK" },
    "frankfurter":        { "configured": true,  "status": "UP",             "role": "PRIMARY" },
    "heuristic_fallback": { "configured": true,  "status": "UP",             "role": "FALLBACK" }
  }
}
```

---

## Cache TTLs

| Cache | TTL | Reason |
|---|---|---|
| `places-search` | 30 min | Search results change infrequently |
| `place-details` | 1 hour | Place details are stable |
| `hotel-search` | 30 min | Prices can change |
| `route-optimize` | 30 min | Routes stable for a given set of places |
| `fx-rates` | 6 hours | FX rates change slowly |
| `provider-health` | 1 min | Health checks should be fresh |

FX rates are also pre-warmed every 4 hours via a scheduled job.
