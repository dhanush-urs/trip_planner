package com.tripforge.trip.client;

import com.tripforge.trip.dto.ApiResponse;
import com.tripforge.trip.dto.SplitResultDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client for split-service.
 */
@FeignClient(name = "split-service", path = "/api/split")
public interface SplitServiceClient {

    @PostMapping("/equal")
    ApiResponse<SplitResultDto> splitEqual(@RequestBody Map<String, Object> request);
}
