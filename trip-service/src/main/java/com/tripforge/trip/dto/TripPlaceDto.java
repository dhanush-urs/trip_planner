package com.tripforge.trip.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripPlaceDto {
    private Long attractionId;
    private String name;
    private String category;
    private String visitTime;
    private Double avgVisitHours;
    private BigDecimal ticketCost;
    private String notes;
    private Integer visitOrder;

    // Phase 9C additions
    private String externalPlaceId;
    private Double lat;
    private Double lng;
    private Integer travelTimeFromPreviousMinutes;
}
