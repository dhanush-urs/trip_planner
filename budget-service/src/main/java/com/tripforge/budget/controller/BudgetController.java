package com.tripforge.budget.controller;

import com.tripforge.budget.dto.ApiResponse;
import com.tripforge.budget.dto.BudgetBreakdownDto;
import com.tripforge.budget.service.BudgetService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/budget")
public class BudgetController {

    @Autowired
    private BudgetService budgetService;

    /** POST /api/budget/calculate */
    @PostMapping("/calculate")
    public ResponseEntity<ApiResponse<BudgetBreakdownDto>> calculate(
            @RequestBody Map<String, Object> request) {
        BudgetBreakdownDto result = budgetService.calculate(request);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    /** GET /api/budget/{tripId} */
    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<BudgetBreakdownDto>> getByTripId(
            @PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.success(budgetService.getByTripId(tripId)));
    }
}
