package com.tripforge.trip.dto;

import com.tripforge.trip.entity.Trip;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a new trip.
 *
 * Phase 9C additions (all optional — existing requests work unchanged):
 *   - currency          default INR; full multi-currency comes in Phase 9E
 *   - tripStyle         optional trip style hint
 *   - pace              RELAXED / BALANCED / PACKED
 *   - mustVisitPlaces   user-selected places with coordinates for live routing
 *   - excludedPlaceIds  places to exclude from itinerary
 */
@Data
public class TripCreateRequest {

    // ── Existing required fields (unchanged) ─────────────────────────────────

    @NotBlank(message = "Destination is required")
    @Size(max = 100)
    private String destination;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "1000.0", message = "Budget must be at least ₹1000")
    private BigDecimal totalBudget;

    @NotNull(message = "Number of travelers is required")
    @Min(value = 1, message = "At least 1 traveler required")
    @Max(value = 20, message = "Maximum 20 travelers supported")
    private Integer travelers;

    private List<String> interests;

    private Trip.HotelPreference hotelPreference;

    // ── Phase 9C optional additions ───────────────────────────────────────────

    /**
     * Currency code for the trip (default: INR).
     * Full multi-currency conversion comes in Phase 9E.
     * Stored now so schema is ready.
     */
    private String currency = "INR";

    /**
     * Trip style hint: BUDGET / FAMILY / NIGHTLIFE / ADVENTURE / LUXURY / CULTURAL
     * Used by ML classifier and (Phase 9D) Gemini preference parsing.
     */
    private String tripStyle;

    /**
     * Travel pace: RELAXED / BALANCED / PACKED
     * Affects attractions-per-day in itinerary generation.
     */
    private String pace;

    /**
     * User-selected must-visit places with coordinates.
     * When provided, route-service uses live route optimization.
     * When absent, falls back to CSV heuristic (existing behavior).
     */
    private List<MustVisitPlace> mustVisitPlaces;

    /**
     * Place IDs to exclude from itinerary (optional).
     */
    private List<String> excludedPlaceIds;

    /**
     * Destination latitude — populated when user selects from autocomplete.
     * Used for coordinate-based hotel search (Overpass OSM).
     */
    private Double destinationLat;

    /**
     * Destination longitude — populated when user selects from autocomplete.
     */
    private Double destinationLng;

    @AssertTrue(message = "End date must be after start date")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) return true;
        return endDate.isAfter(startDate);
    }

    @Data
    public static class MustVisitPlace {
        private String placeId;
        private String name;
        private String category;
        private Double lat;
        private Double lng;
        private Integer estimatedVisitMinutes;
    }
}
