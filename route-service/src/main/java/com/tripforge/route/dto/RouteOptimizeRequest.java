package com.tripforge.route.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class RouteOptimizeRequest {
    private Long tripId;
    private String destination;
    private LocalDate startDate;
    private Integer durationDays;
    private List<String> interests;

    /**
     * Phase 9C: optional live places with coordinates.
     * When present and all have lat/lng, live route optimization is attempted.
     * When absent or empty, falls back to CSV heuristic.
     */
    private List<LivePlace> livePlaces;

    @Data
    public static class LivePlace {
        private String placeId;
        private String name;
        private String category;
        private Double lat;
        private Double lng;
        private Integer estimatedVisitMinutes;
    }
}
