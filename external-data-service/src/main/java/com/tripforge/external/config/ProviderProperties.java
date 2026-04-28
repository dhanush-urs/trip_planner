package com.tripforge.external.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Strongly-typed provider configuration.
 * Bound from application.yml providers.* namespace.
 *
 * Missing keys degrade gracefully — never crash startup.
 * Each provider has an isXxxConfigured() method that returns false
 * when the key is absent, causing the provider to be skipped.
 */
@Component
@ConfigurationProperties(prefix = "providers")
@Data
public class ProviderProperties {

    private Google google = new Google();
    private OpenTripMap opentripmap = new OpenTripMap();
    private OpenRouteService openrouteservice = new OpenRouteService();
    private Frankfurter frankfurter = new Frankfurter();
    private Geoapify geoapify = new Geoapify();
    private Foursquare foursquare = new Foursquare();
    // NOTE: Amadeus removed — requires OAuth2 + billing, not suitable for free-tier

    @Data
    public static class Google {
        private Places places = new Places();
        private Directions directions = new Directions();

        @Data
        public static class Places {
            private String apiKey = "";
            private String baseUrl = "https://maps.googleapis.com/maps/api";
            private boolean enabled = true;
            private int timeoutMs = 5000;
        }

        @Data
        public static class Directions {
            private String apiKey = "";
            private String baseUrl = "https://maps.googleapis.com/maps/api";
            private boolean enabled = true;
            private int timeoutMs = 5000;
        }
    }

    @Data
    public static class OpenTripMap {
        private String apiKey = "";
        private String baseUrl = "https://api.opentripmap.com/0.1/en";
        private boolean enabled = true;
        private int timeoutMs = 5000;
    }

    @Data
    public static class OpenRouteService {
        private String apiKey = "";
        private String baseUrl = "https://api.openrouteservice.org";
        private boolean enabled = true;
        private int timeoutMs = 5000;
    }

    @Data
    public static class Frankfurter {
        private String baseUrl = "https://api.frankfurter.app";
        private boolean enabled = true;
        private int timeoutMs = 3000;
    }

    /**
     * Geoapify — geocoding + places + routing.
     * Free tier: 3,000 requests/day, no credit card.
     * https://www.geoapify.com/
     */
    @Data
    public static class Geoapify {
        private String apiKey = "";
        private String baseUrl = "https://api.geoapify.com";
        private boolean enabled = true;
        private int timeoutMs = 5000;
    }

    /**
     * Foursquare Places API — optional hotel/POI search.
     * Free tier available: https://developer.foursquare.com/
     */
    @Data
    public static class Foursquare {
        private String apiKey = "";
        private String baseUrl = "https://api.foursquare.com/v3";
        private boolean enabled = true;
        private int timeoutMs = 5000;
    }

    // NOTE: Amadeus is intentionally removed from the active provider stack.
    // It requires OAuth2 + billing account and is not suitable for free-tier demos.
    // Aviationstack is for flights only — not used for hotels.

    // ── Configuration check helpers ───────────────────────────────────────────

    public boolean isGooglePlacesConfigured() {
        return google.places.enabled
                && google.places.apiKey != null
                && !google.places.apiKey.isBlank();
    }

    public boolean isGoogleDirectionsConfigured() {
        return google.directions.enabled
                && google.directions.apiKey != null
                && !google.directions.apiKey.isBlank();
    }

    public boolean isOpenTripMapConfigured() {
        return opentripmap.enabled
                && opentripmap.apiKey != null
                && !opentripmap.apiKey.isBlank();
    }

    public boolean isOpenRouteServiceConfigured() {
        return openrouteservice.enabled
                && openrouteservice.apiKey != null
                && !openrouteservice.apiKey.isBlank();
    }

    public boolean isGeoapifyConfigured() {
        return geoapify.enabled
                && geoapify.apiKey != null
                && !geoapify.apiKey.isBlank();
    }

    public boolean isFoursquareConfigured() {
        return foursquare.enabled
                && foursquare.apiKey != null
                && !foursquare.apiKey.isBlank();
    }
}
