package com.tripforge.gateway.config;

import com.tripforge.gateway.filter.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gateway route configuration.
 *
 * Routes all /api/** traffic to the appropriate microservice.
 * JWT filter is applied to all routes — the filter itself handles
 * the public path whitelist (/api/auth/**, /actuator/**).
 *
 * Service discovery via Eureka uses lb:// (load-balanced) URIs.
 */
@Configuration
public class GatewayRoutesConfig {

    @Autowired
    private JwtAuthFilter jwtAuthFilter;

    @Bean
    public RouteLocator tripForgeRoutes(RouteLocatorBuilder builder) {
        return builder.routes()

                // ── Auth Service ──────────────────────────────────────────────
                // Public: /api/auth/register, /api/auth/login
                // Protected: /api/users/**
                .route("auth-service-public", r -> r
                        .path("/api/auth/**")
                        .uri("lb://auth-service"))

                .route("auth-service-users", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://auth-service"))

                // ── Trip Service ──────────────────────────────────────────────
                .route("trip-service", r -> r
                        .path("/api/trip/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://trip-service"))

                // ── Hotel Service ─────────────────────────────────────────────
                .route("hotel-service", r -> r
                        .path("/api/hotels/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://hotel-service"))

                // ── Route Service ─────────────────────────────────────────────
                .route("route-service", r -> r
                        .path("/api/route/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://route-service"))

                // ── Budget Service ────────────────────────────────────────────
                .route("budget-service", r -> r
                        .path("/api/budget/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://budget-service"))

                // ── Split Service ─────────────────────────────────────────────
                .route("split-service", r -> r
                        .path("/api/split/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://split-service"))

                // ── ML Service ────────────────────────────────────────────────
                // ML service is internal — called by hotel-service, not directly by frontend
                // But exposed here for admin/testing purposes
                .route("ml-service", r -> r
                        .path("/api/ml/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("http://ml-service:8087"))

                // ── Actuator (health checks — public) ─────────────────────────
                .route("gateway-actuator", r -> r
                        .path("/actuator/**")
                        .uri("http://localhost:8080"))

                .build();
    }
}
