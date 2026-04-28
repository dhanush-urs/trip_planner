package com.tripforge.route.controller;

import com.tripforge.route.dto.ApiResponse;
import com.tripforge.route.dto.DayPlanDto;
import com.tripforge.route.dto.RouteOptimizeRequest;
import com.tripforge.route.service.RouteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/route")
public class RouteController {

    @Autowired
    private RouteService routeService;

    /**
     * POST /api/route/optimize
     * Accepts a map from trip-service and builds the itinerary.
     * Phase 9C: also reads optional livePlaces for live route optimization.
     */
    @PostMapping("/optimize")
    public ResponseEntity<ApiResponse<List<DayPlanDto>>> optimizeRoute(
            @RequestBody Map<String, Object> requestMap) {

        RouteOptimizeRequest request = new RouteOptimizeRequest();
        request.setDestination((String) requestMap.get("destination"));
        request.setDurationDays((Integer) requestMap.get("durationDays"));
        request.setTripId(requestMap.get("tripId") != null
                ? ((Number) requestMap.get("tripId")).longValue() : null);

        if (requestMap.get("startDate") != null) {
            request.setStartDate(LocalDate.parse((String) requestMap.get("startDate")));
        }

        @SuppressWarnings("unchecked")
        List<String> interests = (List<String>) requestMap.get("interests");
        request.setInterests(interests);

        // Phase 9C: parse optional livePlaces
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> livePlacesRaw = (List<Map<String, Object>>) requestMap.get("livePlaces");
        if (livePlacesRaw != null && !livePlacesRaw.isEmpty()) {
            List<RouteOptimizeRequest.LivePlace> livePlaces = livePlacesRaw.stream()
                    .map(p -> {
                        RouteOptimizeRequest.LivePlace lp = new RouteOptimizeRequest.LivePlace();
                        lp.setPlaceId((String) p.get("placeId"));
                        lp.setName((String) p.get("name"));
                        lp.setCategory((String) p.get("category"));
                        if (p.get("lat") != null) lp.setLat(((Number) p.get("lat")).doubleValue());
                        if (p.get("lng") != null) lp.setLng(((Number) p.get("lng")).doubleValue());
                        if (p.get("estimatedVisitMinutes") != null)
                            lp.setEstimatedVisitMinutes(((Number) p.get("estimatedVisitMinutes")).intValue());
                        return lp;
                    }).collect(Collectors.toList());
            request.setLivePlaces(livePlaces);
        }

        List<DayPlanDto> itinerary = routeService.optimizeRoute(request);
        return ResponseEntity.ok(ApiResponse.success(itinerary));
    }

    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<String>> getRoute(@PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Itinerary is stored in trip-service. Use GET /api/trip/" + tripId));
    }
}
