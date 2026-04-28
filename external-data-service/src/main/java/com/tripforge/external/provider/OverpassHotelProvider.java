package com.tripforge.external.provider;

import com.tripforge.external.dto.HotelCandidateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Overpass API (OpenStreetMap) hotel provider.
 *
 * Completely FREE — no API key required.
 * Queries OSM for tourism=hotel|hostel|motel|guest_house nodes near coordinates.
 * Globally comprehensive — works for any city in the world.
 *
 * API: https://overpass-api.de/api/interpreter
 * Overpass QL docs: https://wiki.openstreetmap.org/wiki/Overpass_API/Overpass_QL
 *
 * Usage policy:
 *   - Do not send more than 1 request per second
 *   - Cache results (handled by Redis in HotelSearchService)
 *   - Use a descriptive User-Agent
 */
@Component
public class OverpassHotelProvider {

    private static final Logger log = LoggerFactory.getLogger(OverpassHotelProvider.class);
    private static final String PROVIDER_NAME = "overpass_osm";
    private static final String OVERPASS_URL = "https://overpass-api.de/api/interpreter";
    private static final String USER_AGENT = "TripForge/1.0 (portfolio project; contact@tripforge.example)";

    // Default search radius in meters
    private static final int DEFAULT_RADIUS_M = 10000;  // 10 km
    private static final int MAX_RESULTS = 8;

    private final RestTemplate restTemplate;

