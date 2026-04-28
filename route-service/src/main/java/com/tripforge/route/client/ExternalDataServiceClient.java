package com.tripforge.route.client;

import com.tripforge.route.dto.ExternalRouteResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client for external-data-service route optimization.
 * Used by route-service to get real travel times and optimized stop order.
 *
 * Falls back gracefully — if external-data-service is down or returns empty,
 * route-service uses its existing CSV + heuristic logic.
 */
@FeignClient(name = "external-data-service", path = "/api/external")
public interface ExternalDataServiceClient {

    /**
     * Optimize a route through a list of waypoints.
     * Returns ProviderResponse<RouteDto> from external-data-service.
     */
    @PostMapping("/routes/optimize")
    ExternalRouteResponse optimizeRoute(@RequestBody Map<String, Object> request);
}
