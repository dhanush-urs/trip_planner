package com.tripforge.external.provider;

import com.tripforge.external.config.ProviderProperties;
import com.tripforge.external.dto.HotelCandidateDto;
import com.tripforge.external.dto.PlaceDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Google Places API provider.
 *
 * Used for:
 *   - Place / attraction search
 *   - Place details (coordinates, hours, rating)
 *   - Hotel search (via type=lodging)
 *
 * Falls back gracefully if API key is missing or quota exceeded.
 * All responses are normalized into internal DTOs.
 */
@Component
public class GooglePlacesProvider {

    private static final Logger log = LoggerFactory.getLogger(GooglePlacesProvider.class);
    private static final String PROVIDER_NAME = "google_places";

    private final ProviderProperties props;
    private final RestTemplate restTemplate;

    public GooglePlacesProvider(ProviderProperties props,
                                @Qualifier("providerRestTemplate") RestTemplate restTemplate) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    /**
     * Search for places (attractions, POIs) in a city.
     *
     * @param city    city name
     * @param query   search query (e.g. "beaches", "temples")
     * @param type    Google place type (e.g. "tourist_attraction", "restaurant")
     * @return list of normalized PlaceDto, empty if provider unavailable
     */
    public List<PlaceDto> searchPlaces(String city, String query, String type) {
        if (!props.isGooglePlacesConfigured()) {
            log.debug("Google Places not configured — skipping");
            return List.of();
        }

        try {
            String searchQuery = query != null ? query + " " + city : city;
            String url = UriComponentsBuilder
                    .fromHttpUrl(props.getGoogle().getPlaces().getBaseUrl() + "/place/textsearch/json")
                    .queryParam("query", searchQuery)
                    .queryParam("type", type != null ? type : "tourist_attraction")
                    .queryParam("key", props.getGoogle().getPlaces().getApiKey())
                    .build()
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return List.of();

            String status = (String) response.get("status");
            if (!"OK".equals(status) && !"ZERO_RESULTS".equals(status)) {
                log.warn("Google Places search returned status: {}", status);
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results == null) return List.of();

            List<PlaceDto> places = new ArrayList<>();
            for (Map<String, Object> result : results) {
                PlaceDto place = normalizePlaceResult(result);
                if (place != null) places.add(place);
            }

            log.info("Google Places returned {} results for query='{}' city='{}'",
                    places.size(), query, city);
            return places;

        } catch (Exception e) {
            log.warn("Google Places search failed: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * Fetch details for a specific place by its Google Place ID.
     */
    public PlaceDto getPlaceDetails(String placeId) {
        if (!props.isGooglePlacesConfigured()) return null;

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(props.getGoogle().getPlaces().getBaseUrl() + "/place/details/json")
                    .queryParam("place_id", placeId)
                    .queryParam("fields", "place_id,name,formatted_address,geometry,rating,user_ratings_total,types,opening_hours,photos")
                    .queryParam("key", props.getGoogle().getPlaces().getApiKey())
                    .build()
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return null;

            @SuppressWarnings("unchecked")
            Map<String, Object> result = (Map<String, Object>) response.get("result");
            return result != null ? normalizePlaceResult(result) : null;

        } catch (Exception e) {
            log.warn("Google Places details failed for {}: {}", placeId, e.getMessage());
            return null;
        }
    }

    /**
     * Search for hotels in a city using Google Places type=lodging.
     */
    public List<HotelCandidateDto> searchHotels(String city, Double lat, Double lng,
                                                  Double budgetPerNight, String currency) {
        if (!props.isGooglePlacesConfigured()) {
            log.debug("Google Places not configured — skipping hotel search");
            return List.of();
        }

        try {
            UriComponentsBuilder builder;
            if (lat != null && lng != null) {
                // Nearby search when coordinates are available
                builder = UriComponentsBuilder
                        .fromHttpUrl(props.getGoogle().getPlaces().getBaseUrl() + "/place/nearbysearch/json")
                        .queryParam("location", lat + "," + lng)
                        .queryParam("radius", 10000)
                        .queryParam("type", "lodging")
                        .queryParam("key", props.getGoogle().getPlaces().getApiKey());
            } else {
                // Text search fallback
                builder = UriComponentsBuilder
                        .fromHttpUrl(props.getGoogle().getPlaces().getBaseUrl() + "/place/textsearch/json")
                        .queryParam("query", "hotels in " + city)
                        .queryParam("type", "lodging")
                        .queryParam("key", props.getGoogle().getPlaces().getApiKey());
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(
                    builder.build().toUriString(), Map.class);
            if (response == null) return List.of();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = (List<Map<String, Object>>) response.get("results");
            if (results == null) return List.of();

            List<HotelCandidateDto> hotels = new ArrayList<>();
            for (Map<String, Object> result : results) {
                HotelCandidateDto hotel = normalizeHotelResult(result, city, lat, lng, currency);
                if (hotel != null) hotels.add(hotel);
            }

            log.info("Google Places returned {} hotel candidates for city='{}'", hotels.size(), city);
            return hotels;

        } catch (Exception e) {
            log.warn("Google Places hotel search failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ── Normalization helpers ─────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private PlaceDto normalizePlaceResult(Map<String, Object> result) {
        try {
            String placeId = (String) result.get("place_id");
            String name = (String) result.get("name");
            if (name == null) return null;

            Map<String, Object> geometry = (Map<String, Object>) result.get("geometry");
            Double lat = null, lng = null;
            if (geometry != null) {
                Map<String, Object> location = (Map<String, Object>) geometry.get("location");
                if (location != null) {
                    lat = toDouble(location.get("lat"));
                    lng = toDouble(location.get("lng"));
                }
            }

            Double rating = toDouble(result.get("rating"));
            Integer reviewCount = toInteger(result.get("user_ratings_total"));
            String address = (String) result.getOrDefault("formatted_address",
                    result.get("vicinity"));

            List<String> types = (List<String>) result.get("types");
            String category = types != null && !types.isEmpty() ? types.get(0) : "attraction";

            // Photo URL (first photo if available)
            String photoUrl = null;
            List<Map<String, Object>> photos = (List<Map<String, Object>>) result.get("photos");
            if (photos != null && !photos.isEmpty() && props.isGooglePlacesConfigured()) {
                String photoRef = (String) photos.get(0).get("photo_reference");
                if (photoRef != null) {
                    photoUrl = props.getGoogle().getPlaces().getBaseUrl()
                            + "/place/photo?maxwidth=400&photo_reference=" + photoRef
                            + "&key=" + props.getGoogle().getPlaces().getApiKey();
                }
            }

            return PlaceDto.builder()
                    .placeId(placeId)
                    .name(name)
                    .address(address)
                    .lat(lat)
                    .lng(lng)
                    .category(category)
                    .types(types)
                    .rating(rating)
                    .reviewCount(reviewCount)
                    .photoUrl(photoUrl)
                    .sourceProvider(PROVIDER_NAME)
                    .fallbackUsed(false)
                    .build();
        } catch (Exception e) {
            log.debug("Failed to normalize place result: {}", e.getMessage());
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private HotelCandidateDto normalizeHotelResult(Map<String, Object> result,
                                                     String city, Double centerLat,
                                                     Double centerLng, String currency) {
        try {
            String externalId = (String) result.get("place_id");
            String name = (String) result.get("name");
            if (name == null) return null;

            Map<String, Object> geometry = (Map<String, Object>) result.get("geometry");
            Double lat = null, lng = null;
            if (geometry != null) {
                Map<String, Object> location = (Map<String, Object>) geometry.get("location");
                if (location != null) {
                    lat = toDouble(location.get("lat"));
                    lng = toDouble(location.get("lng"));
                }
            }

            Double rating = toDouble(result.getOrDefault("rating", 3.5));
            Integer reviewCount = toInteger(result.get("user_ratings_total"));
            String address = (String) result.getOrDefault("formatted_address",
                    result.get("vicinity"));

            // Compute distance from center
            Double distanceKm = null;
            if (lat != null && lng != null && centerLat != null && centerLng != null) {
                distanceKm = haversineKm(centerLat, centerLng, lat, lng);
            }

            // Infer category from price_level (0-4 scale from Google)
            Integer priceLevel = toInteger(result.get("price_level"));
            String category = inferCategory(priceLevel);

            // Popularity from rating + review count
            double popularity = computePopularity(rating, reviewCount);

            // Photo
            String photoUrl = null;
            List<Map<String, Object>> photos = (List<Map<String, Object>>) result.get("photos");
            if (photos != null && !photos.isEmpty() && props.isGooglePlacesConfigured()) {
                String photoRef = (String) photos.get(0).get("photo_reference");
                if (photoRef != null) {
                    photoUrl = props.getGoogle().getPlaces().getBaseUrl()
                            + "/place/photo?maxwidth=400&photo_reference=" + photoRef
                            + "&key=" + props.getGoogle().getPlaces().getApiKey();
                }
            }

            return HotelCandidateDto.builder()
                    .externalHotelId(externalId)
                    .name(name)
                    .address(address)
                    .areaName(city)
                    .lat(lat)
                    .lng(lng)
                    .distanceFromCenterKm(distanceKm)
                    .rating(rating != null ? rating : 3.5)
                    .reviewCount(reviewCount)
                    .pricePerNight(null)   // Google Places doesn't provide prices
                    .currencyCode(currency)
                    .category(category)
                    .photoUrl(photoUrl)
                    .sourceProvider(PROVIDER_NAME)
                    .fallbackUsed(false)
                    .popularityScore(popularity)
                    .build();
        } catch (Exception e) {
            log.debug("Failed to normalize hotel result: {}", e.getMessage());
            return null;
        }
    }

    private String inferCategory(Integer priceLevel) {
        if (priceLevel == null) return "STANDARD";
        if (priceLevel <= 1) return "BUDGET";
        if (priceLevel >= 3) return "LUXURY";
        return "STANDARD";
    }

    private double computePopularity(Double rating, Integer reviewCount) {
        double r = rating != null ? rating : 3.0;
        double rc = reviewCount != null ? Math.min(reviewCount / 1000.0, 1.0) : 0.0;
        return Math.round(((r / 5.0) * 7.0 + rc * 3.0) * 10.0) / 10.0;
    }

    /** Haversine formula — distance between two lat/lng points in km */
    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Double d) return d;
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    private Integer toInteger(Object val) {
        if (val == null) return null;
        if (val instanceof Integer i) return i;
        if (val instanceof Number n) return n.intValue();
        try { return Integer.parseInt(val.toString()); } catch (Exception e) { return null; }
    }

    public String getProviderName() { return PROVIDER_NAME; }
}
