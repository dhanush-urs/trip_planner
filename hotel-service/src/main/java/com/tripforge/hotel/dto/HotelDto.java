package com.tripforge.hotel.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Hotel DTO — returned by hotel-service to trip-service and frontend.
 *
 * Truthfulness contract (Phase 10E):
 *   sourceType  — where the hotel data came from
 *   priceType   — how the price was determined
 *   providerName — which provider served the data
 *
 * These three fields allow the frontend to render honest badges
 * and never falsely claim live pricing for estimated data.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HotelDto {

    // ── Core fields ───────────────────────────────────────────────────────────
    private Long id;
    private String name;
    private String destination;
    private Double pricePerNight;
    private Double rating;
    private Double distanceFromCenterKm;
    private List<String> amenities;
    private String category;
    private Double popularityScore;
    private Double relevanceScore;

    // ── Provider identity ─────────────────────────────────────────────────────

    /** Provider-assigned hotel ID */
    private String externalHotelId;

    /**
     * Source type — truthfulness contract.
     * Allowed values:
     *   LIVE            — real hotel from live provider with live pricing
     *   LIVE_NO_RATE    — real hotel from live provider, no live pricing available
     *   BASIC_PLACE_DATA — real place name from OSM/Overpass, price estimated
     *   DATASET         — matched from local CSV dataset
     *   SYNTHETIC       — generated fallback recommendation
     */
    @Builder.Default
    private String sourceType = "BASIC_PLACE_DATA";

    /**
     * Price type — how the nightly rate was determined.
     * Allowed values:
     *   LIVE_PRICE      — real price from live booking provider
     *   ESTIMATED_PRICE — estimated from budget model / heuristics
     *   DATASET_PRICE   — price from local CSV dataset
     *   NO_PRICE        — no price available
     */
    @Builder.Default
    private String priceType = "ESTIMATED_PRICE";

    /**
     * Which provider served this hotel.
     * Examples: AMADEUS, OVERPASS_OSM, GEOAPIFY, CSV, SYNTHETIC
     */
    @Builder.Default
    private String providerName = "OVERPASS_OSM";

    /** Legacy field — kept for backward compatibility */
    @Builder.Default
    private String sourceProvider = "csv_dataset";

    /** True if this hotel came from a fallback (non-live) source */
    @Builder.Default
    private boolean fallbackUsed = false;

    /** True if this hotel is a synthetic generated recommendation */
    @Builder.Default
    private boolean isSynthetic = false;

    // ── Optional enrichment ───────────────────────────────────────────────────

    /** Photo URL from provider */
    private String imageUrl;

    /** Number of reviews */
    private Integer reviewCount;

    /** Latitude */
    private Double lat;

    /** Longitude */
    private Double lng;

    /** Area / neighborhood name */
    private String areaName;

    /** Booking URL if available from provider */
    private String bookingUrl;

    /** Data quality warnings */
    private List<String> warnings;
}
