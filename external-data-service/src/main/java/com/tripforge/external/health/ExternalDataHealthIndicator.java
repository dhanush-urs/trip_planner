package com.tripforge.external.health;

import com.tripforge.external.config.ProviderProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Component;

/**
 * Custom Spring Boot Actuator health indicator for external-data-service.
 *
 * Exposes:
 *   - Redis connectivity
 *   - Which providers are configured (without leaking keys)
 *   - Overall service readiness
 *
 * Accessible at: GET /actuator/health
 */
@Component
public class ExternalDataHealthIndicator implements HealthIndicator {

    @Autowired
    private ProviderProperties props;

    @Autowired
    private RedisConnectionFactory redisConnectionFactory;

    @Override
    public Health health() {
        Health.Builder builder = Health.up();

        // Redis check
        boolean redisOk = checkRedis();
        builder.withDetail("redis", redisOk ? "UP" : "DOWN");

        // Provider configuration status (no keys leaked)
        builder.withDetail("google_places_configured", props.isGooglePlacesConfigured());
        builder.withDetail("google_directions_configured", props.isGoogleDirectionsConfigured());
        builder.withDetail("opentripmap_configured", props.isOpenTripMapConfigured());
        builder.withDetail("openrouteservice_configured", props.isOpenRouteServiceConfigured());
        builder.withDetail("frankfurter_enabled", props.getFrankfurter().isEnabled());
        builder.withDetail("heuristic_fallback", "always_available");

        // Degrade if Redis is down (caching won't work)
        if (!redisOk) {
            return builder.status("DEGRADED")
                    .withDetail("warning", "Redis unavailable — caching disabled, provider calls will not be cached")
                    .build();
        }

        return builder.build();
    }

    private boolean checkRedis() {
        try {
            redisConnectionFactory.getConnection().ping();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
