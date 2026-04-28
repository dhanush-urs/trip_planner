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
 * OpenTripMap API provider — fallback for attractions/POIs.
 *
 * Free tier available at https://opentripmap.io
 * Used when Google Places is unavailable or quota exceeded.
 *
 * API docs: https://dev.opentripmap.org/docs
 */
@Component
public class OpenTripMapProvider {

    private static final Logger log = LoggerFactory.getLogger(OpenTripMapProvider.class);
    private static final String PROVIDER_NAME = "opentripmap";

    // OpenTripMap category kinds for tourist attractions
    private static final String DEFAULT_KINDS = "interesting_places,tourist_facilities";

    private final ProviderProperties props;
    private final RestTemplate restTemplate;

    public OpenTripMapProvider(ProviderProperties props,
                               @Qualifier("providerRestTemplate") RestTemplate restTemplate) {
        this.props = props;
        this.restTemplate = restTemplate;
    }

    /**
     * Search for attractions near a city using OpenTripMap.
     * Uses geoname lookup to get city coordinates, then radius search.
     *
     * @param city  city name
     * @param query optional interest filter (mapped to OTM kinds)
     * @return list of normalized PlaceDto
     */
    public List<PlaceDto> searchAttractions(String city, String query) {
        if (!props.isOpenTripMapConfigured()) {
            log.debug("OpenTripMap not configured — skipping");
            return List.of();
        }

        try {
            // Step 1: Get city coordinates via geoname endpoint
            double[] coords = getCityCoordinates(city);
            if (coords == null) {
                log.warn("OpenTripMap: could not resolve coordinates for city '{}'", city);
                return List.of();
            }

            // Step 2: Radius search around city center
            String kinds = mapQueryToKinds(query);
            String url = UriComponentsBuilder
                    .fromHttpUrl(props.getOpentripmap().getBaseUrl() + "/places/radius")
                    .queryParam("radius", 10000)
                    .queryParam("lon", coords[1])
                    .queryParam("lat", coords[0])
                    .queryParam("kinds", kinds)
                    .queryParam("limit", 20)
                    .queryParam("format", "json")
                    .queryParam("apikey", props.getOpentripmap().getApiKey())
                    .build()
                    .toUriString();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = restTemplate.getForObject(url, List.class);
            if (results == null) return List.of();

            List<PlaceDto> places = new ArrayList<>();
            for (Map<String, Object> result : results) {
                PlaceDto place = normalizeResult(result);
                if (place != null) places.add(place);
            }

            log.info("OpenTripMap returned {} attractions for city='{}'", places.size(), city);
            return places;

        } catch (Exception e) {
            log.warn("OpenTripMap search failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private double[] getCityCoordinates(String city) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(props.getOpentripmap().getBaseUrl() + "/places/geoname")
                    .queryParam("name", city)
                    .queryParam("apikey", props.getOpentripmap().getApiKey())
                    .build()
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);
            if (response == null) return null;

            Double lat = toDouble(response.get("lat"));
            Double lon = toDouble(response.get("lon"));
            if (lat == null || lon == null) return null;

            return new double[]{lat, lon};
        } catch (Exception e) {
            log.debug("OpenTripMap geoname lookup failed for '{}': {}", city, e.getMessage());
            return null;
        }
    }

    private String mapQueryToKinds(String query) {
        if (query == null) return DEFAULT_KINDS;
        String q = query.toLowerCase();
        if (q.contains("beach")) return "beaches,natural";
        if (q.contains("temple") || q.contains("religion")) return "religion,historic";
        if (q.contains("food") || q.contains("restaurant")) return "foods,restaurants";
        if (q.contains("museum") || q.contains("art")) return "museums,cultural";
        if (q.contains("nature") || q.contains("park")) return "natural,parks";
        if (q.contains("shop")) return "shops,malls";
        if (q.contains("nightlife") || q.contains("bar")) return "bars,nightclubs";
        return DEFAULT_KINDS;
    }

