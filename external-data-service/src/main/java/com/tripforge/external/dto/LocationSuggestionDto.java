package com.tripforge.external.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Normalized destination suggestion returned by the location search endpoint.
 * Used by the frontend autocomplete for the destination input.
 *
 * Supports worldwide destinations: cities, towns, regions, states, islands.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class LocationSuggestionDto {

    /**
     * Stable composite ID for deduplication.
     * Format: "nominatim:{osm_type}:{osm_id}" or "nominatim:{lat},{lng}"
     */
    private String id;

    /**
     * Full human-readable display name.
     * e.g. "Dubai, Dubai, United Arab Emirates"
     *      "Paris, Ile-de-France, France"
     *      "Kyoto, Kyoto Prefecture, Japan"
     */
    private String displayName;

    /**
     * Primary label — the most specific name (city, town, region).
     * e.g. "Dubai", "Paris", "Kyoto", "California"
     */
    private String primaryText;

    /**
     * Secondary label — context (state/country).
     * e.g. "Dubai, United Arab Emirates"
     *      "Ile-de-France, France"
     */
    private String secondaryText;

    /** City or locality name */
    private String city;

    /** State, province, or region */
    private String state;

    /** Country name (English) */
    private String country;

    /** ISO 3166-1 alpha-2 country code (e.g. "AE", "FR", "JP") */
    private String countryCode;

    /**
     * Place type: "city" | "town" | "village" | "region" | "state" | "island" | "district"
     */
    private String type;

    /** Latitude */
    private Double lat;

    /** Longitude */
    private Double lng;

    /** Which provider served this result */
    private String sourceProvider;
}
