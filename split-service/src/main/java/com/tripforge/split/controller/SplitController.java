package com.tripforge.split.controller;

import com.tripforge.split.dto.ApiResponse;
import com.tripforge.split.dto.SplitResultDto;
import com.tripforge.split.service.SplitService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/split")
public class SplitController {

    @Autowired
    private SplitService splitService;

    /** POST /api/split/equal */
    @PostMapping("/equal")
    public ResponseEntity<ApiResponse<SplitResultDto>> splitEqual(
            @RequestBody Map<String, Object> request) {
        return ResponseEntity.ok(ApiResponse.success(splitService.splitEqual(request)));
    }

    /** POST /api/split/custom */
    @PostMapping("/custom")
    public ResponseEntity<ApiResponse<SplitResultDto>> splitCustom(
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
