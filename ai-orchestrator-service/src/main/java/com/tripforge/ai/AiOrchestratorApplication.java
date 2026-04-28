package com.tripforge.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * TripForge AI Orchestrator Service
 *
 * Provides AI-powered enrichment on top of the live-data + fallback architecture.
 * Uses Gemini API for:
 *   - Preference parsing (free-form text → structured constraints)
 *   - Hotel explanation (why this hotel was chosen)
 *   - Itinerary explanation (why this route makes sense)
 *   - Replan feedback interpretation (free-form → canonical reason)
 *   - Trip summary generation
 *
 * CRITICAL: This service is stateless and non-blocking.
 * Trip creation NEVER fails because this service is down.
 * Every endpoint has a deterministic fallback.
 * Gemini is NEVER the source of truth for factual travel data.
 */
@SpringBootApplication
@EnableDiscoveryClient
public class AiOrchestratorApplication {

    public static void main(String[] args) {
        SpringApplication.run(AiOrchestratorApplication.class, args);
    }
}
