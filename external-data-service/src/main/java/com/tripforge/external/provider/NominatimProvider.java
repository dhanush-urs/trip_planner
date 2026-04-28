package com.tripforge.external.provider;

import com.tripforge.external.dto.LocationSuggestionDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Nominatim (OpenStreetMap) geocoding provider.
 *
 * Free, no API key required.
 * Used to:
 *   1. Resolve city names to [lat, lng] coordinates for OpenTripMap and OpenRouteService.
 *   2. Power the frontend destination autocomplete (searchLocations).
 *
 * Usage policy: https://operations.osmfoundation.org/policies/nominatim/
 *   - Must include a descriptive User-Agent
 *   - Must not send bulk requests
 *   - Results are cached to minimize calls
 *
 * API docs: https://nominatim.org/release-docs/develop/api/Search/
 */
@Component
public class NominatimProvider {

    private static final Logger log = LoggerFactory.getLogger(NominatimProvider.class);
    private static final String PROVIDER_NAME = "nominatim";
    private static final String BASE_URL = "https://nominatim.openstreetmap.org";
    private static final String USER_AGENT = "TripForge/1.0 (portfolio project; contact@tripforge.example)";

    /** In-memory geocoding cache — avoids repeated calls for the same city */
    private final Map<String, double[]> geocodeCache = new ConcurrentHashMap<>();

    private final RestTemplate restTemplate;

    public NominatimProvider(@Qualifier("providerRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    // ── Geocoding (existing) ──────────────────────────────────────────────────

    /**
     * Resolve a city name to [lat, lng] coordinates.
     * Returns null if geocoding fails.
     * Results are cached in-memory.
     */
    public double[] geocodeCity(String city) {
        if (city == null || city.isBlank()) return null;

        String key = city.toLowerCase().trim();
        if (geocodeCache.containsKey(key)) {
            log.debug("Nominatim cache hit for '{}'", city);
            return geocodeCache.get(key);
        }

        try {
            // Fetch top 5 results and pick the one with highest importance score.
            // This prevents "London, Ontario" from beating "London, England" —
            // the UK capital has a much higher importance score in OSM.
            String url = UriComponentsBuilder
                    .fromHttpUrl(BASE_URL + "/search")
                    .queryParam("q", city)
                    .queryParam("format", "jsonv2")   // jsonv2 includes importance score
                    .queryParam("limit", 5)
                    .queryParam("addressdetails", 0)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept-Language", "en");
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<List<Map<String, Object>>> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity,
                            (Class<List<Map<String, Object>>>) (Class<?>) List.class);

            List<Map<String, Object>> results = response.getBody();
            if (results == null || results.isEmpty()) {
                log.warn("Nominatim: no results for city '{}'", city);
                return null;
            }

            // Pick the result with the highest importance score (most globally significant)
            Map<String, Object> best = results.stream()
                    .max(java.util.Comparator.comparingDouble(r -> {
                        Object imp = r.get("importance");
                        if (imp == null) return 0.0;
                        try { return Double.parseDouble(imp.toString()); } catch (Exception e) { return 0.0; }
                    }))
                    .orElse(results.get(0));

            double lat = Double.parseDouble(best.get("lat").toString());
            double lon = Double.parseDouble(best.get("lon").toString());
            double[] coords = {lat, lon};

            geocodeCache.put(key, coords);
            log.info("Nominatim geocoded '{}' → [{}, {}] (importance-ranked)", city, lat, lon);
            return coords;

        } catch (Exception e) {
            log.warn("Nominatim geocoding failed for '{}': {}", city, e.getMessage());
            return null;
        }
    }

    // ── Location search (new — powers frontend autocomplete) ─────────────────

    /**
     * Search for destination suggestions matching a query string.
     * Returns up to {@code limit} results with display name, coordinates, and address parts.
     * Returns an empty list (never throws) on any failure.
     *
     * @param query free-text query, e.g. "Goa", "Tokyo", "Paris"
     * @param limit max results to return (1–10)
     * @return list of LocationSuggestionDto, may be empty
     */
    public List<LocationSuggestionDto> searchLocations(String query, int limit) {
        if (query == null || query.isBlank()) return List.of();

        int safeLimit = Math.min(Math.max(limit, 1), 10);

        try {
            // Request more results than needed so we can filter low-quality ones
            int fetchLimit = Math.min(safeLimit * 2, 10);

            String url = UriComponentsBuilder
                    .fromHttpUrl(BASE_URL + "/search")
                    .queryParam("q", query.trim())
                    .queryParam("format", "jsonv2")       // jsonv2 includes place_rank + importance
                    .queryParam("limit", fetchLimit)
                    .queryParam("addressdetails", 1)      // include address breakdown for city/country
                    .queryParam("dedupe", 1)              // remove near-duplicate results
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", USER_AGENT);
            headers.set("Accept-Language", "en");         // force English display names
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<List<Map<String, Object>>> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity,
                            (Class<List<Map<String, Object>>>) (Class<?>) List.class);

            List<Map<String, Object>> results = response.getBody();
            if (results == null || results.isEmpty()) {
                log.debug("Nominatim location search: no results for '{}'", query);
                return List.of();
            }

            List<LocationSuggestionDto> suggestions = new ArrayList<>();
            String queryLower = query.trim().toLowerCase();

            for (Map<String, Object> result : results) {
                // Filter: only include city-like place types
                Object placeRank = result.get("place_rank");
                String osmType   = (String) result.getOrDefault("osm_type", "");
                String category  = (String) result.getOrDefault("category", "");
                String type      = (String) result.getOrDefault("type", "");

                boolean isCityLike = isCityLikeResult(placeRank, category, type, osmType);
                if (!isCityLike) continue;

                LocationSuggestionDto dto = normalizeLocationResult(result);
                if (dto == null) continue;

                // Relevance check: prefer results where the primary name starts with the query.
                // This prevents "Bad Liebenwerda" from appearing before "Bali" for query "bali".
                String primary = dto.getPrimaryText() != null ? dto.getPrimaryText().toLowerCase() : "";
                String city    = dto.getCity() != null ? dto.getCity().toLowerCase() : "";
                boolean nameMatch = primary.startsWith(queryLower) || city.startsWith(queryLower)
                        || (dto.getDisplayName() != null && dto.getDisplayName().toLowerCase().startsWith(queryLower));

                if (nameMatch) {
                    suggestions.add(dto);
                    if (suggestions.size() >= safeLimit) break;
                }
            }

            // If name-match filtering removed everything, fall back to all city-like results
            if (suggestions.isEmpty()) {
                for (Map<String, Object> result : results) {
                    Object placeRank = result.get("place_rank");
                    String category  = (String) result.getOrDefault("category", "");
                    String type      = (String) result.getOrDefault("type", "");
                    String osmType   = (String) result.getOrDefault("osm_type", "");
                    if (!isCityLikeResult(placeRank, category, type, osmType)) continue;
                    LocationSuggestionDto dto = normalizeLocationResult(result);
                    if (dto != null) {
                        suggestions.add(dto);
                        if (suggestions.size() >= safeLimit) break;
                    }
                }
            }

            // Last resort: any result at all
            if (suggestions.isEmpty()) {
                for (Map<String, Object> result : results) {
                    LocationSuggestionDto dto = normalizeLocationResult(result);
                    if (dto != null) {
                        suggestions.add(dto);
                        if (suggestions.size() >= safeLimit) break;
                    }
                }
            }

            log.debug("Nominatim location search '{}' → {} results", query, suggestions.size());
            return suggestions;

        } catch (Exception e) {
            log.warn("Nominatim location search failed for '{}': {}", query, e.getMessage());
            return List.of();
        }
    }