    public OverpassHotelProvider(@Qualifier("providerRestTemplate") RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    /**
     * Search for hotels near the given coordinates using Overpass API (OSM).
     *
     * @param lat      center latitude
     * @param lng      center longitude
     * @param city     city name for display
     * @param currency currency code for display
     * @param budget   optional budget per night hint for price estimation
     * @param preference hotel preference tier (BUDGET/STANDARD/LUXURY)
     * @return list of normalized HotelCandidateDto, may be empty on failure
     */
    public List<HotelCandidateDto> searchHotels(double lat, double lng, String city,
                                                  String currency, Double budget,
                                                  String preference) {
        try {
            // Overpass QL query: find hotels/hostels/motels/guest_houses within radius
            // Returns nodes and ways with tourism=hotel|hostel|motel|guest_house
            String query = buildOverpassQuery(lat, lng, DEFAULT_RADIUS_M);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("User-Agent", USER_AGENT);

            String body = "data=" + java.net.URLEncoder.encode(query, java.nio.charset.StandardCharsets.UTF_8);
            HttpEntity<String> entity = new HttpEntity<>(body, headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<Map<String, Object>> response = restTemplate.exchange(
                    OVERPASS_URL, HttpMethod.POST, entity,
                    (Class<Map<String, Object>>) (Class<?>) Map.class);

            if (response.getBody() == null) {
                log.debug("Overpass returned null body for lat={} lng={}", lat, lng);
                return List.of();
            }

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> elements =
                    (List<Map<String, Object>>) response.getBody().get("elements");

            if (elements == null || elements.isEmpty()) {
                log.debug("Overpass: no hotels found near lat={} lng={}", lat, lng);
                return List.of();
            }

            List<HotelCandidateDto> hotels = new ArrayList<>();
            for (Map<String, Object> element : elements) {
                HotelCandidateDto hotel = normalizeElement(element, lat, lng, city,
                        currency, budget, preference);
                if (hotel != null) hotels.add(hotel);
                if (hotels.size() >= MAX_RESULTS) break;
            }

            log.info("Overpass OSM returned {} hotels near lat={} lng={} ({})",
                    hotels.size(), lat, lng, city);
            return hotels;

        } catch (Exception e) {
            log.warn("Overpass hotel search failed for lat={} lng={}: {}", lat, lng, e.getMessage());
            return List.of();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Build an Overpass QL query for hotels within radius of a point.
     * Searches for nodes and ways tagged as tourism accommodation.
     */
    private String buildOverpassQuery(double lat, double lng, int radiusM) {
        return String.format(
            "[out:json][timeout:15];" +
            "(" +
            "  node[\"tourism\"~\"hotel|hostel|motel|guest_house|apartment\"](around:%d,%f,%f);" +
            "  way[\"tourism\"~\"hotel|hostel|motel|guest_house|apartment\"](around:%d,%f,%f);" +
            ");" +
            "out body %d;",
            radiusM, lat, lng,
            radiusM, lat, lng,
            MAX_RESULTS * 3   // fetch more than needed to allow filtering
        );
    }

    @SuppressWarnings("unchecked")
    private HotelCandidateDto normalizeElement(Map<String, Object> element,
                                                double centerLat, double centerLng,
                                                String city, String currency,
                                                Double budget, String preference) {
        try {
            Map<String, Object> tags = (Map<String, Object>) element.get("tags");
            if (tags == null) return null;

            String name = (String) tags.get("name");
            if (name == null || name.isBlank()) return null;

            // Get coordinates — nodes have lat/lon directly, ways have center
            Double lat = parseDouble(element.get("lat"));
            Double lng = parseDouble(element.get("lon"));
            if (lat == null || lng == null) {
                // Way element — try center
                Map<String, Object> center = (Map<String, Object>) element.get("center");
                if (center != null) {
                    lat = parseDouble(center.get("lat"));
                    lng = parseDouble(center.get("lon"));
                }
            }
            if (lat == null || lng == null) return null;

            // Distance from center
            double distKm = haversineKm(centerLat, centerLng, lat, lng);

            // Tourism type → category
            String tourismType = (String) tags.getOrDefault("tourism", "hotel");
            String category = inferCategory(tourismType, preference);

            // Stars → rating
            Double rating = inferRating(tags, category);

            // Price estimate
            double nightlyRate = estimateNightlyRate(budget, preference, currency);

            // Address
            String street  = (String) tags.get("addr:street");
            String houseNo = (String) tags.get("addr:housenumber");
            String address = buildAddress(name, street, houseNo, city);

            // Amenities from OSM tags
            List<String> amenities = extractAmenities(tags);

            // Popularity from stars/rating
            double popularity = Math.min(10.0, (rating != null ? rating : 3.5) * 1.8);

            String osmId = element.get("id") != null ? element.get("id").toString() : "0";
            String osmType = (String) element.getOrDefault("type", "node");

            return HotelCandidateDto.builder()
                    .externalHotelId("osm_" + osmType + "_" + osmId)
                    .name(name)
                    .address(address)
                    .areaName(city)
                    .lat(lat)
                    .lng(lng)
                    .distanceFromCenterKm(Math.round(distKm * 10.0) / 10.0)
                    .rating(rating)
                    .category(category)
                    .amenities(amenities)
                    .pricePerNight(nightlyRate)
                    .currencyCode(currency != null ? currency : "USD")
                    .sourceProvider(PROVIDER_NAME)
                    .fallbackUsed(false)
                    .popularityScore(Math.round(popularity * 10.0) / 10.0)
                    .build();

        } catch (Exception e) {
            log.debug("Failed to normalize Overpass element: {}", e.getMessage());
            return null;
        }
    }

    private String inferCategory(String tourismType, String preference) {
        if (preference != null) {
            return switch (preference.toUpperCase()) {
                case "LUXURY" -> "LUXURY";
                case "BUDGET" -> "BUDGET";
                default -> "STANDARD";
            };
        }
        return switch (tourismType.toLowerCase()) {
            case "hostel" -> "BUDGET";
            case "hotel"  -> "STANDARD";
            default       -> "STANDARD";
        };
    }

    @SuppressWarnings("unchecked")
    private Double inferRating(Map<String, Object> tags, String category) {
        // Try OSM stars tag first
        Object stars = tags.get("stars");
        if (stars != null) {
            try {
                double s = Double.parseDouble(stars.toString());
                // Convert 1–5 star scale to 0–5 rating
                return Math.min(5.0, Math.max(1.0, s));
            } catch (Exception ignored) {}
        }
        // Default by category
        return switch (category) {
            case "LUXURY" -> 4.5;
            case "BUDGET" -> 3.7;
            default       -> 4.1;
        };
    }

    /**
     * Estimate nightly rate based on budget, preference, and currency.
     * Uses 30% of total budget / duration as hotel budget slice.
     */
    private double estimateNightlyRate(Double budget, String preference, String currency) {
        // Base rates in USD — will be used as-is since we don't convert here
        // The budget-service handles FX conversion
        double base = switch (preference != null ? preference.toUpperCase() : "STANDARD") {
            case "LUXURY" -> 200.0;
            case "BUDGET" -> 40.0;
            default       -> 90.0;
        };

        if (budget != null && budget > 0) {
            // Use 30% of budget as hotel slice — budget is already in target currency
            double hotelSlice = budget * 0.30;
            // Clamp to reasonable range
            base = Math.max(base * 0.5, Math.min(base * 3.0, hotelSlice));
        }

        return Math.round(base * 100.0) / 100.0;
    }

    private String buildAddress(String name, String street, String houseNo, String city) {
        if (street != null && !street.isBlank()) {
            String addr = houseNo != null ? houseNo + " " + street : street;
            return addr + ", " + city;
        }
        return city;
    }

    @SuppressWarnings("unchecked")
    private List<String> extractAmenities(Map<String, Object> tags) {
        List<String> amenities = new ArrayList<>();
        if ("yes".equals(tags.get("internet_access")) || "wlan".equals(tags.get("internet_access"))) {
            amenities.add("wifi");
        }
        if ("yes".equals(tags.get("swimming_pool"))) amenities.add("pool");
        if ("yes".equals(tags.get("parking"))) amenities.add("parking");
        if ("yes".equals(tags.get("restaurant"))) amenities.add("restaurant");
        if ("yes".equals(tags.get("bar"))) amenities.add("bar");
        if ("yes".equals(tags.get("gym"))) amenities.add("gym");
        if ("yes".equals(tags.get("spa"))) amenities.add("spa");
        if ("yes".equals(tags.get("air_conditioning"))) amenities.add("air_conditioning");
        // Default wifi if no amenities found (most modern hotels have it)
        if (amenities.isEmpty()) amenities.add("wifi");
        return amenities;
    }

    private double haversineKm(double lat1, double lng1, double lat2, double lng2) {
        final double R = 6371.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLng = Math.toRadians(lng2 - lng1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLng / 2) * Math.sin(dLng / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private Double parseDouble(Object val) {
        if (val == null) return null;
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    public String getProviderName() { return PROVIDER_NAME; }
}
