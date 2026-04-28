package com.tripforge.external.provider;

import com.tripforge.external.config.ProviderProperties;
import com.tripforge.external.dto.RouteDto;
import com.tripforge.external.dto.RouteOptimizeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Google Directions API provider for route optimization.
 *
 * Uses the Directions API with waypoint optimization to produce
 * an ordered route with travel times between stops.
 */
@Component
public class GoogleDirectionsProvider {

    private static final Logger log = LoggerFactory.getLogger(GoogleDirectionsProvider.class);
    private static final String PROVIDER_NAME = "google_directions";

    private final ProviderProperties props;
    private final RestTemplate restTemplate;

    public GoogleDirectionsProvider(ProviderProperties props,
                                    @Qualifier("providerRestTemplate") RestTemplate restTemplate) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    /**
     * Optimize a route through a list of waypoints.
     * Returns ordered stops with travel times.
     */
    public RouteDto optimizeRoute(RouteOptimizeRequest request) {
        if (!props.isGoogleDirectionsConfigured()) {
            log.debug("Google Directions not configured — skipping");
            return null;
        }

        List<RouteOptimizeRequest.Waypoint> waypoints = request.getWaypoints();
        if (waypoints == null || waypoints.size() < 2) {
            log.debug("Not enough waypoints for route optimization");
            return null;
        }

        try {
            // Origin = first waypoint, destination = last, intermediates = middle
            RouteOptimizeRequest.Waypoint origin = waypoints.get(0);
            RouteOptimizeRequest.Waypoint destination = waypoints.get(waypoints.size() - 1);

            String originStr = origin.getLat() + "," + origin.getLng();
            String destStr = destination.getLat() + "," + destination.getLng();

            // Build waypoints string (pipe-separated, with optimize:true prefix)
            String waypointsStr = "";
            if (waypoints.size() > 2) {
                waypointsStr = "optimize:true|" + waypoints.subList(1, waypoints.size() - 1)
                        .stream()
                        .map(w -> w.getLat() + "," + w.getLng())
                        .collect(Collectors.joining("|"));
            }

            UriComponentsBuilder builder = UriComponentsBuilder
                    .fromHttpUrl(props.getGoogle().getDirections().getBaseUrl() + "/directions/json")
                    .queryParam("origin", originStr)
                    .queryParam("destination", destStr)
                    .queryParam("mode", request.getTravelMode().toLowerCase())
                    .queryParam("key", props.getGoogle().getDirections().getApiKey());

            if (!waypointsStr.isEmpty()) {
                builder.queryParam("waypoints", waypointsStr);
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(
                    builder.build().toUriString(), Map.class);

            if (response == null) return null;

            String status = (String) response.get("status");
            if (!"OK".equals(status)) {
                log.warn("Google Directions returned status: {}", status);
                return null;
            }

            return parseDirectionsResponse(response, waypoints);

        } catch (Exception e) {
            log.warn("Google Directions optimization failed: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private RouteDto parseDirectionsResponse(Map<String, Object> response,
                                              List<RouteOptimizeRequest.Waypoint> originalWaypoints) {
        try {
            List<Map<String, Object>> routes = (List<Map<String, Object>>) response.get("routes");
            if (routes == null || routes.isEmpty()) return null;

            Map<String, Object> route = routes.get(0);

            // Waypoint order from Google (optimized order indices)
            List<Integer> waypointOrder = (List<Integer>) route.get("waypoint_order");

            List<Map<String, Object>> legs = (List<Map<String, Object>>) route.get("legs");
            if (legs == null) return null;

            // Build ordered stops
            List<RouteDto.RouteStop> stops = new ArrayList<>();
            int totalTravelMinutes = 0;
            double totalDistanceKm = 0;

            // First stop (origin)
            RouteOptimizeRequest.Waypoint firstWp = originalWaypoints.get(0);
            stops.add(RouteDto.RouteStop.builder()
                    .order(0)
                    .placeId(firstWp.getPlaceId())
                    .name(firstWp.getName())
                    .lat(firstWp.getLat())
                    .lng(firstWp.getLng())
                    .travelTimeFromPreviousMinutes(0)
                    .distanceFromPreviousKm(0.0)
                    .estimatedVisitMinutes(firstWp.getEstimatedVisitMinutes())
                    .build());

            // Intermediate + final stops from legs
            for (int i = 0; i < legs.size(); i++) {
                Map<String, Object> leg = legs.get(i);

                Map<String, Object> duration = (Map<String, Object>) leg.get("duration");
                Map<String, Object> distance = (Map<String, Object>) leg.get("distance");

                int legMinutes = duration != null ? ((Number) duration.get("value")).intValue() / 60 : 0;
                double legKm = distance != null ? ((Number) distance.get("value")).doubleValue() / 1000.0 : 0;

                totalTravelMinutes += legMinutes;
                totalDistanceKm += legKm;

                // Determine which original waypoint this leg ends at
                RouteOptimizeRequest.Waypoint wp;
                if (i < legs.size() - 1 && waypointOrder != null && i < waypointOrder.size()) {
                    // +1 because index 0 is origin, waypoints start at index 1
                    int originalIdx = waypointOrder.get(i) + 1;
                    wp = originalIdx < originalWaypoints.size()
                            ? originalWaypoints.get(originalIdx)
                            : originalWaypoints.get(originalWaypoints.size() - 1);
                } else {
                    wp = originalWaypoints.get(originalWaypoints.size() - 1);
                }

                stops.add(RouteDto.RouteStop.builder()
                        .order(i + 1)
                        .placeId(wp.getPlaceId())
                        .name(wp.getName())
                        .lat(wp.getLat())
                        .lng(wp.getLng())
                        .travelTimeFromPreviousMinutes(legMinutes)
                        .distanceFromPreviousKm(Math.round(legKm * 10.0) / 10.0)
                        .estimatedVisitMinutes(wp.getEstimatedVisitMinutes())
                        .build());
            }

            return RouteDto.builder()
                    .stops(stops)
                    .totalTravelMinutes(totalTravelMinutes)
                    .totalDistanceKm(Math.round(totalDistanceKm * 10.0) / 10.0)
                    .sourceProvider(PROVIDER_NAME)
                    .fallbackUsed(false)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to parse Google Directions response: {}", e.getMessage());
            return null;
        }
    }

    public String getProviderName() { return PROVIDER_NAME; }
}
