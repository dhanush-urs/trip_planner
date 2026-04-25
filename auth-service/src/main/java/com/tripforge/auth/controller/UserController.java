package com.tripforge.auth.controller;

import com.tripforge.auth.dto.ApiResponse;
import com.tripforge.auth.dto.UserPreferenceDto;
import com.tripforge.auth.dto.UserProfileDto;
import com.tripforge.auth.service.UserService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * User profile and preferences controller.
 * All endpoints require a valid JWT (enforced by SecurityConfig + JwtAuthFilter).
 *
 * The user ID is extracted from the X-User-Id header forwarded by the API Gateway.
 * This avoids re-parsing the JWT in every service.
 *
 * GET /api/users/profile         — get current user's profile
 * GET /api/users/preferences     — get current user's travel preferences
 * PUT /api/users/preferences     — update travel preferences
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * Get the authenticated user's profile.
     *
     * Response 200:
     * {
     *   "success": true,
     *   "message": "Success",
     *   "data": {
     *     "id": 1,
     *     "email": "arjun@example.com",
     *     "firstName": "Arjun",
     *     "lastName": "Sharma",
     *     "phone": "9876543210",
     *     "role": "USER",
     *     "createdAt": "2024-01-15T10:30:00"
     *   }
     * }
     */
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileDto>> getProfile(
            @RequestHeader("X-User-Id") Long userId) {
        UserProfileDto profile = userService.getProfile(userId);
        return ResponseEntity.ok(ApiResponse.success(profile));
    }

    /**
     * Get the authenticated user's travel preferences.
     *
     * Response 200:
     * {
     *   "success": true,
     *   "data": {
     *     "interests": ["nature", "food", "beaches"],
     *     "hotelPreference": "STANDARD",
     *     "defaultBudget": 50000.00,
     *     "defaultTravelers": 2
     *   }
     * }
     */
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<UserPreferenceDto>> getPreferences(
            @RequestHeader("X-User-Id") Long userId) {
        UserPreferenceDto preferences = userService.getPreferences(userId);
        return ResponseEntity.ok(ApiResponse.success(preferences));
    }

    /**
     * Update the authenticated user's travel preferences.
     *
     * Request:
     * PUT /api/users/preferences
     * {
     *   "interests": ["nature", "food", "beaches"],
     *   "hotelPreference": "STANDARD",
     *   "defaultBudget": 50000.00,
     *   "defaultTravelers": 2
     * }
     *
     * Response 200:
     * {
     *   "success": true,
     *   "message": "Preferences updated successfully",
     *   "data": { ... updated preferences ... }
     * }
     */
    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<UserPreferenceDto>> updatePreferences(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UserPreferenceDto dto) {
        UserPreferenceDto updated = userService.updatePreferences(userId, dto);
        return ResponseEntity.ok(ApiResponse.success("Preferences updated successfully", updated));
    }
}
