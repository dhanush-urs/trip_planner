package com.tripforge.ai.dto;

import lombok.Data;
import java.util.List;

@Data
public class ItineraryExplanationRequest {
    private String destination;
    private Integer days;
    private String tripStyle;
    private String pace;
    private List<PlaceInfo> places;
    private String providerMode;

    @Data
    public static class PlaceInfo {
        private String name;
        private String category;
    }
}
