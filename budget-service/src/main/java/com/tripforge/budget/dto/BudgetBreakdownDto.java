package com.tripforge.budget.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
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

    // Phase 9E: currency fields
    @Builder.Default
    private String currencyCode = "INR";
    private BigDecimal exchangeRateUsed;
    private String fxSourceProvider;
    @Builder.Default
    private boolean fxFallbackUsed = false;
    private List<String> warnings;
}
