package com.tripforge.trip.client;

import com.tripforge.trip.dto.ApiResponse;
import com.tripforge.trip.dto.ItineraryDayDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * Feign client for route-service.
 */
@FeignClient(name = "route-service", path = "/api/route")
public interface RouteServiceClient {

    @PostMapping("/optimize")
    ApiResponse<List<ItineraryDayDto>> optimizeRoute(@RequestBody Map<String, Object> request);
}
