package com.tripforge.external.provider;

import com.tripforge.external.config.ProviderProperties;
import com.tripforge.external.dto.LocationSuggestionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Geoapify Geocoding / Autocomplete provider.
 *
 * Free tier: 3,000 requests/day, no credit card required.
 * https://www.geoapify.com/
 *
 * Used for:
 *   1. Destination autocomplete (global, English-preferred, strong relevance)
 *   2. Geocoding destination text to lat/lng
 *
 * Advantages over Nominatim:
 *   - Better relevance ranking (no "Bad Liebenwerda" for "Bali")
 *   - Cleaner English labels
 *   - Supports city/state/country/district types
 *   - Returns importance score for ranking
 *
 * API docs: https://apidocs.geoapify.com/docs/geocoding/
 */
@Component
public class GeoapifyLocationProvider {

    private static final Logger log = LoggerFactory.getLogger(GeoapifyLocationProvider.class);
    private static final String PROVIDER_NAME = "geoapify";
    private static final String AUTOCOMPLETE_URL = "https://api.geoapify.com/v1/geocode/autocomplete";
    private static final String GEOCODE_URL = "https://api.geoapify.com/v1/geocode/search";

    /** In-memory geocoding cache */
    private final Map<String, double[]> geocodeCache = new ConcurrentHashMap<>();

    private final ProviderProperties props;
    private final RestTemplate restTemplate;

    public GeoapifyLocationProvider(ProviderProperties props,
                                     @Qualifier("providerRestTemplate") RestTemplate restTemplate) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    /**
     * Search for destination suggestions using Geoapify autocomplete.
     * Returns up to {@code limit} results, English-preferred, city-first.
     */
    public List<LocationSuggestionDto> searchLocations(String query, int limit) {
        if (!props.isGeoapifyConfigured()) {
            log.debug("Geoapify not configured — skipping location search");
            return List.of();
        }
        if (query == null || query.isBlank()) return List.of();

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(AUTOCOMPLETE_URL)
                    .queryParam("text", query.trim())
                    .queryParam("apiKey", props.getGeoapify().getApiKey())
                    .queryParam("lang", "en")
                    .queryParam("limit", Math.min(limit * 2, 10))
                    .queryParam("type", "city,county,state,country,district")
                    .build()
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return List.of();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> features =
                    (List<Map<String, Object>>) response.get("features");
            if (features == null || features.isEmpty()) return List.of();

            List<LocationSuggestionDto> suggestions = new ArrayList<>();
            String queryLower = query.trim().toLowerCase();

            for (Map<String, Object> feature : features) {
                LocationSuggestionDto dto = normalizeFeature(feature, queryLower);
                if (dto != null && dto.getPrimaryText() != null) {
                    suggestions.add(dto);
                    if (suggestions.size() >= limit) break;
                }
            }

            log.debug("Geoapify location search '{}' → {} results", query, suggestions.size());
            return suggestions;

        } catch (Exception e) {
            log.warn("Geoapify location search failed for '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    /**
     * Geocode a city name to [lat, lng] coordinates.
     * Returns null on failure.
     */
    public double[] geocodeCity(String city) {
        if (!props.isGeoapifyConfigured()) return null;
        if (city == null || city.isBlank()) return null;

        String key = city.toLowerCase().trim();
        if (geocodeCache.containsKey(key)) return geocodeCache.get(key);

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(GEOCODE_URL)
                    .queryParam("text", city.trim())
                    .queryParam("apiKey", props.getGeoapify().getApiKey())
                    .queryParam("lang", "en")
                    .queryParam("limit", 1)
                    .queryParam("type", "city,county,state")
                    .build()
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return null;

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> features =
                    (List<Map<String, Object>>) response.get("features");
            if (features == null || features.isEmpty()) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> geometry =
                    (Map<String, Object>) features.get(0).get("geometry");
            if (geometry == null) return null;

            @SuppressWarnings("unchecked")
            List<Number> coords = (List<Number>) geometry.get("coordinates");
            if (coords == null || coords.size() < 2) return null;

            // GeoJSON: [lng, lat]
            double lng = coords.get(0).doubleValue();
            double lat = coords.get(1).doubleValue();
            double[] result = {lat, lng};

            geocodeCache.put(key, result);
            log.info("Geoapify geocoded '{}' → [{}, {}]", city, lat, lng);
            return result;

        } catch (Exception e) {
            log.warn("Geoapify geocoding failed for '{}': {}", city, e.getMessage());
            return null;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private LocationSuggestionDto normalizeFeature(Map<String, Object> feature, String queryLower) {
        try {
            Map<String, Object> props = (Map<String, Object>) feature.get("properties");
            Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
            if (props == null || geometry == null) return null;

            // Coordinates — GeoJSON [lng, lat]
            List<Number> coords = (List<Number>) geometry.get("coordinates");
            if (coords == null || coords.size() < 2) return null;
            double lng = coords.get(0).doubleValue();
            double lat = coords.get(1).doubleValue();

            // Name fields
            String city    = (String) props.get("city");
            String county  = (String) props.get("county");
            String state   = (String) props.get("state");
            String country = (String) props.get("country");
            String countryCode = (String) props.get("country_code");
            String name    = (String) props.get("name");
            String type    = (String) props.getOrDefault("result_type", "city");

            // Primary text: most specific name
            String primaryText = city != null ? city
                    : county != null ? county
                    : state != null ? state
                    : name != null ? name
                    : country;

            if (primaryText == null) return null;

            // Relevance check: primary name should start with or contain the query
            if (!primaryText.toLowerCase().startsWith(queryLower)
                    && !primaryText.toLowerCase().contains(queryLower)) {
                return null;
            }

            // Secondary text: context
            String secondaryText = buildSecondaryText(city, county, state, country);

            // Display name
            String displayName = secondaryText != null
                    ? primaryText + ", " + secondaryText
                    : primaryText;

            // Stable ID
            String osmId = props.get("osm_id") != null ? props.get("osm_id").toString() : null;
            String id = osmId != null ? "geoapify:" + osmId
                    : "geoapify:" + Math.round(lat * 10000) + "," + Math.round(lng * 10000);

            // Normalize type
            String normalizedType = normalizeType(type);

            return LocationSuggestionDto.builder()
                    .id(id)
                    .displayName(displayName)
                    .primaryText(primaryText)
                    .secondaryText(secondaryText)
                    .city(city != null ? city : (county != null ? county : state))
                    .state(state)
                    .country(country)
                    .countryCode(countryCode != null ? countryCode.toUpperCase() : null)
                    .type(normalizedType)
                    .lat(lat)
                    .lng(lng)
                    .sourceProvider(PROVIDER_NAME)
                    .build();

        } catch (Exception e) {
            log.debug("Failed to normalize Geoapify feature: {}", e.getMessage());
            return null;
        }
    }

    private String buildSecondaryText(String city, String county, String state, String country) {
        if (city != null && state != null && country != null) return state + ", " + country;
        if (city != null && country != null) return country;
        if (county != null && country != null) return country;
        if (state != null && country != null) return country;
        return country;
    }

    private String normalizeType(String geoapifyType) {
        if (geoapifyType == null) return "city";
        return switch (geoapifyType.toLowerCase()) {
            case "city", "town", "village", "hamlet" -> "city";
            case "county", "district", "suburb"      -> "district";
            case "state", "province", "region"       -> "state";
            case "country"                           -> "country";
            default                                  -> "city";
        };
    }

    public String getProviderName() { return PROVIDER_NAME; }
}
