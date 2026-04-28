package com.tripforge.external.controller;

import com.tripforge.external.dto.ProviderResponse;
import com.tripforge.external.dto.RouteDto;
import com.tripforge.external.dto.RouteOptimizeRequest;
import com.tripforge.external.service.RouteService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Route optimization controller.
 *
 * POST /api/external/routes/optimize — optimize a route through waypoints
 */
@RestController
@RequestMapping("/api/external/routes")
public class RouteController {

    @Autowired
    private RouteService routeService;

    /**
     * Optimize a route through a list of waypoints.
     * Returns ordered stops with travel times between each stop.
     *
     * Falls back to heuristic nearest-neighbor if Google Directions is unavailable.
     * Never returns null — always returns a valid route.
     *
     * Example request:
     * POST /api/external/routes/optimize
     * {
     *   "waypoints": [
     *     {"placeId": "p1", "name": "Baga Beach", "lat": 15.5553, "lng": 73.7517, "estimatedVisitMinutes": 120},
     *     {"placeId": "p2", "name": "Fort Aguada", "lat": 15.4924, "lng": 73.7731, "estimatedVisitMinutes": 90}
     *   ],
     *   "travelMode": "DRIVING",
     *   "optimizeFor": "TIME"
     * }
     */
    @PostMapping("/optimize")
    public ResponseEntity<ProviderResponse<RouteDto>> optimizeRoute(
            @Valid @RequestBody RouteOptimizeRequest request) {

        ProviderResponse<RouteDto> response = routeService.optimizeRoute(request);
        return ResponseEntity.ok(response);
    }
}
