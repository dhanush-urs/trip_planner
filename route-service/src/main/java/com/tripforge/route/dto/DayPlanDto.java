package com.tripforge.route.dto;

import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class DayPlanDto {
    private Integer dayNumber;
    private LocalDate date;
    private String theme;
    private List<PlaceDto> places;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class PlaceDto {
        private Long attractionId;
        private String name;
        private String category;
        private String visitTime;
        private Double avgVisitHours;
        private BigDecimal ticketCost;
        private String notes;
        private Integer visitOrder;
    }
}
