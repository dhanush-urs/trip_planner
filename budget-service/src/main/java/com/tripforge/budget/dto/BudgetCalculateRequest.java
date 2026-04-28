package com.tripforge.budget.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
public class BudgetCalculateRequest {
    private Long tripId;
    private String destination;
    private Integer durationDays;
    private Integer travelers;
    private BigDecimal totalBudget;
    private BigDecimal hotelPricePerNight;
    private String hotelCategory;
    private List<Map<String, Object>> itinerary;
    // Phase 9E
    private String currencyCode;
}
