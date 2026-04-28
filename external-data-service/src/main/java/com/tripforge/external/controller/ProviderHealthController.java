package com.tripforge.external.controller;

import com.tripforge.external.dto.ProviderHealthDto;
import com.tripforge.external.service.ProviderHealthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Provider health controller.
 *
 * GET /api/external/providers/health — show which providers are configured and reachable
 *
 * Used by:
 *   - make health script
 *   - monitoring dashboards
 *   - frontend degraded-mode badges
 */
@RestController
@RequestMapping("/api/external/providers")
public class ProviderHealthController {

    @Autowired
    private ProviderHealthService providerHealthService;

    /**
     * Returns health status of all configured external providers.
     * Never leaks API keys — only reports configured/enabled/reachable status.
     *
     * Example response:
     * {
     *   "providers": {
     *     "google_places": { "configured": true, "enabled": true, "status": "CONFIGURED", "role": "PRIMARY" },
     *     "frankfurter":   { "configured": true, "enabled": true, "status": "UP",         "role": "PRIMARY" },
     *     "heuristic_fallback": { "configured": true, "enabled": true, "status": "UP",    "role": "FALLBACK" }
     *   }
     * }
     */
    @GetMapping("/health")
    public ResponseEntity<ProviderHealthDto> getProviderHealth() {
        return ResponseEntity.ok(providerHealthService.getProviderHealth());
    }
}
