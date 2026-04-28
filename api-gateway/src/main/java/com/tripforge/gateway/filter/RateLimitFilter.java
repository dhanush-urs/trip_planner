package com.tripforge.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lightweight in-memory rate limiter for sensitive endpoints.
 *
 * Uses a sliding window per IP address.
 * No Redis dependency — suitable for single-instance local/demo deployment.
 * For production multi-instance: replace with Redis-backed rate limiter.
 *
 * Limits:
 *   /api/auth/login     → 5 req/min per IP
 *   /api/auth/register  → 5 req/min per IP
 *   /api/payments/**    → 10 req/min per IP
 *   /api/external/**    → 20 req/min per IP
 *   /api/ai/**          → 20 req/min per IP
 */
@Component
public class RateLimitFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    // key: "path_prefix:ip" → [count, windowStartEpochSecond]
    private final Map<String, long[]> windowMap = new ConcurrentHashMap<>();

    private static final Map<String, int[]> LIMITS = Map.of(
            "/api/auth/login",    new int[]{5,  60},
            "/api/auth/register", new int[]{5,  60},
            "/api/payments",      new int[]{10, 60},
            "/api/external",      new int[]{20, 60},
            "/api/ai",            new int[]{20, 60}
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        String ip   = getClientIp(request);

        for (Map.Entry<String, int[]> entry : LIMITS.entrySet()) {
            if (path.startsWith(entry.getKey())) {
                int maxReq    = entry.getValue()[0];
                int windowSec = entry.getValue()[1];
                String key    = entry.getKey() + ":" + ip;

                if (!isAllowed(key, maxReq, windowSec)) {
                    log.warn("Rate limit exceeded: path={} ip={}", path, ip);
                    ServerHttpResponse response = exchange.getResponse();
                    response.setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
                    response.getHeaders().add("Content-Type", "application/json");
                    response.getHeaders().add("Retry-After", String.valueOf(windowSec));
                    String body = String.format(
                            "{\"success\":false,\"message\":\"Rate limit exceeded. Max %d requests per %d seconds.\",\"error\":\"RATE_LIMITED\"}",
                            maxReq, windowSec);
                    var buffer = response.bufferFactory().wrap(body.getBytes());
                    return response.writeWith(Mono.just(buffer));
                }
                break;
            }
        }

        return chain.filter(exchange);
    }

    private boolean isAllowed(String key, int maxReq, int windowSec) {
        long now = Instant.now().getEpochSecond();
        long[] window = windowMap.compute(key, (k, existing) -> {
            if (existing == null || now - existing[1] >= windowSec) {
                return new long[]{1, now};
            }
            existing[0]++;
            return existing;
        });
        return window[0] <= maxReq;
    }

    private String getClientIp(ServerHttpRequest request) {
        String forwarded = request.getHeaders().getFirst("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        var addr = request.getRemoteAddress();
        return addr != null ? addr.getAddress().getHostAddress() : "unknown";
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;  // After correlation ID, before JWT
    }
}