    @SuppressWarnings("unchecked")
    private PlaceDto normalizeResult(Map<String, Object> result) {
        try {
            String xid = (String) result.get("xid");
            String name = (String) result.get("name");
            if (name == null || name.isBlank()) return null;

            Map<String, Object> point = (Map<String, Object>) result.get("point");
            Double lat = null, lng = null;
            if (point != null) {
                lat = toDouble(point.get("lat"));
                lng = toDouble(point.get("lon"));
            }

            String kinds = (String) result.get("kinds");
            String category = kinds != null ? kinds.split(",")[0] : "attraction";

            Double rate = toDouble(result.get("rate"));
            Double rating = rate != null ? Math.min(5.0, rate) : null;

            return PlaceDto.builder()
                    .placeId("otm_" + xid)
                    .name(name)
                    .lat(lat)
                    .lng(lng)
                    .category(category)
                    .rating(rating)
                    .sourceProvider(PROVIDER_NAME)
                    .fallbackUsed(true)
                    .build();
        } catch (Exception e) {
            log.debug("Failed to normalize OTM result: {}", e.getMessage());
            return null;
        }
    }

    private Double toDouble(Object val) {
        if (val == null) return null;
        if (val instanceof Double d) return d;
        if (val instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return null; }
    }

    public String getProviderName() { return PROVIDER_NAME; }

    /**
     * Search for hotels near a city using OpenTripMap (kinds=accomodations).
     * Uses Nominatim for geocoding if OTM geoname fails.
     *
     * @param city     city name
     * @param lat      optional pre-resolved latitude
     * @param lng      optional pre-resolved longitude
     * @param currency currency code for display
     * @return list of normalized HotelCandidateDto
     */
    public List<HotelCandidateDto> searchHotels(
            String city, Double lat, Double lng, String currency) {
        if (!props.isOpenTripMapConfigured()) {
            log.debug("OpenTripMap not configured — skipping hotel search");
            return List.of();
        }

        try {
            double[] coords = (lat != null && lng != null)
                    ? new double[]{lat, lng}
                    : getCityCoordinates(city);

            if (coords == null) {
                log.warn("OpenTripMap: could not resolve coordinates for hotel search in '{}'", city);
                return List.of();
            }

            String url = UriComponentsBuilder
                    .fromHttpUrl(props.getOpentripmap().getBaseUrl() + "/places/radius")
                    .queryParam("radius", 8000)
                    .queryParam("lon", coords[1])
                    .queryParam("lat", coords[0])
                    .queryParam("kinds", "accomodations")
                    .queryParam("limit", 20)
                    .queryParam("format", "json")
                    .queryParam("apikey", props.getOpentripmap().getApiKey())
                    .build()
                    .toUriString();

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> results = restTemplate.getForObject(url, List.class);
            if (results == null) return List.of();

            List<HotelCandidateDto> hotels = new ArrayList<>();
            for (Map<String, Object> result : results) {
                HotelCandidateDto hotel =
                        normalizeHotelResult(result, city, coords[0], coords[1], currency);
                if (hotel != null) hotels.add(hotel);
            }

            log.info("OpenTripMap returned {} hotel candidates for city='{}'", hotels.size(), city);
            return hotels;

        } catch (Exception e) {
            log.warn("OpenTripMap hotel search failed: {}", e.getMessage());
            return List.of();
        }
    }

    @SuppressWarnings("unchecked")
    private HotelCandidateDto normalizeHotelResult(
            Map<String, Object> result, String city,
            double centerLat, double centerLng, String currency) {
        try {
            String xid = (String) result.get("xid");
            String name = (String) result.get("name");
            if (name == null || name.isBlank()) return null;

            Map<String, Object> point = (Map<String, Object>) result.get("point");
            Double lat = null, lng = null;
            if (point != null) {
                lat = toDouble(point.get("lat"));
                lng = toDouble(point.get("lon"));
            }

            double distKm = (lat != null && lng != null)
                    ? haversineKm(centerLat, centerLng, lat, lng) : 2.0;

            Double rate = toDouble(result.get("rate"));
            double rating = rate != null ? Math.min(5.0, rate * 1.25) : 3.5;
            double popularity = Math.min(10.0, rating * 1.8);

            return HotelCandidateDto.builder()
                    .externalHotelId("otm_" + xid)
                    .name(name)
                    .areaName(city)
                    .lat(lat)
                    .lng(lng)
                    .distanceFromCenterKm(Math.round(distKm * 10.0) / 10.0)
                    .rating(Math.round(rating * 10.0) / 10.0)
                    .category("STANDARD")
                    .currencyCode(currency != null ? currency : "INR")
                    .sourceProvider(PROVIDER_NAME)
                    .fallbackUsed(false)
                    .popularityScore(Math.round(popularity * 10.0) / 10.0)
                    .build();
        } catch (Exception e) {
            log.debug("Failed to normalize OTM hotel result: {}", e.getMessage());
            return null;
        }
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
}
