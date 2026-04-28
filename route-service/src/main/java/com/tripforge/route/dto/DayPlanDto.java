package com.tripforge.route.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class DayPlanDto {
    private Integer dayNumber;
    private LocalDate date;
    private String theme;
    private List<PlaceDto> places;

    // Phase 9C additions
    private String sourceProvider;
    private boolean fallbackUsed;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class PlaceDto {
        private Long attractionId;

        // Phase 9C additions
        private String externalPlaceId;
        private Double lat;
        private Double lng;
        private Integer travelTimeFromPreviousMinutes;

        private String name;
        private String category;
        private String visitTime;
        private Double avgVisitHours;
        private BigDecimal ticketCost;
        private String notes;
        private Integer visitOrder;
    }
}
