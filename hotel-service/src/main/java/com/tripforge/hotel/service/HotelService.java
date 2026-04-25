package com.tripforge.hotel.service;

import com.tripforge.hotel.client.MlServiceClient;
import com.tripforge.hotel.dto.HotelChangeRequest;
import com.tripforge.hotel.dto.HotelDto;
import com.tripforge.hotel.entity.Hotel;
import com.tripforge.hotel.exception.ResourceNotFoundException;
import com.tripforge.hotel.repository.HotelRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Hotel recommendation service.
 *
 * Recommendation pipeline:
 *   1. Rule-based filtering (destination, budget, category)
 *   2. ML ranking via ml-service (with graceful fallback)
 *   3. Return top-N ranked hotels
 */
@Service
public class HotelService {

    private static final Logger log = LoggerFactory.getLogger(HotelService.class);
    private static final int MAX_RECOMMENDATIONS = 5;

    @Autowired private HotelRepository hotelRepository;
    @Autowired private MlServiceClient mlServiceClient;

    /**
     * Returns ranked hotel recommendations for a destination and budget.
     */
    public List<HotelDto> recommendHotels(String destination, BigDecimal budget,
                                           Integer durationDays, Integer travelers,
                                           String hotelPreference) {
        log.info("Recommending hotels for {} | budget={} | days={} | pref={}",
                destination, budget, durationDays, hotelPreference);

        // Step 1: Rule-based filter
        List<Hotel> candidates = filterCandidates(destination, budget, durationDays, hotelPreference);

        if (candidates.isEmpty()) {
            log.warn("No hotels found for destination: {}", destination);
            return List.of();
        }

        // Step 2: ML ranking (with fallback to rule-based score)
        List<HotelDto> ranked = rankWithMl(candidates, destination, budget, durationDays, travelers, hotelPreference);

        return ranked.stream().limit(MAX_RECOMMENDATIONS).collect(Collectors.toList());
    }

    /**
     * Returns alternative hotels based on a change reason.
     * Re-ranks using ML with adjusted weights for the given reason.
     */
    public List<HotelDto> changeHotel(HotelChangeRequest request) {
        log.info("Hotel change requested for trip {} | reason={}", request.getTripId(), request.getReason());

        List<Hotel> allHotels = hotelRepository.findByDestinationIgnoreCase(request.getDestination());

        // Exclude the current hotel
        List<Hotel> candidates = allHotels.stream()
                .filter(h -> !h.getId().equals(request.getCurrentHotelId()))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) return List.of();

        // Apply reason-based pre-filter
        candidates = applyReasonFilter(candidates, request.getReason(), request.getCurrentHotelId());

