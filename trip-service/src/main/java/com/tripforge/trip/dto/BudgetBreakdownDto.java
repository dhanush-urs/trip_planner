package com.tripforge.trip.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * Budget breakdown DTO — mirrors budget-service BudgetBreakdownDto exactly
 * so all fields survive Feign deserialization without being dropped.
 *
 * Phase 9E: currencyCode, exchangeRateUsed, fxSourceProvider, fxFallbackUsed, warnings
 * are all required for correct multi-currency display on the frontend.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
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

    // Phase 9E: currency fields — must NOT have @Builder.Default so the
    // value from budget-service is preserved during deserialization.
    // The frontend reads budgetBreakdown.currencyCode for all formatting.
    private String currencyCode;
    private BigDecimal exchangeRateUsed;
    private String fxSourceProvider;
    private boolean fxFallbackUsed;
    private List<String> warnings;
}
