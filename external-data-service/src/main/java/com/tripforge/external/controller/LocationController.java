package com.tripforge.external.controller;

import com.tripforge.external.dto.LocationSuggestionDto;
import com.tripforge.external.dto.ProviderResponse;
import com.tripforge.external.service.LocationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Destination / location search controller.
 *
 * Powers the frontend destination autocomplete input.
 * Uses Nominatim (OpenStreetMap) as the primary free provider.
 *
 * GET /api/external/locations/search?q=<query>
 *
 * Example:
 *   GET /api/external/locations/search?q=Goa
 *   GET /api/external/locations/search?q=Tokyo
 *   GET /api/external/locations/search?q=Paris
 */
@RestController
@RequestMapping("/api/external/locations")
public class LocationController {

    private final LocationService locationService;

    public LocationController(LocationService locationService) {
        this.locationService = locationService;
    }

    /**
     * Search for destination suggestions matching a free-text query.
     *
     * @param q the search query (min 2 chars recommended)
     * @return list of location suggestions; empty list if none found or provider unavailable
     *
     * Response shape:
     * {
     *   "data": [
     *     {
     *       "displayName": "Goa, Goa State, India",
     *       "city": "Goa",
     *       "state": "Goa State",
     *       "country": "India",
     *       "countryCode": "IN",
     *       "lat": 15.2993265,
     *       "lng": 74.123996,
     *       "sourceProvider": "nominatim"
     *     }
     *   ],
     *   "sourceProvider": "nominatim",
     *   "fallbackUsed": false,
     *   "degradedMode": false
     * }
     */
    @GetMapping("/search")
    public ResponseEntity<ProviderResponse<List<LocationSuggestionDto>>> searchLocations(
            @RequestParam(name = "q") String q) {

        // Require at least 3 characters — 2-char queries return too many ambiguous
        // country-level results from Nominatim (e.g. "du" → France, UAE, China)
        if (q == null || q.trim().length() < 3) {
            return ResponseEntity.ok(ProviderResponse.of(List.of(), "none"));
        }

        ProviderResponse<List<LocationSuggestionDto>> response =
                locationService.searchLocations(q.trim());
        return ResponseEntity.ok(response);
    }
}
