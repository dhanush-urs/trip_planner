package com.tripforge.external.service;

import com.tripforge.external.dto.PlaceDto;
import com.tripforge.external.dto.ProviderResponse;
import com.tripforge.external.provider.GooglePlacesProvider;
import com.tripforge.external.provider.OpenTripMapProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Places service — orchestrates provider fallback chain for place/POI search.
 *
 * FREE-FIRST provider order (Phase 9G):
 *   1. OpenTripMap API    (PRIMARY — free tier, no billing required)
 *   2. Google Places API  (OPTIONAL — only if key is configured)
 *   3. Empty list         (degraded mode — callers use local CSV dataset)
 *
 * Results are cached in Redis to reduce API calls.
 */
@Service
public class PlacesService {

    private static final Logger log = LoggerFactory.getLogger(PlacesService.class);

    @Autowired private OpenTripMapProvider openTripMap;
    @Autowired private GooglePlacesProvider googlePlaces;

    @Cacheable(value = "places-search",
               key = "#city + '_' + #query + '_' + #type",
               unless = "#result.data.isEmpty()")
    public ProviderResponse<List<PlaceDto>> searchPlaces(String city, String query, String type) {
        log.info("Searching places: city='{}' query='{}' type='{}'", city, query, type);

        // 1. Try OpenTripMap first (free, no billing)
        List<PlaceDto> results = openTripMap.searchAttractions(city, query);
        if (!results.isEmpty()) {
            return ProviderResponse.of(results, "opentripmap");
        }

        // 2. Try Google Places if configured (optional)
        results = googlePlaces.searchPlaces(city, query, type);
        if (!results.isEmpty()) {
            return ProviderResponse.fallback(results, "google_places",
                    "OpenTripMap returned no results — using optional Google Places");
        }

        // 3. Degraded — callers use CSV dataset
        log.warn("All place providers returned empty for city='{}' — using CSV fallback", city);
        return ProviderResponse.degraded(List.of(), "none",
                "No live place provider available — using local CSV dataset fallback");
    }

    @Cacheable(value = "place-details", key = "#placeId", unless = "#result.data == null")
    public ProviderResponse<PlaceDto> getPlaceDetails(String placeId) {
        log.info("Fetching place details for placeId='{}'", placeId);

        if (placeId.startsWith("otm_")) {
            return ProviderResponse.degraded(null, "opentripmap",
                    "OpenTripMap detailed lookup not available in free tier");
        }

        // Try Google Places if configured
        PlaceDto place = googlePlaces.getPlaceDetails(placeId);
        if (place != null) {
            return ProviderResponse.of(place, "google_places");
        }

        return ProviderResponse.degraded(null, "none",
                "Place details unavailable from all providers");
    }
}
