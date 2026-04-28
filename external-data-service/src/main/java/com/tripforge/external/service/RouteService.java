package com.tripforge.external.service;

import com.tripforge.external.dto.ProviderResponse;
import com.tripforge.external.dto.RouteDto;
import com.tripforge.external.dto.RouteOptimizeRequest;
import com.tripforge.external.provider.GoogleDirectionsProvider;
import com.tripforge.external.provider.HeuristicRouteProvider;
import com.tripforge.external.provider.OpenRouteServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Route optimization service — orchestrates provider fallback chain.
 *
 * FREE-FIRST provider order (Phase 9G):
 *   1. OpenRouteService   (PRIMARY — free tier, 2000 req/day, no billing)
 *   2. Google Directions  (OPTIONAL — only if key configured)
 *   3. Heuristic fallback (ALWAYS available — nearest-neighbor, no external calls)
 *
 * The heuristic fallback ALWAYS succeeds — route optimization never fails.
 * Results are cached in Redis for 30 minutes.
 */
@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    @Autowired private OpenRouteServiceProvider openRouteService;
    @Autowired private GoogleDirectionsProvider googleDirections;
    @Autowired private HeuristicRouteProvider heuristicRoute;

    @Cacheable(value = "route-optimize", key = "#request.waypoints.![placeId].toString()")
    public ProviderResponse<RouteDto> optimizeRoute(RouteOptimizeRequest request) {
        if (request.getWaypoints() == null || request.getWaypoints().isEmpty()) {
            return ProviderResponse.degraded(
                    RouteDto.builder().stops(List.of()).totalTravelMinutes(0)
                            .totalDistanceKm(0.0).sourceProvider("none").fallbackUsed(true).build(),
                    "none", "No waypoints provided");
        }

        log.info("Optimizing route for {} waypoints", request.getWaypoints().size());

        // 1. Try OpenRouteService first (free, primary)
        RouteDto route = openRouteService.optimizeRoute(request);
        if (route != null) {
            log.info("OpenRouteService optimized route: {} stops, {} min",
                    route.getStops().size(), route.getTotalTravelMinutes());
            return ProviderResponse.of(route, "openrouteservice");
        }

        // 2. Try Google Directions if configured (optional)
        route = googleDirections.optimizeRoute(request);
        if (route != null) {
            log.info("Google Directions optimized route: {} stops, {} min",
                    route.getStops().size(), route.getTotalTravelMinutes());
            return ProviderResponse.fallback(route, "google_directions",
                    "OpenRouteService unavailable — using optional Google Directions");
        }

        // 3. Heuristic fallback — always succeeds
        log.info("Live route providers unavailable — using heuristic nearest-neighbor fallback");
        RouteDto heuristicResult = heuristicRoute.optimizeRoute(request);
        return ProviderResponse.fallback(heuristicResult, "heuristic_fallback",
                "No live route provider available — using nearest-neighbor heuristic with estimated travel times");
    }
}
