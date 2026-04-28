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
                //
                // Uses static Docker DNS (http://auth-service:8081) instead of
                // lb://auth-service to avoid Eureka startup race conditions.
                // Auth is the most critical path — it must work immediately on boot.
                // All other services use lb:// (Eureka) for load-balanced discovery.
                .route("auth-service-public", r -> r
                        .path("/api/auth/**")
                        .uri("http://auth-service:8081"))

                .route("auth-service-users", r -> r
                        .path("/api/users/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("http://auth-service:8081"))

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

                // ── External Data Service (Phase 9B) ─────────────────────────
                // Internal service — called by hotel-service, route-service, trip-service
                // Exposed via gateway for admin/testing and future frontend use.
                // Location search (/api/external/locations/**) is PUBLIC — used by the
                // destination autocomplete on the Plan Trip page (pre-trip, may be unauthenticated).
                .route("external-data-locations-public", r -> r
                        .path("/api/external/locations/**")
                        .uri("lb://external-data-service"))

                .route("external-data-service", r -> r
                        .path("/api/external/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://external-data-service"))

                // ── AI Orchestrator Service (Phase 9D) ────────────────────────
                // Gemini-powered AI enrichment — hotel/itinerary explanations, preference parsing
                // Public parse-preferences endpoint; all others require JWT
                .route("ai-orchestrator-public", r -> r
                        .path("/api/ai/health")
                        .uri("lb://ai-orchestrator-service"))

                .route("ai-orchestrator-service", r -> r
                        .path("/api/ai/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://ai-orchestrator-service"))

                // ── Payment Service (Phase 9F) ────────────────────────────────
                // Webhook is public (signature-verified); all other payment endpoints require JWT
                .route("payment-webhook", r -> r
                        .path("/api/payments/webhook")
                        .uri("lb://payment-service"))

                .route("payment-service", r -> r
                        .path("/api/payments/**")
                        .filters(f -> f.filter(jwtAuthFilter.apply(new JwtAuthFilter.Config())))
                        .uri("lb://payment-service"))

                // ── Actuator (health checks — public) ─────────────────────────
                .route("gateway-actuator", r -> r
                        .path("/actuator/**")
                        .uri("http://localhost:8080"))

                .build();
    }
}
