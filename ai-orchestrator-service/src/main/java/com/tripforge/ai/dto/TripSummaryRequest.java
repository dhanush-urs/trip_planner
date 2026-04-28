package com.tripforge.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class TripSummaryRequest {
    private String destination;
    private Integer days;
    private String hotelName;
    private Double budget;
    private String currency;
    private String tripStyle;
    private String pace;
    private String providerMode;
    private List<String> topPlaces;
}
