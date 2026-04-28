package com.tripforge.external.controller;

import com.tripforge.external.dto.HotelCandidateDto;
import com.tripforge.external.dto.ProviderResponse;
import com.tripforge.external.service.HotelSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Hotel search controller.
 *
 * GET /api/external/hotels/search — search hotel candidates for a city
 */
@RestController
@RequestMapping("/api/external/hotels")
public class HotelSearchController {

    @Autowired
    private HotelSearchService hotelSearchService;

    /**
     * Search for hotel candidates in a city.
     * Returns normalized HotelCandidateDto list for hotel-service to rank.
     *
     * @param city     city name (required)
     * @param lat      city center latitude (optional, improves results)
     * @param lng      city center longitude (optional, improves results)
     * @param budget   max budget per night in the given currency (optional)
     * @param currency currency code, e.g. "INR" (optional, default INR)
     *
     * Example: GET /api/external/hotels/search?city=Goa&lat=15.2993&lng=74.1240&currency=INR
     */
    @GetMapping("/search")
    public ResponseEntity<ProviderResponse<List<HotelCandidateDto>>> searchHotels(
            @RequestParam String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double budget,
            @RequestParam(defaultValue = "INR") String currency) {

        ProviderResponse<List<HotelCandidateDto>> response =
                hotelSearchService.searchHotels(city, lat, lng, budget, currency);
        return ResponseEntity.ok(response);
    }
}
