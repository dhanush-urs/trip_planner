package com.tripforge.hotel.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Mirrors the ProviderResponse<List<HotelCandidateDto>> shape returned by external-data-service.
 * Uses @JsonIgnoreProperties to be resilient against future field additions.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalHotelSearchResponse {

    private List<HotelCandidateDto> data;
    private String sourceProvider;
    private boolean fallbackUsed;
    private boolean degradedMode;
    private List<String> warnings;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HotelCandidateDto {
        private String externalHotelId;
        private String name;
        private String address;
        private String areaName;
        private Double lat;
        private Double lng;
        private Double distanceFromCenterKm;
        private Double rating;
        private Integer reviewCount;
        private Double pricePerNight;
        private String currencyCode;
        private String category;
        private List<String> amenities;
        private String photoUrl;
        private String sourceProvider;
        private boolean fallbackUsed;
        private Double popularityScore;
    }
}
