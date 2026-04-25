package com.tripforge.trip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripPlaceDto {
    private Long attractionId;
    private String name;
    private String category;
    private String visitTime;
    private Double avgVisitHours;
    private BigDecimal ticketCost;
    private String notes;
    private Integer visitOrder;
}
