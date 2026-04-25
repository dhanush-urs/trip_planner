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
public class BudgetBreakdownDto {
    private Long tripId;
    private BigDecimal hotelCost;
    private BigDecimal foodCost;
    private BigDecimal transportCost;
    private BigDecimal attractionCost;
    private BigDecimal miscCost;
    private BigDecimal totalEstimated;
    private BigDecimal totalBudget;
    private BigDecimal remainingBudget;
    private boolean overBudget;
}
