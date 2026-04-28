package com.tripforge.external.controller;

import com.tripforge.external.dto.PlaceDto;
import com.tripforge.external.dto.ProviderResponse;
import com.tripforge.external.service.PlacesService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Places / POI search controller.
 *
 * GET /api/external/places/search  — search places in a city
 * GET /api/external/places/details/{placeId} — get place details
 */
@RestController
@RequestMapping("/api/external/places")
public class PlacesController {

    @Autowired
    private PlacesService placesService;

    /**
     * Search for places (attractions, restaurants, POIs) in a city.
     *
     * @param city  city name (required)
     * @param query search query, e.g. "beaches", "temples" (optional)
     * @param type  Google place type, e.g. "tourist_attraction" (optional)
     *
     * Example: GET /api/external/places/search?city=Goa&query=beaches&type=tourist_attraction
     */
    @GetMapping("/search")
    public ResponseEntity<ProviderResponse<List<PlaceDto>>> searchPlaces(
            @RequestParam String city,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String type) {

        ProviderResponse<List<PlaceDto>> response = placesService.searchPlaces(city, query, type);
        return ResponseEntity.ok(response);
    }

    /**
     * Get details for a specific place by its provider place ID.
     *
     * @param placeId provider-assigned place ID (e.g. Google Place ID or "otm_" prefixed OTM ID)
     *
     * Example: GET /api/external/places/details/ChIJN1t_tDeuEmsRUsoyG83frY4
     */
    @GetMapping("/details/{placeId}")
    public ResponseEntity<ProviderResponse<PlaceDto>> getPlaceDetails(
            @PathVariable String placeId) {

        ProviderResponse<PlaceDto> response = placesService.getPlaceDetails(placeId);
        return ResponseEntity.ok(response);
    }
}
