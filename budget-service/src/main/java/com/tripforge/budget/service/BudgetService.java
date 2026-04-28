package com.tripforge.budget.service;

import com.tripforge.budget.client.FxServiceClient;
import com.tripforge.budget.dto.BudgetBreakdownDto;
import com.tripforge.budget.dto.BudgetCalculateRequest;
import com.tripforge.budget.dto.FxRateResponse;
import com.tripforge.budget.entity.BudgetBreakdown;
import com.tripforge.budget.repository.BudgetBreakdownRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Budget calculation service — Phase 9E upgrade.
 *
 * Strategy:
 *   1. All base rates (food, transport) are defined in INR
 *   2. Hotel price arrives in the currency it was sourced in
 *      (INR for CSV hotels, provider currency for live hotels)
 *   3. If selected currency != INR:
 *      - convert all INR-based costs via external-data-service FX
 *      - if FX unavailable: return INR amounts with fxFallbackUsed=true + warning
 *   4. All output amounts are in the selected currency
 *   5. exchangeRateUsed is stored for auditability
 */
@Service
public class BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);
    private static final String BASE_CURRENCY = "INR";

    @Autowired private BudgetBreakdownRepository breakdownRepository;
    @Autowired private FxServiceClient fxServiceClient;

    @Transactional
    public BudgetBreakdownDto calculate(Map<String, Object> requestMap) {
        BudgetCalculateRequest request = parseRequest(requestMap);
        String targetCurrency = (request.getCurrencyCode() != null
                && !request.getCurrencyCode().isBlank())
                ? request.getCurrencyCode().toUpperCase() : BASE_CURRENCY;

        log.info("Calculating budget for trip {} | {} days | {} travelers | currency={}",
                request.getTripId(), request.getDurationDays(), request.getTravelers(), targetCurrency);

        int days = request.getDurationDays() != null ? request.getDurationDays() : 1;
        int travelers = request.getTravelers() != null ? request.getTravelers() : 1;
        String destination = request.getDestination() != null ? request.getDestination() : "";

        // ── Step 1: Compute all costs in INR (base currency) ─────────────────

        // Hotel cost — hotel price may already be in target currency if from live provider
        BigDecimal hotelCostInr = BigDecimal.ZERO;
        if (request.getHotelPricePerNight() != null) {
            // If hotel price currency matches target, no conversion needed for hotel
            // Otherwise treat as INR (CSV dataset prices are INR-based)
            hotelCostInr = request.getHotelPricePerNight()
                    .multiply(BigDecimal.valueOf(days))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        BigDecimal foodCostInr = BigDecimal.valueOf(getDailyFoodRate(destination) * travelers * days)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal transportCostInr = BigDecimal.valueOf(getDailyTransportRate(destination) * travelers * days)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal attractionCostInr = calculateAttractionCost(request.getItinerary(), travelers);

        BigDecimal subtotalInr = hotelCostInr.add(foodCostInr).add(transportCostInr).add(attractionCostInr);
        BigDecimal miscCostInr = subtotalInr.multiply(BigDecimal.valueOf(0.05))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalInr = subtotalInr.add(miscCostInr).setScale(2, RoundingMode.HALF_UP);

        // ── Step 2: Convert to target currency if needed ──────────────────────
        FxResult fx = convertToTargetCurrency(totalInr, targetCurrency);

        BigDecimal conversionRate = fx.rate;
        boolean fxFallback = fx.fallbackUsed;
        String fxProvider = fx.provider;
        List<String> warnings = new ArrayList<>();

        if (fxFallback && !BASE_CURRENCY.equals(targetCurrency)) {
            warnings.add("FX rate unavailable — amounts shown in " + BASE_CURRENCY
                    + " (target: " + targetCurrency + ")");
            // Fall back to INR display
            targetCurrency = BASE_CURRENCY;
            conversionRate = BigDecimal.ONE;
        }

        // Apply conversion rate to all line items
        BigDecimal hotelCost     = applyRate(hotelCostInr, conversionRate);
        BigDecimal foodCost      = applyRate(foodCostInr, conversionRate);
        BigDecimal transportCost = applyRate(transportCostInr, conversionRate);
        BigDecimal attractionCost= applyRate(attractionCostInr, conversionRate);
        BigDecimal miscCost      = applyRate(miscCostInr, conversionRate);
        BigDecimal totalEstimated= applyRate(totalInr, conversionRate);

        // Total budget in target currency
        BigDecimal totalBudget = request.getTotalBudget() != null
                ? request.getTotalBudget() : totalEstimated;
        boolean overBudget = totalEstimated.compareTo(totalBudget) > 0;
        BigDecimal remaining = totalBudget.subtract(totalEstimated).setScale(2, RoundingMode.HALF_UP);

        // ── Step 3: Persist ───────────────────────────────────────────────────
        BudgetBreakdown breakdown = BudgetBreakdown.builder()
                .tripId(request.getTripId())
                .hotelCost(hotelCost)
                .foodCost(foodCost)
                .transportCost(transportCost)
                .attractionCost(attractionCost)
                .miscCost(miscCost)
                .totalEstimated(totalEstimated)
                .totalBudget(totalBudget)
                .overBudget(overBudget)
                .currencyCode(targetCurrency)
                .exchangeRateUsed(conversionRate.compareTo(BigDecimal.ONE) == 0 ? null : conversionRate)
                .fxSourceProvider(fxProvider)
                .fxFallbackUsed(fxFallback)
                .build();

        breakdownRepository.findByTripId(request.getTripId())
                .ifPresent(existing -> breakdown.setId(existing.getId()));
        breakdownRepository.save(breakdown);

        return BudgetBreakdownDto.builder()
                .tripId(request.getTripId())
                .hotelCost(hotelCost)
                .foodCost(foodCost)
                .transportCost(transportCost)
                .attractionCost(attractionCost)
                .miscCost(miscCost)
                .totalEstimated(totalEstimated)
                .totalBudget(totalBudget)
                .remainingBudget(remaining)
                .overBudget(overBudget)
                .currencyCode(targetCurrency)
                .exchangeRateUsed(conversionRate.compareTo(BigDecimal.ONE) == 0 ? null : conversionRate)
                .fxSourceProvider(fxProvider)
                .fxFallbackUsed(fxFallback)
                .warnings(warnings.isEmpty() ? null : warnings)
                .build();
    }

    public BudgetBreakdownDto getByTripId(Long tripId) {
        return breakdownRepository.findByTripId(tripId)
                .map(b -> BudgetBreakdownDto.builder()
                        .tripId(b.getTripId())
                        .hotelCost(b.getHotelCost())
                        .foodCost(b.getFoodCost())
                        .transportCost(b.getTransportCost())
                        .attractionCost(b.getAttractionCost())
                        .miscCost(b.getMiscCost())
                        .totalEstimated(b.getTotalEstimated())
                        .totalBudget(b.getTotalBudget())
                        .remainingBudget(b.getTotalBudget().subtract(b.getTotalEstimated()))
                        .overBudget(b.isOverBudget())
                        .currencyCode(b.getCurrencyCode() != null ? b.getCurrencyCode() : BASE_CURRENCY)
                        .exchangeRateUsed(b.getExchangeRateUsed())
                        .fxSourceProvider(b.getFxSourceProvider())
                        .fxFallbackUsed(b.isFxFallbackUsed())
                        .build())
                .orElseThrow(() -> new RuntimeException("Budget not found for trip: " + tripId));
    }

    // ── FX conversion ─────────────────────────────────────────────────────────

    private static class FxResult {
        BigDecimal rate = BigDecimal.ONE;
        boolean fallbackUsed = false;
        String provider = "none";
    }

    private FxResult convertToTargetCurrency(BigDecimal amountInr, String targetCurrency) {
        FxResult result = new FxResult();
        if (BASE_CURRENCY.equals(targetCurrency)) return result;

        try {
            FxRateResponse response = fxServiceClient.getRate(BASE_CURRENCY, targetCurrency);
            if (response != null && response.getData() != null
                    && response.getData().getRate() != null) {
                result.rate = response.getData().getRate();
                result.fallbackUsed = response.isFallbackUsed() || response.getData().isFallbackRate();
                result.provider = response.getSourceProvider() != null
                        ? response.getSourceProvider() : "frankfurter";
                log.info("FX rate {}/{} = {} (provider={}, fallback={})",
                        BASE_CURRENCY, targetCurrency, result.rate, result.provider, result.fallbackUsed);
            } else {
                log.warn("FX response empty for {}/{} — using INR fallback", BASE_CURRENCY, targetCurrency);
                result.fallbackUsed = true;
            }
        } catch (Exception e) {
            log.warn("FX call failed for {}/{}: {} — using INR fallback",
                    BASE_CURRENCY, targetCurrency, e.getMessage());
            result.fallbackUsed = true;
        }
        return result;
    }

    private BigDecimal applyRate(BigDecimal amount, BigDecimal rate) {
        if (rate == null || rate.compareTo(BigDecimal.ONE) == 0) return amount;
        return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
    }

    // ── Base rate helpers (INR) ───────────────────────────────────────────────

    private double getDailyFoodRate(String destination) {
        return switch (destination.toLowerCase()) {
            case "bangalore" -> 800.0;
            case "goa", "manali" -> 600.0;
            default -> 400.0;
        };
    }

    private double getDailyTransportRate(String destination) {
        return switch (destination.toLowerCase()) {
            case "bangalore" -> 500.0;
            case "goa" -> 400.0;
            case "manali" -> 600.0;
            default -> 300.0;
        };
    }

    @SuppressWarnings("unchecked")
    private BigDecimal calculateAttractionCost(List<Map<String, Object>> itinerary, int travelers) {
        if (itinerary == null || itinerary.isEmpty()) return BigDecimal.ZERO;
        double total = 0.0;
        for (Map<String, Object> day : itinerary) {
            List<Map<String, Object>> places = (List<Map<String, Object>>) day.get("places");
            if (places == null) continue;
            for (Map<String, Object> place : places) {
                Object cost = place.get("ticketCost");
                if (cost != null) total += ((Number) cost).doubleValue();
            }
        }
        return BigDecimal.valueOf(total * travelers).setScale(2, RoundingMode.HALF_UP);
    }

    @SuppressWarnings("unchecked")
    private BudgetCalculateRequest parseRequest(Map<String, Object> map) {
        BudgetCalculateRequest req = new BudgetCalculateRequest();
        req.setTripId(map.get("tripId") != null ? ((Number) map.get("tripId")).longValue() : null);
        req.setDestination((String) map.get("destination"));
        req.setDurationDays(map.get("durationDays") != null ? ((Number) map.get("durationDays")).intValue() : 1);
        req.setTravelers(map.get("travelers") != null ? ((Number) map.get("travelers")).intValue() : 1);
        req.setTotalBudget(map.get("totalBudget") != null
                ? new BigDecimal(map.get("totalBudget").toString()) : null);
        req.setHotelPricePerNight(map.get("hotelPricePerNight") != null
                ? new BigDecimal(map.get("hotelPricePerNight").toString()) : null);
        req.setHotelCategory((String) map.get("hotelCategory"));
        req.setItinerary((List<Map<String, Object>>) map.get("itinerary"));
        // Phase 9E: currency
        req.setCurrencyCode((String) map.getOrDefault("currencyCode", "INR"));
        return req;
    }
}
