package com.tripforge.external.provider;

import com.tripforge.external.config.ProviderProperties;
import com.tripforge.external.dto.RouteDto;
import com.tripforge.external.dto.RouteOptimizeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * OpenRouteService provider for route optimization.
 *
 * Free tier: https://openrouteservice.org/dev/#/signup
 *   - 2,000 requests/day free
 *   - No credit card required
 *
 * Uses the Directions API (v2/directions/driving-car) with matrix optimization.
 * Falls back to heuristic if API key is absent or call fails.
 *
 * API docs: https://openrouteservice.org/dev/#/api-docs/v2/directions
 */
@Component
public class OpenRouteServiceProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenRouteServiceProvider.class);
    private static final String PROVIDER_NAME = "openrouteservice";

    private final ProviderProperties props;
    private final RestTemplate restTemplate;

    public OpenRouteServiceProvider(ProviderProperties props,
                                    @Qualifier("providerRestTemplate") RestTemplate restTemplate) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    public boolean isConfigured() {
        return props.isOpenRouteServiceConfigured();
    }

    /**
     * Optimize a route through waypoints using OpenRouteService Directions API.
     * Returns null on any failure — caller falls back to heuristic.
     */
    public RouteDto optimizeRoute(RouteOptimizeRequest request) {
        if (!isConfigured()) {
            log.debug("OpenRouteService not configured — skipping");
            return null;
        }

        List<RouteOptimizeRequest.Waypoint> waypoints = request.getWaypoints();
        if (waypoints == null || waypoints.size() < 2) return null;

        try {
            // Build coordinate array [[lng, lat], ...]
            List<List<Double>> coordinates = waypoints.stream()
                    .map(w -> List.of(w.getLng(), w.getLat()))
                    .collect(Collectors.toList());

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("coordinates", coordinates);
            body.put("instructions", false);
            body.put("geometry", false);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", props.getOpenrouteservice().getApiKey());

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            String url = props.getOpenrouteservice().getBaseUrl()
                    + "/v2/directions/driving-car/json";

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response =
                    restTemplate.exchange(url, HttpMethod.POST, entity,
                            (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getBody() == null) return null;

            return parseOrsResponse(response.getBody(), waypoints);

        } catch (Exception e) {
            log.warn("OpenRouteService optimization failed: {} — using heuristic fallback",
                    e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private RouteDto parseOrsResponse(Map<String, Object> response,
                                       List<RouteOptimizeRequest.Waypoint> waypoints) {
        try {
            List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
            if (routes == null || routes.isEmpty()) return null;

            Map<String, Object> route = routes.get(0);
            Map<String, Object> summary = (Map<String, Object>) route.get("summary");

            double totalDistanceM = summary != null
                    ? ((Number) summary.getOrDefault("distance", 0)).doubleValue() : 0;
            double totalDurationS = summary != null
                    ? ((Number) summary.getOrDefault("duration", 0)).doubleValue() : 0;

            List<Map<String, Object>> segments =
                    (List<Map<String, Object>>) route.get("segments");

            List<RouteDto.RouteStop> stops = new ArrayList<>();
            int totalMinutes = (int) (totalDurationS / 60);
            double totalKm = totalDistanceM / 1000.0;

            // First stop
            RouteOptimizeRequest.Waypoint first = waypoints.get(0);
            stops.add(RouteDto.RouteStop.builder()
                    .order(0).placeId(first.getPlaceId()).name(first.getName())
                    .lat(first.getLat()).lng(first.getLng())
                    .travelTimeFromPreviousMinutes(0).distanceFromPreviousKm(0.0)
                    .estimatedVisitMinutes(first.getEstimatedVisitMinutes())
                    .build());

            // Remaining stops from segments
            if (segments != null) {
                for (int i = 0; i < segments.size() && i + 1 < waypoints.size(); i++) {
                    Map<String, Object> seg = segments.get(i);
                    int segMinutes = (int) (((Number) seg.getOrDefault("duration", 0))
                            .doubleValue() / 60);
                    double segKm = ((Number) seg.getOrDefault("distance", 0))
                            .doubleValue() / 1000.0;

                    RouteOptimizeRequest.Waypoint wp = waypoints.get(i + 1);
                    stops.add(RouteDto.RouteStop.builder()
                            .order(i + 1).placeId(wp.getPlaceId()).name(wp.getName())
                            .lat(wp.getLat()).lng(wp.getLng())
                            .travelTimeFromPreviousMinutes(segMinutes)
                            .distanceFromPreviousKm(Math.round(segKm * 10.0) / 10.0)
                            .estimatedVisitMinutes(wp.getEstimatedVisitMinutes())
                            .build());
                }
            }

            log.info("OpenRouteService optimized {} stops, {} km, {} min",
                    stops.size(), Math.round(totalKm * 10.0) / 10.0, totalMinutes);

            return RouteDto.builder()
                    .stops(stops)
                    .totalTravelMinutes(totalMinutes)
                    .totalDistanceKm(Math.round(totalKm * 10.0) / 10.0)
                    .sourceProvider(PROVIDER_NAME)
                    .fallbackUsed(false)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse ORS response: {}", e.getMessage());
            return null;
        }
    }

    public String getProviderName() { return PROVIDER_NAME; }
}
