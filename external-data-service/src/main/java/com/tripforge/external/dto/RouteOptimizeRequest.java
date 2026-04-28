package com.tripforge.external.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Request body for POST /api/external/routes/optimize
 */
@Data
public class RouteOptimizeRequest {

    @NotEmpty(message = "At least one waypoint is required")
    private List<Waypoint> waypoints;

    /** Travel mode: DRIVING, WALKING, TRANSIT */
    private String travelMode = "DRIVING";

    /** Optimize for: TIME or DISTANCE */
    private String optimizeFor = "TIME";

    @Data
    public static class Waypoint {
        @NotNull
        private String placeId;
        private String name;
        @NotNull
        private Double lat;
        @NotNull
        private Double lng;
        /** Estimated visit duration in minutes */
        private Integer estimatedVisitMinutes;
    }
}
