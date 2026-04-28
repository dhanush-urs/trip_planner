package com.tripforge.external.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Normalized hotel candidate DTO.
 * Returned by external-data-service to hotel-service for ML ranking.
 * Provider-agnostic — works for Google Places, RapidAPI, or any future hotel provider.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HotelCandidateDto {

    /** Provider-assigned hotel ID */
    private String externalHotelId;

    /** Human-readable name */
    private String name;

    /** Full address */
    private String address;

    /** Area / neighborhood name */
    private String areaName;

    /** Latitude */
    private Double lat;

    /** Longitude */
    private Double lng;

    /** Distance from trip centroid in km (computed by external-data-service) */
    private Double distanceFromCenterKm;

    /** Rating (0–5 scale, normalized) */
    private Double rating;

    /** Number of reviews */
    private Integer reviewCount;

    /** Price per night in the requested currency */
    private Double pricePerNight;

    /** Currency code for the price */
    private String currencyCode;

    /** Hotel category: BUDGET / STANDARD / LUXURY */
    private String category;

    /** Amenities list */
    private List<String> amenities;

    /** Primary photo URL */
    private String photoUrl;

    /** Provider that served this data */
    private String sourceProvider;

    /** Whether this came from a fallback provider */
    private boolean fallbackUsed;

    /** Popularity score (0–10, normalized from provider signals) */
    private Double popularityScore;
}
