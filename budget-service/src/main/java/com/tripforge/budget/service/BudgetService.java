package com.tripforge.budget.service;

import com.tripforge.budget.dto.BudgetBreakdownDto;
import com.tripforge.budget.dto.BudgetCalculateRequest;
import com.tripforge.budget.entity.BudgetBreakdown;
import com.tripforge.budget.repository.BudgetBreakdownRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Budget calculation service.
 *
 * Cost estimation model (per trip):
 *   Hotel cost     = hotelPricePerNight × durationDays
 *   Food cost      = dailyFoodRate × travelers × durationDays
 *   Transport cost = dailyTransportRate × travelers × durationDays
 *   Attraction cost= sum of ticket costs from itinerary × travelers
 *   Misc cost      = 5% of (hotel + food + transport + attraction)
 *
 * Daily food rates by destination tier:
 *   Metro (Bangalore): ₹800/person/day
 *   Tourist (Goa, Manali): ₹600/person/day
 *   Budget (Mysore, Ooty): ₹400/person/day
 */
@Service
public class BudgetService {

    private static final Logger log = LoggerFactory.getLogger(BudgetService.class);

    @Autowired
    private BudgetBreakdownRepository breakdownRepository;

    @Transactional
    public BudgetBreakdownDto calculate(Map<String, Object> requestMap) {
        BudgetCalculateRequest request = parseRequest(requestMap);
        log.info("Calculating budget for trip {} | {} days | {} travelers",
                request.getTripId(), request.getDurationDays(), request.getTravelers());

        int days = request.getDurationDays() != null ? request.getDurationDays() : 1;
        int travelers = request.getTravelers() != null ? request.getTravelers() : 1;
        String destination = request.getDestination() != null ? request.getDestination() : "";

        // Hotel cost
        BigDecimal hotelCost = BigDecimal.ZERO;
        if (request.getHotelPricePerNight() != null) {
            hotelCost = request.getHotelPricePerNight()
                    .multiply(BigDecimal.valueOf(days))
                    .setScale(2, RoundingMode.HALF_UP);
        }

        // Food cost
        double dailyFoodRate = getDailyFoodRate(destination);
        BigDecimal foodCost = BigDecimal.valueOf(dailyFoodRate * travelers * days)
                .setScale(2, RoundingMode.HALF_UP);

        // Transport cost
        double dailyTransportRate = getDailyTransportRate(destination);
        BigDecimal transportCost = BigDecimal.valueOf(dailyTransportRate * travelers * days)
                .setScale(2, RoundingMode.HALF_UP);

        // Attraction cost from itinerary
        BigDecimal attractionCost = calculateAttractionCost(request.getItinerary(), travelers);

        // Misc = 5% of subtotal
        BigDecimal subtotal = hotelCost.add(foodCost).add(transportCost).add(attractionCost);
        BigDecimal miscCost = subtotal.multiply(BigDecimal.valueOf(0.05))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalEstimated = subtotal.add(miscCost).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalBudget = request.getTotalBudget() != null
                ? request.getTotalBudget() : totalEstimated;
        boolean overBudget = totalEstimated.compareTo(totalBudget) > 0;

        // Persist
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
                .build();

        // Upsert
        breakdownRepository.findByTripId(request.getTripId())
                .ifPresent(existing -> breakdown.setId(existing.getId()));
        breakdownRepository.save(breakdown);

        BigDecimal remaining = totalBudget.subtract(totalEstimated).setScale(2, RoundingMode.HALF_UP);

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
                        .build())
                .orElseThrow(() -> new RuntimeException("Budget not found for trip: " + tripId));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private double getDailyFoodRate(String destination) {
        return switch (destination.toLowerCase()) {
            case "bangalore" -> 800.0;
            case "goa", "manali" -> 600.0;
            default -> 400.0; // Mysore, Ooty
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
                if (cost != null) {
                    total += ((Number) cost).doubleValue();
                }
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
        return req;
    }
}
