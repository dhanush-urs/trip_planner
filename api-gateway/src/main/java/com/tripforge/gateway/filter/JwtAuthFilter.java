package com.tripforge.gateway.filter;

import com.tripforge.gateway.util.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

/**
 * JWT Authentication Filter for Spring Cloud Gateway.
 *
 * Applied to all routes EXCEPT:
 *   - /api/auth/**  (login, register — public)
 *   - /actuator/**  (health checks — public)
 *
 * For protected routes:
 *   1. Checks for Authorization: Bearer <token> header
 *   2. Validates the JWT signature and expiry
 *   3. Forwards user identity headers (X-User-Id, X-User-Email) to downstream services
 *   4. Returns 401 if token is missing or invalid
 */
@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<JwtAuthFilter.Config> {

    private static final Logger log = LoggerFactory.getLogger(JwtAuthFilter.class);
    private static final String BEARER_PREFIX = "Bearer ";

    // Paths that bypass JWT validation
    private static final List<String> PUBLIC_PATHS = List.of(
            "/api/auth/register",
            "/api/auth/login",
            "/actuator"
    );

    @Autowired
    private JwtUtil jwtUtil;

    public JwtAuthFilter() {
        super(Config.class);
    }

    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            String path = request.getURI().getPath();

            // Skip JWT check for public paths
            if (isPublicPath(path)) {
                log.debug("Public path accessed, skipping JWT check: {}", path);
                return chain.filter(exchange);
            }

            // Check Authorization header
            if (!request.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                log.warn("Missing Authorization header for path: {}", path);
                return unauthorizedResponse(exchange, "Missing Authorization header");
            }

            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
                log.warn("Invalid Authorization header format for path: {}", path);
                return unauthorizedResponse(exchange, "Invalid Authorization header format");
            }

            String token = authHeader.substring(BEARER_PREFIX.length());

            // Validate token
            if (!jwtUtil.validateToken(token)) {
                log.warn("Invalid or expired JWT token for path: {}", path);
                return unauthorizedResponse(exchange, "Invalid or expired token");
            }

            // Extract user info and forward as headers to downstream services
            String userEmail = jwtUtil.extractSubject(token);
            Long userId = jwtUtil.extractUserId(token);

            log.debug("JWT validated for user: {} (id: {}), path: {}", userEmail, userId, path);

            // Mutate request to add user identity headers
            ServerHttpRequest mutatedRequest = request.mutate()
                    .header("X-User-Id", userId != null ? userId.toString() : "")
                    .header("X-User-Email", userEmail != null ? userEmail : "")
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());
        };
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private Mono<Void> unauthorizedResponse(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().add("Content-Type", "application/json");
        String body = String.format("{\"error\": \"Unauthorized\", \"message\": \"%s\"}", message);
        var buffer = response.bufferFactory().wrap(body.getBytes());
        return response.writeWith(Mono.just(buffer));
    }

    public static class Config {
        // Configuration properties can be added here if needed
    }
}