    /**
     * Returns true if the Nominatim result represents a city, town, village,
     * or major administrative area worth showing as a destination.
     *
     * Nominatim place_rank guide:
     *   4  = country
     *   8  = state/province
     *   12 = city (major)
     *   14 = town
     *   16 = village
     *   18 = suburb / neighbourhood
     *   20+ = street / POI
     *
     * We accept rank 8–16 to cover states/provinces (useful for "California",
     * "Kerala", "Tuscany") as well as cities and towns.
     * We exclude rank 4 (countries) and rank 18+ (suburbs/streets).
     */
    private boolean isCityLikeResult(Object placeRank, String category, String type, String osmType) {
        if (placeRank instanceof Number) {
            int rank = ((Number) placeRank).intValue();
            // Accept states/provinces (8), cities (12), towns (14), villages (16)
            // Exclude countries (4) and suburbs/streets (18+)
            if (rank >= 8 && rank <= 16) return true;
        }
        // Also accept place category with city/town/village/municipality type
        if ("place".equals(category)) {
            return "city".equals(type) || "town".equals(type) || "village".equals(type)
                    || "municipality".equals(type) || "hamlet".equals(type)
                    || "island".equals(type) || "region".equals(type);
        }
        // Accept boundary/administrative for major cities (rank 8–12)
        if ("boundary".equals(category) && "administrative".equals(type)) {
            if (placeRank instanceof Number) {
                int rank = ((Number) placeRank).intValue();
                return rank >= 8 && rank <= 12;
            }
        }
        return false;
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private LocationSuggestionDto normalizeLocationResult(Map<String, Object> result) {
        try {
            String displayName = (String) result.get("display_name");
            Double lat = parseDouble(result.get("lat"));
            Double lon = parseDouble(result.get("lon"));

            if (displayName == null || lat == null || lon == null) return null;

            // Extract address parts
            Map<String, Object> address = (Map<String, Object>) result.get("address");
            String city        = extractCity(address);
            String state       = extractString(address, "state", "province", "region", "county");
            String country     = extractString(address, "country");
            String countryCode = extractString(address, "country_code");

            // Determine place type from Nominatim category/type
            String category = (String) result.getOrDefault("category", "");
            String type     = (String) result.getOrDefault("type", "");
            String placeType = deriveType(category, type, city, state);

            // primaryText = most specific name available
            String primaryText = city != null ? city
                    : state != null ? state
                    : country;

            // secondaryText = context after primaryText
            String secondaryText = buildSecondaryText(city, state, country);

            // Build a clean short display name
            String shortDisplay = buildShortDisplay(city, state, country, displayName);

            // Stable ID: use osm_type + osm_id if available, else lat/lng
            String osmType = (String) result.getOrDefault("osm_type", "");
            Object osmId   = result.get("osm_id");
            String id = (osmType != null && !osmType.isBlank() && osmId != null)
                    ? "nominatim:" + osmType + ":" + osmId
                    : "nominatim:" + Math.round(lat * 10000) + "," + Math.round(lon * 10000);

            return LocationSuggestionDto.builder()
                    .id(id)
                    .displayName(shortDisplay)
                    .primaryText(primaryText)
                    .secondaryText(secondaryText)
                    .city(city)
                    .state(state)
                    .country(country)
                    .countryCode(countryCode != null ? countryCode.toUpperCase() : null)
                    .type(placeType)
                    .lat(lat)
                    .lng(lon)
                    .sourceProvider(PROVIDER_NAME)
                    .build();

        } catch (Exception e) {
            log.debug("Failed to normalize Nominatim result: {}", e.getMessage());
            return null;
        }
    }

    /** Derive a human-friendly place type from Nominatim category/type fields. */
    private String deriveType(String category, String type, String city, String state) {
        if ("place".equals(category)) {
            return switch (type) {
                case "city"         -> "city";
                case "town"         -> "town";
                case "village"      -> "village";
                case "hamlet"       -> "village";
                case "island"       -> "island";
                case "region"       -> "region";
                case "municipality" -> "city";
                default             -> city != null ? "city" : "region";
            };
        }
        if ("boundary".equals(category) && "administrative".equals(type)) {
            return state != null && city == null ? "state" : "city";
        }
        return city != null ? "city" : state != null ? "state" : "region";
    }

    /** Build secondary text: context after the primary name. */
    private String buildSecondaryText(String city, String state, String country) {
        if (city != null) {
            // city is primary → secondary = state + country
            if (state != null && country != null) return state + ", " + country;
            if (country != null) return country;
            return state;
        }
        if (state != null && country != null) return country;
        return null;
    }

    /** Extract the most specific city-level name from the address object. */
    private String extractCity(Map<String, Object> address) {
        if (address == null) return null;
        // Ordered from most specific to least specific
        for (String key : new String[]{
                "city", "town", "village", "hamlet",
                "municipality", "city_district", "district",
                "suburb", "quarter", "neighbourhood"}) {
            Object val = address.get(key);
            if (val != null && !val.toString().isBlank()) return val.toString();
        }
        return null;
    }

    /** Extract the first non-null, non-blank value from a list of address keys. */
    private String extractString(Map<String, Object> address, String... keys) {
        if (address == null) return null;
        for (String key : keys) {
            Object val = address.get(key);
            if (val != null && !val.toString().isBlank()) return val.toString();
        }
        return null;
    }

    /** Build a clean short display name for the suggestion dropdown. */
    private String buildShortDisplay(String city, String state, String country, String fallback) {
        if (city != null && country != null) {
            return state != null ? city + ", " + state + ", " + country
                                 : city + ", " + country;
        }
        if (country != null) return country;
        // Trim the full Nominatim display_name to first 2 comma-parts
        if (fallback != null) {
            String[] parts = fallback.split(",", 3);
            return parts.length >= 2
                    ? parts[0].trim() + ", " + parts[1].trim()
                    : fallback;
        }
        return fallback;
    }

    private Double parseDouble(Object val) {
        if (val == null) return null;
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    public String getProviderName() { return PROVIDER_NAME; }
}
