package com.tripforge.hotel.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HotelDto {
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
}
