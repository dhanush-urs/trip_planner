package com.tripforge.hotel.client;

import com.tripforge.hotel.dto.ExternalHotelSearchResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Feign client for external-data-service.
 * Used by hotel-service to fetch live hotel candidates from Google Places.
 *
 * Falls back gracefully — if external-data-service is down or returns empty,
 * hotel-service uses its local CSV dataset.
 */
@FeignClient(name = "external-data-service", path = "/api/external")
public interface ExternalDataServiceClient {

    /**
     * Search for live hotel candidates in a city.
     * Returns ProviderResponse<List<HotelCandidateDto>> from external-data-service.
     */
    @GetMapping("/hotels/search")
    ExternalHotelSearchResponse searchHotels(
            @RequestParam String city,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestParam(required = false) Double budget,
            @RequestParam(defaultValue = "INR") String currency
    );
}
