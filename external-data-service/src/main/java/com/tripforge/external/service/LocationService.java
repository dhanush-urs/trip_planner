package com.tripforge.external.service;

import com.tripforge.external.dto.LocationSuggestionDto;
import com.tripforge.external.dto.ProviderResponse;
import com.tripforge.external.provider.GeoapifyLocationProvider;
import com.tripforge.external.provider.NominatimProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Location search service — powers the frontend destination autocomplete.
 *
 * FINAL provider order (Phase 10E — Real Data Upgrade):
 *   1. Geoapify (PRIMARY — free tier, better relevance, English-preferred)
 *   2. Nominatim (OpenStreetMap) — free, no key, global fallback
 *   3. Empty list — frontend shows curated global fallback list
 *
 * Geoapify is preferred over Nominatim because:
 *   - Better relevance ranking (no "Bad Liebenwerda" for "Bali")
 *   - Cleaner English labels
 *   - Supports type filtering (city/state/country)
 *   - Returns importance score for ranking
 *
 * Results are cached in Redis (TTL configured in application.yml).
 */
@Service
public class LocationService {

    private static final Logger log = LoggerFactory.getLogger(LocationService.class);
    private static final int DEFAULT_LIMIT = 6;

    private final GeoapifyLocationProvider geoapify;
    private final NominatimProvider nominatim;

    public LocationService(GeoapifyLocationProvider geoapify, NominatimProvider nominatim) {
        this.geoapify = geoapify;
        this.nominatim = nominatim;
    }

    /**
     * Search for destination suggestions matching the query.
     *
     * @param query free-text destination query (e.g. "Goa", "Tokyo")
     * @return ProviderResponse wrapping a list of LocationSuggestionDto
     *         — never null, never throws; returns empty list on failure
     */
    @Cacheable(value = "location-search", key = "#query.toLowerCase().trim()",
               unless = "#result.data.isEmpty()")
    public ProviderResponse<List<LocationSuggestionDto>> searchLocations(String query) {
        if (query == null || query.isBlank()) {
            return ProviderResponse.of(List.of(), "none");
        }

        log.debug("Location search: query='{}'", query);

        // 1. Geoapify (primary — better relevance, English-preferred)
        List<LocationSuggestionDto> geoapifyResults =
                geoapify.searchLocations(query, DEFAULT_LIMIT);
        if (!geoapifyResults.isEmpty()) {
            log.debug("Geoapify location search '{}' → {} results", query, geoapifyResults.size());
            return ProviderResponse.of(geoapifyResults, "geoapify");
        }

        // 2. Nominatim (fallback — free, no key)
        List<LocationSuggestionDto> nominatimResults =
                nominatim.searchLocations(query, DEFAULT_LIMIT);
        if (!nominatimResults.isEmpty()) {
            log.debug("Nominatim location search '{}' → {} results", query, nominatimResults.size());
            return ProviderResponse.fallback(nominatimResults, "nominatim",
                    "Geoapify returned empty — using Nominatim fallback");
        }

        // 3. Degraded — frontend shows curated global fallback list
        log.debug("Location search: no results for '{}' — frontend will show curated fallback", query);
        return ProviderResponse.degraded(List.of(), "none",
                "No location suggestions available — frontend will show curated global list");
    }
}
