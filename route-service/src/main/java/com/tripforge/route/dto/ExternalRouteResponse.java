package com.tripforge.route.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

/**
 * Mirrors the ProviderResponse<RouteDto> shape returned by external-data-service.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ExternalRouteResponse {

    private RouteData data;
    private String sourceProvider;
    private boolean fallbackUsed;
    private boolean degradedMode;
    private List<String> warnings;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RouteData {
        private List<RouteStop> stops;
        private Integer totalTravelMinutes;
        private Double totalDistanceKm;
        private String sourceProvider;
        private boolean fallbackUsed;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class RouteStop {
        private Integer order;
        private String placeId;
        private String name;
        private Double lat;
        private Double lng;
        private Integer travelTimeFromPreviousMinutes;
        private Double distanceFromPreviousKm;
        private String suggestedArrivalTime;
        private Integer estimatedVisitMinutes;
    }
}
