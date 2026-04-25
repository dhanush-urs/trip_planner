package com.tripforge.hotel.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * Feign client for the Python ML service.
 * Uses direct URL (not Eureka) since ml-service is Python/FastAPI.
 */
@FeignClient(name = "ml-service", url = "${ml.service.url:http://ml-service:8087}")
public interface MlServiceClient {

    @PostMapping("/ml/hotel-rank")
    Map<String, Object> rankHotels(@RequestBody Map<String, Object> request);

    @PostMapping("/ml/recommend-alternative-hotel")
    Map<String, Object> recommendAlternative(@RequestBody Map<String, Object> request);
}
