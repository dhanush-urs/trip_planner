package com.tripforge.external.service;

import com.tripforge.external.config.ProviderProperties;
import com.tripforge.external.dto.ProviderHealthDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Provider health service.
 * Reports which providers are configured and reachable.
 * Never leaks API keys — only reports configured/enabled status.
 * Results cached for 1 minute to avoid hammering providers on health polls.
 */
@Service
public class ProviderHealthService {

    private static final Logger log = LoggerFactory.getLogger(ProviderHealthService.class);

    @Autowired
    private ProviderProperties props;

    @Autowired
    private RestTemplate restTemplate;

    @Cacheable(value = "provider-health", key = "'all'")
    public ProviderHealthDto getProviderHealth() {
        Map<String, ProviderHealthDto.ProviderStatus> statuses = new LinkedHashMap<>();

        // ── Free providers (primary) ──────────────────────────────────────────
        statuses.put("opentripmap", ProviderHealthDto.ProviderStatus.builder()
                .configured(props.isOpenTripMapConfigured())
                .enabled(props.getOpentripmap().isEnabled())
                .status(props.isOpenTripMapConfigured() ? "CONFIGURED" : "NOT_CONFIGURED")
                .role("PRIMARY")
                .purpose("Hotel search, attraction search (free tier — primary provider)")
                .build());

        statuses.put("openrouteservice", ProviderHealthDto.ProviderStatus.builder()
                .configured(props.isOpenRouteServiceConfigured())
                .enabled(props.getOpenrouteservice().isEnabled())
                .status(props.isOpenRouteServiceConfigured() ? "CONFIGURED" : "NOT_CONFIGURED")
                .role("PRIMARY")
                .purpose("Route optimization, travel time estimation (free tier — primary provider)")
                .build());

        statuses.put("frankfurter", ProviderHealthDto.ProviderStatus.builder()
                .configured(true)   // No API key required
                .enabled(props.getFrankfurter().isEnabled())
                .status(checkFrankfurterReachability())
                .role("PRIMARY")
                .purpose("Currency exchange rates (ECB rates, no API key required)")
                .build());

        statuses.put("nominatim", ProviderHealthDto.ProviderStatus.builder()
                .configured(true)   // No API key required
                .enabled(true)
                .status("UP")
                .role("PRIMARY")
                .purpose("City geocoding via OpenStreetMap (no API key required)")
                .build());

        statuses.put("heuristic_fallback", ProviderHealthDto.ProviderStatus.builder()
                .configured(true)
                .enabled(true)
                .status("UP")
                .role("FALLBACK")
                .purpose("Route optimization fallback — always available, no external calls")
                .build());

        // ── Optional providers (Google — disabled by default) ─────────────────
        statuses.put("google_places", ProviderHealthDto.ProviderStatus.builder()
                .configured(props.isGooglePlacesConfigured())
                .enabled(props.getGoogle().getPlaces().isEnabled())
                .status(props.isGooglePlacesConfigured() ? "CONFIGURED" : "DISABLED")
                .role("OPTIONAL")
                .purpose("Optional: hotel/attraction search (requires paid Google API key)")
                .build());

        statuses.put("google_directions", ProviderHealthDto.ProviderStatus.builder()
                .configured(props.isGoogleDirectionsConfigured())
                .enabled(props.getGoogle().getDirections().isEnabled())
                .status(props.isGoogleDirectionsConfigured() ? "CONFIGURED" : "DISABLED")
                .role("OPTIONAL")
                .purpose("Optional: route optimization (requires paid Google API key)")
                .build());

        return ProviderHealthDto.builder().providers(statuses).build();
    }

    private String checkFrankfurterReachability() {
        if (!props.getFrankfurter().isEnabled()) return "DISABLED";
        try {
            restTemplate.getForObject(
                    props.getFrankfurter().getBaseUrl() + "/latest?from=USD&to=EUR",
                    String.class);
            return "UP";
        } catch (Exception e) {
            log.debug("Frankfurter reachability check failed: {}", e.getMessage());
            return "DOWN";
        }
    }
}
