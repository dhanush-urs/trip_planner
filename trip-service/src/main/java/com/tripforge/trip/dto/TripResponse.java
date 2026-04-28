package com.tripforge.trip.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
 *
 * Phase 9C additions (all nullable — backward-compatible):
 *   - providerMode     LIVE | MIXED | FALLBACK
 *   - providerSummary  human-readable summary of which providers were used
 *   - warnings         list of data quality warnings
 *   - currency         currency code used for this trip
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripResponse {

    // ── Existing fields (unchanged) ───────────────────────────────────────────
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

    // ── Phase 9C additions ────────────────────────────────────────────────────

    /**
     * LIVE   = all major components used live providers
     * MIXED  = some live, some fallback
     * FALLBACK = core planning relied on local datasets / heuristic only
     */
    private String providerMode;

    /** Human-readable summary of which providers were used */
    private String providerSummary;

    /** Data quality warnings from downstream services */
    private List<String> warnings;

    /** Currency code for all monetary values in this response */
    @Builder.Default
    private String currency = "INR";

    // ── Phase 9D additions — AI enrichment (all nullable, backward-compatible) ─

    /** AI-generated trip headline (e.g. "A balanced 4-day Goa getaway") */
    private String aiHeadline;

    /** AI-generated short trip summary (2 sentences max) */
    private String aiSummary;

    /** AI explanation for why the selected hotel was chosen */
    private String hotelExplanation;

    /** AI explanation for why the itinerary sequence makes sense */
    private String itineraryExplanation;

    /** Whether AI enrichment used Gemini (false = deterministic fallback) */
    private Boolean aiEnriched;

    /** AI provider used: "gemini" or "keyword_fallback" */
    private String aiProvider;

    // ── Phase 9F additions — payment summary (optional, non-blocking) ─────────

    /** Payment summary from payment-service (null if not initialized or service unavailable) */
    private com.tripforge.trip.dto.payment.PaymentSummaryDto paymentSummary;

    /** True if payment-service is available and tracking is initialized */
    private Boolean paymentAvailable;
}
