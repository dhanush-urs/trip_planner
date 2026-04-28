package com.tripforge.trip.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Hotel DTO used within trip responses.
 *
 * Phase 10E: added truthfulness contract fields (sourceType, priceType, providerName).
 * These allow the frontend to render honest badges and never falsely claim
 * live pricing for estimated data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HotelDto {
    private Long id;
    private String name;
    private String destination;
    private BigDecimal pricePerNight;
    private Double rating;
    private Double distanceFromCenterKm;
    private List<String> amenities;
    private String category;
    private Double popularityScore;
    private Double relevanceScore;

    // Phase 9C additions
    private String externalHotelId;
    private String sourceProvider;
    private boolean fallbackUsed;
    private String imageUrl;
    private Integer reviewCount;
    private Double lat;
    private Double lng;
    private String areaName;
    private List<String> warnings;

    // Phase 10E — Truthfulness contract
    /**
     * Source type: LIVE | LIVE_NO_RATE | BASIC_PLACE_DATA | DATASET | SYNTHETIC
     */
    private String sourceType;

    /**
     * Price type: LIVE_PRICE | ESTIMATED_PRICE | DATASET_PRICE | NO_PRICE
     */
    private String priceType;

    /**
     * Provider name: AMADEUS | OVERPASS_OSM | GEOAPIFY | CSV | SYNTHETIC
     */
    private String providerName;

    /** True if this hotel is a synthetic generated recommendation */
    private boolean isSynthetic;

    /** Booking URL if available from provider */
    private String bookingUrl;
}
