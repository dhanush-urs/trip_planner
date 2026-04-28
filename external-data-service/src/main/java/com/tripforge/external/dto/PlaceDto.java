package com.tripforge.external.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Normalized place / POI DTO.
 * Provider-agnostic — works for Google Places, OpenTripMap, or any future provider.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PlaceDto {

    /** Internal or provider-assigned place ID */
    private String placeId;

    /** Human-readable name */
    private String name;

    /** Full formatted address */
    private String address;

    /** City / locality */
    private String city;

    /** Country */
    private String country;

    /** Latitude */
    private Double lat;

    /** Longitude */
    private Double lng;

    /** Category / type (e.g. "tourist_attraction", "restaurant", "museum") */
    private String category;

    /** Sub-types from provider */
    private List<String> types;

    /** Rating (0–5 scale, normalized) */
    private Double rating;

    /** Number of reviews */
    private Integer reviewCount;

    /** Primary photo URL (if available) */
    private String photoUrl;

    /** Opening hours summary (e.g. "Mon-Fri 9am-5pm") */
    private String openingHours;

    /** Estimated visit duration in minutes */
    private Integer estimatedVisitMinutes;

    /** Admission/ticket cost estimate (in provider's currency) */
    private Double ticketCostEstimate;

    /** Provider that served this data */
    private String sourceProvider;

    /** Whether this came from a fallback provider */
    private boolean fallbackUsed;
}
