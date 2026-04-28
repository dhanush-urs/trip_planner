package com.tripforge.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Gemini API configuration properties.
 * Bound from application.yml gemini.* namespace.
 * Never logs or exposes the API key.
 */
@Component
@ConfigurationProperties(prefix = "gemini")
@Data
public class GeminiProperties {

    private String apiKey = "";
    private String model = "gemini-1.5-flash";
    private boolean enabled = true;
    private int timeoutMs = 5000;
    private String baseUrl = "https://generativelanguage.googleapis.com/v1beta";
    private int maxRetries = 2;
    private double temperature = 0.1;

    /** Returns true if Gemini is enabled AND an API key is configured */
    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}
