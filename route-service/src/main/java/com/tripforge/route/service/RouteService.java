package com.tripforge.route.service;

import com.tripforge.route.dto.DayPlanDto;
import com.tripforge.route.dto.RouteOptimizeRequest;
import com.tripforge.route.entity.Attraction;
import com.tripforge.route.repository.AttractionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Route optimization service.
 *
 * Algorithm:
 *   1. Filter attractions by destination and user interests
 *   2. Score each attraction (interest match + priority)
 *   3. Group by distance cluster (A/B/C) to minimize travel
 *   4. Assign ~3-4 attractions per day using a greedy nearest-neighbor approach
 *   5. Assign visit times (morning / afternoon / evening)
 */
@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);

    // Attractions per day target
    private static final int ATTRACTIONS_PER_DAY = 3;

    // Visit time slots
    private static final String[] TIME_SLOTS = {"09:00 AM", "12:30 PM", "04:00 PM", "07:00 PM"};

    @Autowired
    private AttractionRepository attractionRepository;

    /**
     * Generates a day-wise itinerary for the given request.
     */
    public List<DayPlanDto> optimizeRoute(RouteOptimizeRequest request) {
        log.info("Optimizing route for {} | {} days | interests: {}",
                request.getDestination(), request.getDurationDays(), request.getInterests());

        List<Attraction> allAttractions = attractionRepository
                .findByDestinationOrderByPriority(request.getDestination());

        if (allAttractions.isEmpty()) {
            log.warn("No attractions found for destination: {}", request.getDestination());
            return List.of();
        }

        // Score and filter by interests
        List<Attraction> scored = scoreAndFilter(allAttractions, request.getInterests());

        // Distribute across days
        return buildDayPlans(scored, request.getStartDate(), request.getDurationDays());
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<Attraction> scoreAndFilter(List<Attraction> attractions, List<String> interests) {
        if (interests == null || interests.isEmpty()) {
            return attractions;
        }

        // Score each attraction by how many interests it matches
        return attractions.stream()
                .sorted(Comparator.comparingDouble(a -> -computeInterestScore(a, interests)))
                .collect(Collectors.toList());
    }

    private double computeInterestScore(Attraction a, List<String> interests) {
        if (a.getSuitableForInterests() == null) return a.getPriorityScore();

        List<String> suitable = Arrays.asList(a.getSuitableForInterests().toLowerCase().split(","));
        long matches = interests.stream()
                .filter(i -> suitable.contains(i.toLowerCase()))
                .count();

        // Weighted: interest match (60%) + base priority (40%)
        return (matches * 2.0 * 0.6) + (a.getPriorityScore() / 10.0 * 0.4);
    }

    private List<DayPlanDto> buildDayPlans(List<Attraction> attractions,
                                            LocalDate startDate, int durationDays) {
        List<DayPlanDto> days = new ArrayList<>();

        // Group by cluster for geographic coherence
        Map<String, List<Attraction>> byCluster = attractions.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getDistanceCluster() != null ? a.getDistanceCluster() : "A"));

        // Flatten in cluster order: A → B → C (nearby first)
        List<Attraction> ordered = new ArrayList<>();
        for (String cluster : List.of("A", "B", "C")) {
            if (byCluster.containsKey(cluster)) {
                ordered.addAll(byCluster.get(cluster));
            }
        }

        // Assign to days
        int attractionIndex = 0;
        for (int day = 1; day <= durationDays; day++) {
            LocalDate date = startDate.plusDays(day - 1);
            List<DayPlanDto.PlaceDto> places = new ArrayList<>();

            int count = 0;
            while (count < ATTRACTIONS_PER_DAY && attractionIndex < ordered.size()) {
                Attraction a = ordered.get(attractionIndex++);
                places.add(DayPlanDto.PlaceDto.builder()
                        .attractionId(a.getId())
                        .name(a.getName())
                        .category(a.getCategory())
                        .visitTime(TIME_SLOTS[count % TIME_SLOTS.length])
                        .avgVisitHours(a.getAvgVisitHours())
                        .ticketCost(BigDecimal.valueOf(a.getTicketCost()))
                        .notes(buildNotes(a))
                        .visitOrder(count + 1)
                        .build());
                count++;
            }

            // If we ran out of attractions, cycle back for longer trips
            if (places.isEmpty() && !ordered.isEmpty()) {
                attractionIndex = 0;
                Attraction a = ordered.get(attractionIndex++);
                places.add(DayPlanDto.PlaceDto.builder()
                        .attractionId(a.getId())
                        .name(a.getName())
                        .category(a.getCategory())
                        .visitTime(TIME_SLOTS[0])
                        .avgVisitHours(a.getAvgVisitHours())
                        .ticketCost(BigDecimal.valueOf(a.getTicketCost()))
                        .visitOrder(1)
                        .build());
            }

            days.add(DayPlanDto.builder()
                    .dayNumber(day)
                    .date(date)
                    .theme(buildTheme(places))
                    .places(places)
                    .build());
        }

        return days;
    }

    private String buildTheme(List<DayPlanDto.PlaceDto> places) {
        if (places.isEmpty()) return "Exploration Day";
        String primaryCategory = places.get(0).getCategory();
        return switch (primaryCategory.toLowerCase()) {
            case "nature" -> "Nature & Outdoors";
            case "temple" -> "Culture & Heritage";
            case "beach" -> "Beach & Relaxation";
            case "adventure" -> "Adventure & Thrills";
            case "food" -> "Food & Local Flavours";
            case "nightlife" -> "Nightlife & Entertainment";
            case "shopping" -> "Shopping & Markets";
            default -> "City Exploration";
        };
    }

    private String buildNotes(Attraction a) {
        return String.format("Avg visit: %.1f hrs | Category: %s", a.getAvgVisitHours(), a.getCategory());
    }
}
