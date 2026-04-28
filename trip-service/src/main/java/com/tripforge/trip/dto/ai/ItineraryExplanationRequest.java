package com.tripforge.trip.dto.ai;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class ItineraryExplanationRequest {
    private String destination;
    private Integer days;
    private String tripStyle;
    private String pace;
    private List<PlaceInfo> places;
    private String providerMode;

    @Data @Builder
    public static class PlaceInfo {
        private String name;
        private String category;
    }
}
