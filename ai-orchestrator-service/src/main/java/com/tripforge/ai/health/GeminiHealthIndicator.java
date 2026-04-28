package com.tripforge.ai.health;

import com.tripforge.ai.config.GeminiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom Spring Boot Actuator health indicator for Gemini.
 * Reports configuration status without leaking the API key.
 * Service is UP even when Gemini is not configured (fallback mode).
 */
@Component
public class GeminiHealthIndicator implements HealthIndicator {

    @Autowired
    private GeminiProperties props;

    @Override
    public Health health() {
        if (props.isConfigured()) {
            return Health.up()
                    .withDetail("gemini_configured", true)
                    .withDetail("model", props.getModel())
                    .withDetail("mode", "LIVE")
                    .build();
        }
        // Degraded but not DOWN — fallback is always available
        return Health.status("DEGRADED")
                .withDetail("gemini_configured", false)
                .withDetail("mode", "FALLBACK")
                .withDetail("note", "Set GEMINI_API_KEY env var to enable live AI responses")
                .build();
    }
}
