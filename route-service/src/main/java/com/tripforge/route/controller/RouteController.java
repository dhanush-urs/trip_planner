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

@RestController
@RequestMapping("/api/route")
public class RouteController {

    @Autowired
    private RouteService routeService;

    /**
     * POST /api/route/optimize
     * Accepts a map (from trip-service Feign call) and builds the itinerary.
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

        List<DayPlanDto> itinerary = routeService.optimizeRoute(request);
        return ResponseEntity.ok(ApiResponse.success(itinerary));
    }

    /** GET /api/route/{tripId} — returns stored itinerary (trip-service owns storage) */
    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<String>> getRoute(@PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.success(
                "Itinerary is stored in trip-service. Use GET /api/trip/" + tripId));
    }
}
