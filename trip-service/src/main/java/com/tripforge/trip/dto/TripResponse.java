package com.tripforge.trip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Full trip response — returned after trip creation or when fetching a trip.
 * Aggregates data from hotel, route, budget, and split services.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TripResponse {

    private Long tripId;
    private Long userId;
    private String destination;
    private LocalDate startDate;
    private LocalDate endDate;
    private Integer durationDays;
    private BigDecimal totalBudget;
    private Integer travelers;
    private List<String> interests;
    private String hotelPreference;
    private String status;
    private LocalDateTime createdAt;

    // Aggregated from downstream services
    private List<ItineraryDayDto> itinerary;
    private HotelDto selectedHotel;
    private List<HotelDto> alternativeHotels;
    private BudgetBreakdownDto budgetBreakdown;
    private SplitResultDto splitResult;
}
