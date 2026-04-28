package com.tripforge.trip.dto.ai;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class HotelExplanationRequest {
    private String destination;
    private String hotelName;
    private String hotelPreference;
    private Double budget;
    private Integer travelers;
    private List<String> reasons;
    private Double rating;
    private String areaName;
    private Double distanceFromTripCentroid;
    private String providerMode;
}
