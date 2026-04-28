package com.tripforge.external.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Normalized route / directions DTO.
 * Returned by external-data-service after optimizing a list of waypoints.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RouteDto {

    /** Ordered list of stops in the optimized route */
    private List<RouteStop> stops;

    /** Total travel time for the route in minutes */
    private Integer totalTravelMinutes;

    /** Total distance in km */
    private Double totalDistanceKm;

    /** Provider that served this data */
    private String sourceProvider;

    /** Whether this came from a fallback provider or heuristic */
    private boolean fallbackUsed;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class RouteStop {

        /** Index in the optimized order */
        private Integer order;

        /** Place ID (matches PlaceDto.placeId) */
        private String placeId;

        /** Place name */
        private String name;

        /** Latitude */
        private Double lat;

        /** Longitude */
        private Double lng;

        /** Travel time from the previous stop in minutes */
        private Integer travelTimeFromPreviousMinutes;

        /** Distance from the previous stop in km */
        private Double distanceFromPreviousKm;

        /** Suggested arrival time (e.g. "09:00") */
        private String suggestedArrivalTime;

        /** Estimated visit duration in minutes */
        private Integer estimatedVisitMinutes;
    }
}
