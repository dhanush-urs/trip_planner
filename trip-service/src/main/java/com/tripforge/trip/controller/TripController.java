package com.tripforge.trip.controller;

import com.tripforge.trip.dto.*;
import com.tripforge.trip.service.TripService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Trip REST controller.
 * User identity comes from X-User-Id header forwarded by the API Gateway.
 */
@RestController
@RequestMapping("/api/trip")
public class TripController {

    @Autowired
    private TripService tripService;

    /** POST /api/trip/create — create and plan a new trip */
    @PostMapping("/create")
    public ResponseEntity<ApiResponse<TripResponse>> createTrip(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody TripCreateRequest request) {
        TripResponse response = tripService.createTrip(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Trip created successfully", response));
    }

    /** GET /api/trip/{tripId} — get full trip details */
    @GetMapping("/{tripId}")
    public ResponseEntity<ApiResponse<TripResponse>> getTrip(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long tripId) {
        TripResponse response = tripService.getTrip(tripId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /** GET /api/trip/user/{userId} — get all trips for a user */
    @GetMapping("/user/{userId}")
    public ResponseEntity<ApiResponse<List<TripSummaryDto>>> getUserTrips(
            @RequestHeader("X-User-Id") Long requestingUserId,
            @PathVariable Long userId) {
        // Users can only see their own trips
        List<TripSummaryDto> trips = tripService.getUserTrips(requestingUserId);
        return ResponseEntity.ok(ApiResponse.success(trips));
    }

    /** PUT /api/trip/replan — replan with a different hotel */
    @PutMapping("/replan")
    public ResponseEntity<ApiResponse<TripResponse>> replan(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ReplanRequest request) {
        TripResponse response = tripService.replan(userId, request);
        return ResponseEntity.ok(ApiResponse.success("Trip replanned successfully", response));
    }
}
