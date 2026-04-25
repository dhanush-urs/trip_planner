package com.tripforge.trip.client;

import com.tripforge.trip.dto.ApiResponse;
import com.tripforge.trip.dto.BudgetBreakdownDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

/**
 * Feign client for budget-service.
 */
@FeignClient(name = "budget-service", path = "/api/budget")
public interface BudgetServiceClient {

    @PostMapping("/calculate")
    ApiResponse<BudgetBreakdownDto> calculateBudget(@RequestBody Map<String, Object> request);
}
