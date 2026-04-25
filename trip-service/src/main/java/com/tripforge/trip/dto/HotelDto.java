package com.tripforge.trip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Hotel DTO used within trip responses (mirrors hotel-service HotelDto).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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
}
