package com.tripforge.external.provider;

import com.tripforge.external.config.ProviderProperties;
import com.tripforge.external.dto.HotelCandidateDto;
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
 * Geoapify Places API — hotel/accommodation search.
 *
 * Free tier: 3,000 requests/day, no credit card required.
 * https://www.geoapify.com/places-api
 *
 * Searches for real hotel/accommodation places near coordinates.
 * Returns real place names, addresses, and coordinates.
 * Prices are always estimated (Geoapify does not provide booking prices).
 *
 * sourceType = BASIC_PLACE_DATA
 * priceType  = ESTIMATED_PRICE
 * providerName = GEOAPIFY
 */
@Component
public class GeoapifyHotelProvider {

    private static final Logger log = LoggerFactory.getLogger(GeoapifyHotelProvider.class);
    private static final String PROVIDER_NAME = "geoapify";
    private static final String PLACES_URL = "https://api.geoapify.com/v2/places";

    // Geoapify categories for accommodation
    private static final String HOTEL_CATEGORIES =
            "accommodation.hotel,accommodation.hostel,accommodation.motel," +
            "accommodation.guest_house,accommodation.resort,accommodation.apartment";

    private static final int DEFAULT_RADIUS_M = 10000;  // 10 km
    private static final int MAX_RESULTS = 8;

    private final ProviderProperties props;
    private final RestTemplate restTemplate;

    public GeoapifyHotelProvider(ProviderProperties props,
                                  @Qualifier("providerRestTemplate") RestTemplate restTemplate) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    /**
     * Search for hotels near the given coordinates using Geoapify Places API.
     *
     * @param lat        center latitude
     * @param lng        center longitude
     * @param city       city name for display
     * @param currency   currency code for display
     * @param budget     optional budget per night hint
     * @param preference hotel preference tier (BUDGET/STANDARD/LUXURY)
     * @return list of normalized HotelCandidateDto, may be empty on failure
     */
    public List<HotelCandidateDto> searchHotels(double lat, double lng, String city,
                                                  String currency, Double budget,
                                                  String preference) {
        if (!props.isGeoapifyConfigured()) {
            log.debug("Geoapify not configured — skipping hotel search");
            return List.of();
        }

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(PLACES_URL)
                    .queryParam("categories", HOTEL_CATEGORIES)
                    .queryParam("filter", "circle:" + lng + "," + lat + "," + DEFAULT_RADIUS_M)
                    .queryParam("bias", "proximity:" + lng + "," + lat)
                    .queryParam("limit", MAX_RESULTS)
                    .queryParam("apiKey", props.getGeoapify().getApiKey())
                    .queryParam("lang", "en")
                    .build()
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return List.of();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> features =
                    (List<Map<String, Object>>) response.get("features");
            if (features == null || features.isEmpty()) {
                log.debug("Geoapify: no hotels found near lat={} lng={}", lat, lng);
                return List.of();
            }

            List<HotelCandidateDto> hotels = new ArrayList<>();
            for (Map<String, Object> feature : features) {
                HotelCandidateDto hotel = normalizeFeature(feature, lat, lng, city,
                        currency, budget, preference);
                if (hotel != null) hotels.add(hotel);
                if (hotels.size() >= MAX_RESULTS) break;
            }

            log.info("Geoapify returned {} hotels near lat={} lng={} ({})",
                    hotels.size(), lat, lng, city);
            return hotels;

        } catch (Exception e) {
            log.warn("Geoapify hotel search failed for lat={} lng={}: {}", lat, lng, e.getMessage());
            return List.of();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private HotelCandidateDto normalizeFeature(Map<String, Object> feature,
                                                double centerLat, double centerLng,
                                                String city, String currency,
                                                Double budget, String preference) {
        try {
            Map<String, Object> props = (Map<String, Object>) feature.get("properties");
            Map<String, Object> geometry = (Map<String, Object>) feature.get("geometry");
            if (props == null || geometry == null) return null;

            String name = (String) props.get("name");
            if (name == null || name.isBlank()) return null;

            // Coordinates — GeoJSON [lng, lat]
            List<Number> coords = (List<Number>) geometry.get("coordinates");
            if (coords == null || coords.size() < 2) return null;
            double lng = coords.get(0).doubleValue();
            double lat = coords.get(1).doubleValue();

            // Distance from center
            double distKm = haversineKm(centerLat, centerLng, lat, lng);

            // Category → hotel tier
            String categories = props.get("categories") != null
                    ? props.get("categories").toString() : "";
            String category = inferCategory(categories, preference);

            // Address
            String street  = (String) props.get("street");
            String suburb  = (String) props.get("suburb");
            String areaName = suburb != null ? suburb : city;
            String address = buildAddress(name, street, city);

            // Rating — Geoapify doesn't provide ratings, use tier default
            double rating = switch (category) {
                case "LUXURY" -> 4.5;
                case "BUDGET" -> 3.7;
                default       -> 4.1;
            };

            // Price estimate
            double nightlyRate = estimateNightlyRate(budget, preference, currency);

            // Popularity
            double popularity = Math.min(10.0, rating * 1.8);

            // Stable ID
            String placeId = props.get("place_id") != null
                    ? "geoapify_" + props.get("place_id").toString()
                    : "geoapify_" + Math.round(lat * 10000) + "_" + Math.round(lng * 10000);

            // Amenities — basic defaults since Geoapify doesn't return amenities
            List<String> amenities = List.of("wifi");

            return HotelCandidateDto.builder()
                    .externalHotelId(placeId)
                    .name(name)
                    .address(address)
                    .areaName(areaName)
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
            log.debug("Failed to normalize Geoapify feature: {}", e.getMessage());
            return null;
        }
    }

    private String inferCategory(String categories, String preference) {
        if (preference != null) {
            return switch (preference.toUpperCase()) {
                case "LUXURY" -> "LUXURY";
                case "BUDGET" -> "BUDGET";
                default -> "STANDARD";
            };
        }
        if (categories.contains("hostel")) return "BUDGET";
        if (categories.contains("resort")) return "LUXURY";
        return "STANDARD";
    }

    private double estimateNightlyRate(Double budget, String preference, String currency) {
        double base = switch (preference != null ? preference.toUpperCase() : "STANDARD") {
            case "LUXURY" -> 200.0;
            case "BUDGET" -> 40.0;
            default       -> 90.0;
        };
        if (budget != null && budget > 0) {
            double hotelSlice = budget * 0.30;
            base = Math.max(base * 0.5, Math.min(base * 3.0, hotelSlice));
        }
        return Math.round(base * 100.0) / 100.0;
    }

    private String buildAddress(String name, String street, String city) {
        if (street != null && !street.isBlank()) return street + ", " + city;
        return city;
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

    public String getProviderName() { return PROVIDER_NAME; }
}