        // ML re-ranking with feedback reason
        return reRankWithFeedback(candidates, request);
    }

    /**
     * Returns alternative hotels for a trip (already saved).
     */
    public List<HotelDto> getAlternatives(Long tripId, String destination, Long excludeHotelId) {
        List<Hotel> hotels = hotelRepository.findByDestinationIgnoreCase(destination)
                .stream()
                .filter(h -> !h.getId().equals(excludeHotelId))
                .collect(Collectors.toList());

        return hotels.stream()
                .sorted(Comparator.comparingDouble(Hotel::getRating).reversed())
                .limit(4)
                .map(h -> mapToDto(h, computeRuleScore(h)))
                .collect(Collectors.toList());
    }

    public HotelDto getHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + id));
        return mapToDto(hotel, computeRuleScore(hotel));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<Hotel> filterCandidates(String destination, BigDecimal budget,
                                          Integer durationDays, String hotelPreference) {
        // Budget per night = total budget * 30% / duration
        double budgetPerNight = budget.doubleValue() * 0.30 / Math.max(durationDays, 1);

        List<Hotel> byDestination = hotelRepository.findByDestinationIgnoreCase(destination);

        // Filter by category preference
        List<Hotel> byCategory = byDestination.stream()
                .filter(h -> matchesPreference(h.getCategory(), hotelPreference))
                .collect(Collectors.toList());

        // If category filter returns nothing, fall back to all hotels in destination
        List<Hotel> pool = byCategory.isEmpty() ? byDestination : byCategory;

        // Filter by price (allow 20% over budget per night)
        List<Hotel> affordable = pool.stream()
                .filter(h -> h.getPricePerNight() <= budgetPerNight * 1.2)
                .collect(Collectors.toList());

        return affordable.isEmpty() ? pool : affordable;
    }

    private boolean matchesPreference(String category, String preference) {
        if (preference == null) return true;
        return switch (preference.toUpperCase()) {
            case "BUDGET" -> "BUDGET".equals(category);
            case "LUXURY" -> "LUXURY".equals(category);
            default -> true; // STANDARD accepts all
        };
    }

    private List<HotelDto> rankWithMl(List<Hotel> candidates, String destination,
                                       BigDecimal budget, Integer durationDays,
                                       Integer travelers, String preference) {
        try {
            // Build ML request
            List<Map<String, Object>> hotelFeatures = candidates.stream()
                    .map(h -> {
                        Map<String, Object> f = new HashMap<>();
                        f.put("hotel_id", h.getId());
                        f.put("price_per_night", h.getPricePerNight());
                        f.put("rating", h.getRating());
                        f.put("distance_from_center_km", h.getDistanceFromCenterKm());
                        f.put("category", h.getCategory());
                        f.put("popularity_score", h.getPopularityScore());
                        return f;
                    }).collect(Collectors.toList());

            Map<String, Object> mlRequest = new HashMap<>();
            mlRequest.put("destination", destination);
            mlRequest.put("budget", budget);
            mlRequest.put("duration_days", durationDays);
            mlRequest.put("travelers", travelers);
            mlRequest.put("hotel_preference", preference);
            mlRequest.put("hotels", hotelFeatures);

            Map<String, Object> mlResponse = mlServiceClient.rankHotels(mlRequest);

            // Parse ML scores
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rankedHotels = (List<Map<String, Object>>) mlResponse.get("ranked_hotels");

            if (rankedHotels != null) {
                Map<Long, Double> scoreMap = new HashMap<>();
                for (Map<String, Object> rh : rankedHotels) {
                    Long hotelId = ((Number) rh.get("hotel_id")).longValue();
                    Double score = ((Number) rh.get("relevance_score")).doubleValue();
                    scoreMap.put(hotelId, score);
                }

                return candidates.stream()
                        .sorted(Comparator.comparingDouble(
                                h -> -scoreMap.getOrDefault(h.getId(), computeRuleScore(h))))
                        .map(h -> mapToDto(h, scoreMap.getOrDefault(h.getId(), computeRuleScore(h))))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("ML ranking failed, falling back to rule-based: {}", e.getMessage());
        }

        // Fallback: rule-based scoring
        return candidates.stream()
                .sorted(Comparator.comparingDouble(h -> -computeRuleScore(h)))
                .map(h -> mapToDto(h, computeRuleScore(h)))
                .collect(Collectors.toList());
    }

    private List<HotelDto> reRankWithFeedback(List<Hotel> candidates, HotelChangeRequest request) {
        try {
            List<Map<String, Object>> hotelFeatures = candidates.stream()
                    .map(h -> {
                        Map<String, Object> f = new HashMap<>();
                        f.put("hotel_id", h.getId());
                        f.put("price_per_night", h.getPricePerNight());
                        f.put("rating", h.getRating());
                        f.put("distance_from_center_km", h.getDistanceFromCenterKm());
                        f.put("category", h.getCategory());
                        f.put("popularity_score", h.getPopularityScore());
                        return f;
                    }).collect(Collectors.toList());

            Map<String, Object> mlRequest = new HashMap<>();
            mlRequest.put("feedback_reason", request.getReason());
            mlRequest.put("hotels", hotelFeatures);

            Map<String, Object> mlResponse = mlServiceClient.recommendAlternative(mlRequest);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> ranked = (List<Map<String, Object>>) mlResponse.get("ranked_hotels");

            if (ranked != null) {
                Map<Long, Double> scoreMap = new HashMap<>();
                for (Map<String, Object> rh : ranked) {
                    scoreMap.put(((Number) rh.get("hotel_id")).longValue(),
                            ((Number) rh.get("relevance_score")).doubleValue());
                }
                return candidates.stream()
                        .sorted(Comparator.comparingDouble(h -> -scoreMap.getOrDefault(h.getId(), 0.0)))
                        .map(h -> mapToDto(h, scoreMap.getOrDefault(h.getId(), 0.0)))
                        .limit(4)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("ML re-ranking failed, using rule-based fallback: {}", e.getMessage());
        }

        // Fallback: sort by reason
        return applyReasonSort(candidates, request.getReason()).stream()
                .map(h -> mapToDto(h, computeRuleScore(h)))
                .limit(4)
                .collect(Collectors.toList());
    }

    private List<Hotel> applyReasonFilter(List<Hotel> hotels, String reason, Long currentHotelId) {
        Hotel current = hotelRepository.findById(currentHotelId).orElse(null);
        if (current == null) return hotels;

        return switch (reason.toUpperCase()) {
            case "CHEAPER" -> hotels.stream()
                    .filter(h -> h.getPricePerNight() < current.getPricePerNight())
                    .collect(Collectors.toList());
            case "BETTER_RATING" -> hotels.stream()
                    .filter(h -> h.getRating() > current.getRating())
                    .collect(Collectors.toList());
            case "CLOSER" -> hotels.stream()
                    .filter(h -> h.getDistanceFromCenterKm() < current.getDistanceFromCenterKm())
                    .collect(Collectors.toList());
            case "PREMIUM" -> hotels.stream()
                    .filter(h -> "LUXURY".equals(h.getCategory()))
                    .collect(Collectors.toList());
            default -> hotels;
        };
    }

    private List<Hotel> applyReasonSort(List<Hotel> hotels, String reason) {
        return switch (reason.toUpperCase()) {
            case "CHEAPER" -> hotels.stream()
                    .sorted(Comparator.comparingDouble(Hotel::getPricePerNight))
                    .collect(Collectors.toList());
            case "BETTER_RATING" -> hotels.stream()
                    .sorted(Comparator.comparingDouble(Hotel::getRating).reversed())
                    .collect(Collectors.toList());
            case "CLOSER" -> hotels.stream()
                    .sorted(Comparator.comparingDouble(Hotel::getDistanceFromCenterKm))
                    .collect(Collectors.toList());
            case "PREMIUM" -> hotels.stream()
                    .sorted(Comparator.comparingDouble(Hotel::getPricePerNight).reversed())
                    .collect(Collectors.toList());
            default -> hotels;
        };
    }

    /**
     * Simple rule-based relevance score (0-1).
     * Combines rating, popularity, and inverse distance.
     */
    private double computeRuleScore(Hotel h) {
        double ratingScore = h.getRating() / 5.0;
        double popularityScore = h.getPopularityScore() / 10.0;
        double distanceScore = 1.0 / (1.0 + h.getDistanceFromCenterKm());
        return (ratingScore * 0.4) + (popularityScore * 0.35) + (distanceScore * 0.25);
    }

    private HotelDto mapToDto(Hotel h, double relevanceScore) {
        List<String> amenitiesList = (h.getAmenities() != null && !h.getAmenities().isBlank())
                ? Arrays.asList(h.getAmenities().split(","))
                : List.of();

        return HotelDto.builder()
                .id(h.getId())
                .name(h.getName())
                .destination(h.getDestination())
                .pricePerNight(h.getPricePerNight())
                .rating(h.getRating())
                .distanceFromCenterKm(h.getDistanceFromCenterKm())
                .amenities(amenitiesList)
                .category(h.getCategory())
                .popularityScore(h.getPopularityScore())
                .relevanceScore(Math.round(relevanceScore * 1000.0) / 1000.0)
                .build();
    }
}
