package com.tripforge.external.provider;

import com.tripforge.external.dto.RouteDto;
import com.tripforge.external.dto.RouteOptimizeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Heuristic route provider — fallback when Google Directions is unavailable.
 *
 * Uses a nearest-neighbor greedy algorithm to order waypoints geographically.
 * Estimates travel times using average urban speed (30 km/h).
 * No external API calls — always available.
 */
@Component
public class HeuristicRouteProvider {

    private static final Logger log = LoggerFactory.getLogger(HeuristicRouteProvider.class);
    private static final String PROVIDER_NAME = "heuristic_fallback";
    private static final double AVG_SPEED_KMH = 30.0;

    /**
     * Optimize route using nearest-neighbor heuristic.
     * Always succeeds — never returns null.
     */
    public RouteDto optimizeRoute(RouteOptimizeRequest request) {
        List<RouteOptimizeRequest.Waypoint> waypoints = request.getWaypoints();
        if (waypoints == null || waypoints.isEmpty()) {
            return RouteDto.builder()
                    .stops(List.of())
                    .totalTravelMinutes(0)
                    .totalDistanceKm(0.0)
                    .sourceProvider(PROVIDER_NAME)
                    .fallbackUsed(true)
                    .build();
        }

        if (waypoints.size() == 1) {
            RouteOptimizeRequest.Waypoint wp = waypoints.get(0);
            return RouteDto.builder()
                    .stops(List.of(RouteDto.RouteStop.builder()
                            .order(0).placeId(wp.getPlaceId()).name(wp.getName())
                            .lat(wp.getLat()).lng(wp.getLng())
                            .travelTimeFromPreviousMinutes(0).distanceFromPreviousKm(0.0)
                            .estimatedVisitMinutes(wp.getEstimatedVisitMinutes())
                            .build()))
                    .totalTravelMinutes(0)
                    .totalDistanceKm(0.0)
                    .sourceProvider(PROVIDER_NAME)
                    .fallbackUsed(true)
                    .build();
        }

        log.info("Using heuristic route optimization for {} waypoints", waypoints.size());

        // Nearest-neighbor from first waypoint
        List<RouteOptimizeRequest.Waypoint> remaining = new ArrayList<>(waypoints.subList(1, waypoints.size()));
        List<RouteOptimizeRequest.Waypoint> ordered = new ArrayList<>();
        ordered.add(waypoints.get(0));

        RouteOptimizeRequest.Waypoint current = waypoints.get(0);
        while (!remaining.isEmpty()) {
            RouteOptimizeRequest.Waypoint nearest = findNearest(current, remaining);
            ordered.add(nearest);
            remaining.remove(nearest);
            current = nearest;
        }

        // Build RouteDto from ordered waypoints
        List<RouteDto.RouteStop> stops = new ArrayList<>();
        int totalMinutes = 0;
        double totalKm = 0;

        for (int i = 0; i < ordered.size(); i++) {
            RouteOptimizeRequest.Waypoint wp = ordered.get(i);
            int travelMinutes = 0;
            double distKm = 0;

            if (i > 0) {
                RouteOptimizeRequest.Waypoint prev = ordered.get(i - 1);
                distKm = haversineKm(prev.getLat(), prev.getLng(), wp.getLat(), wp.getLng());
                travelMinutes = (int) Math.ceil((distKm / AVG_SPEED_KMH) * 60);
                totalMinutes += travelMinutes;
                totalKm += distKm;
            }

            stops.add(RouteDto.RouteStop.builder()
                    .order(i)
                    .placeId(wp.getPlaceId())
                    .name(wp.getName())
                    .lat(wp.getLat())
                    .lng(wp.getLng())
                    .travelTimeFromPreviousMinutes(travelMinutes)
                    .distanceFromPreviousKm(Math.round(distKm * 10.0) / 10.0)
                    .estimatedVisitMinutes(wp.getEstimatedVisitMinutes())
                    .build());
        }

        return RouteDto.builder()
                .stops(stops)
                .totalTravelMinutes(totalMinutes)
                .totalDistanceKm(Math.round(totalKm * 10.0) / 10.0)
                .sourceProvider(PROVIDER_NAME)
                .fallbackUsed(true)
                .build();
    }

    private RouteOptimizeRequest.Waypoint findNearest(RouteOptimizeRequest.Waypoint from,
                                                       List<RouteOptimizeRequest.Waypoint> candidates) {
        RouteOptimizeRequest.Waypoint nearest = null;
        double minDist = Double.MAX_VALUE;
        for (RouteOptimizeRequest.Waypoint candidate : candidates) {
            double dist = haversineKm(from.getLat(), from.getLng(),
                    candidate.getLat(), candidate.getLng());
            if (dist < minDist) {
                minDist = dist;
                nearest = candidate;
            }
        }
        return nearest;
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    public String getProviderName() { return PROVIDER_NAME; }
}
