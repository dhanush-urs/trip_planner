package com.tripforge.split.controller;

import com.tripforge.split.dto.ApiResponse;
import com.tripforge.split.dto.SplitRequest;
import com.tripforge.split.dto.SplitResultDto;
import com.tripforge.split.service.SplitService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Split controller — Phase 9F upgrade.
 *
 * Endpoints:
 *   POST /api/split/equal              — equal split (typed or legacy map)
 *   POST /api/split/custom-percentage  — custom % split (NEW)
 *   POST /api/split/custom-amount      — custom amount split (NEW)
 *   POST /api/split/custom             — legacy alias for custom-percentage
 *   GET  /api/split/{tripId}           — get stored split
 */
@RestController
@RequestMapping("/api/split")
public class SplitController {

    @Autowired
    private SplitService splitService;

    /** POST /api/split/equal — typed request */
    @PostMapping("/equal")
    public ResponseEntity<ApiResponse<SplitResultDto>> splitEqual(
            @Valid @RequestBody SplitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(splitService.splitEqual(request)));
    }

    /** POST /api/split/equal/legacy — backward-compatible map-based (used by trip-service Feign) */
    @PostMapping("/equal/legacy")
    public ResponseEntity<ApiResponse<SplitResultDto>> splitEqualLegacy(
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.success(splitService.splitEqual(request)));
    }

    /** POST /api/split/custom-percentage — custom percentage split */
    @PostMapping("/custom-percentage")
    public ResponseEntity<ApiResponse<SplitResultDto>> splitCustomPercentage(
            @Valid @RequestBody SplitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(splitService.splitCustomPercentage(request)));
    }

    /** POST /api/split/custom-amount — custom amount split */
    @PostMapping("/custom-amount")
    public ResponseEntity<ApiResponse<SplitResultDto>> splitCustomAmount(
            @Valid @RequestBody SplitRequest request) {
        return ResponseEntity.ok(ApiResponse.success(splitService.splitCustomAmount(request)));
    }

    /** POST /api/split/custom — legacy alias (backward-compatible) */
    @PostMapping("/custom")
    public ResponseEntity<ApiResponse<SplitResultDto>> splitCustomLegacy(
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.success(splitService.splitCustom(request)));
    }

    /** GET /api/split/{tripId} */
    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<SplitResultDto>> getByTripId(
            @PathVariable Long tripId) {
        return ResponseEntity.ok(ApiResponse.success(splitService.getByTripId(tripId)));
    }
}
