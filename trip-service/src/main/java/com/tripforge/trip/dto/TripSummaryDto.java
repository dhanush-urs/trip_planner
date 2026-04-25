package com.tripforge.trip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lightweight trip summary for history/list views.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripSummaryDto {
    private Long tripId;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;
    private BigDecimal totalBudget;
    private Integer travelers;
    private String status;
    private LocalDateTime createdAt;
}
