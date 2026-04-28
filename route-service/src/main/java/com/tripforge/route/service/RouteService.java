package com.tripforge.route.service;

import com.tripforge.route.client.ExternalDataServiceClient;
import com.tripforge.route.dto.DayPlanDto;
import com.tripforge.route.dto.ExternalRouteResponse;
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
 * Route optimization service — Phase 9C upgrade.
 *
 * Pipeline:
 *   1. If user-selected place IDs with coordinates are available:
 *      → call external-data-service for real travel times + optimized order
 *   2. If external route optimization succeeds:
 *      → use returned stop order and travel times
 *   3. If external route fails or no coordinates available:
 *      → fall back to existing CSV + cluster-based heuristic (unchanged behavior)
 *
 * Zero-key fallback: if external-data-service is unreachable,
 * the service behaves exactly as before Phase 9C.
 */
@Service
public class RouteService {

    private static final Logger log = LoggerFactory.getLogger(RouteService.class);
    private static final int ATTRACTIONS_PER_DAY = 3;
    private static final String[] TIME_SLOTS = {"09:00 AM", "12:30 PM", "04:00 PM", "07:00 PM"};

    @Autowired private AttractionRepository attractionRepository;
    @Autowired private ExternalDataServiceClient externalDataServiceClient;

    /**
     * Generates a day-wise itinerary.
     * Tries live route optimization first, falls back to CSV heuristic,
     * then synthetic fallback. NEVER returns an empty list.
     */
    public List<DayPlanDto> optimizeRoute(RouteOptimizeRequest request) {
        log.info("Optimizing route for {} | {} days | interests: {}",
                request.getDestination(), request.getDurationDays(), request.getInterests());

        // ── Try live route optimization if place coordinates are available ────
        if (hasLivePlaces(request)) {
            List<DayPlanDto> liveItinerary = tryLiveRouteOptimization(request);
            if (liveItinerary != null && !liveItinerary.isEmpty()) {
                log.info("Using live route optimization for '{}'", request.getDestination());
                return liveItinerary;
            }
        }

        // ── Fall back to CSV + heuristic (existing behavior) ─────────────────
        log.info("Using CSV heuristic route for '{}'", request.getDestination());
        List<DayPlanDto> csvItinerary = buildCsvItinerary(request);
        if (!csvItinerary.isEmpty()) {
            return csvItinerary;
        }

        // ── Synthetic fallback — always returns a non-empty itinerary ─────────
        log.warn("No CSV attractions for '{}' — generating synthetic itinerary", request.getDestination());
        return buildSyntheticItinerary(request);
    }

    // ── Live route path ───────────────────────────────────────────────────────

    private boolean hasLivePlaces(RouteOptimizeRequest request) {
        return request.getLivePlaces() != null && !request.getLivePlaces().isEmpty()
                && request.getLivePlaces().stream()
                        .allMatch(p -> p.getLat() != null && p.getLng() != null);
    }

    private List<DayPlanDto> tryLiveRouteOptimization(RouteOptimizeRequest request) {
        try {
            List<RouteOptimizeRequest.LivePlace> places = request.getLivePlaces();
            int durationDays = request.getDurationDays() != null ? request.getDurationDays() : 1;
            int placesPerDay = ATTRACTIONS_PER_DAY;

            // Split places across days
            List<List<RouteOptimizeRequest.LivePlace>> dayGroups = splitIntoDays(places, durationDays, placesPerDay);

            List<DayPlanDto> itinerary = new ArrayList<>();
            for (int dayIdx = 0; dayIdx < dayGroups.size(); dayIdx++) {
                List<RouteOptimizeRequest.LivePlace> dayPlaces = dayGroups.get(dayIdx);
                if (dayPlaces.isEmpty()) continue;

                LocalDate date = request.getStartDate() != null
                        ? request.getStartDate().plusDays(dayIdx) : LocalDate.now().plusDays(dayIdx);

                // Build waypoints for external-data-service
                final int currentDayIdx = dayIdx;
                List<Map<String, Object>> waypoints = dayPlaces.stream().map(p -> {
                    Map<String, Object> wp = new HashMap<>();
                    wp.put("placeId", p.getPlaceId() != null ? p.getPlaceId() : "place_" + currentDayIdx);
                    wp.put("name", p.getName());
                    wp.put("lat", p.getLat());
                    wp.put("lng", p.getLng());
                    wp.put("estimatedVisitMinutes", p.getEstimatedVisitMinutes() != null
                            ? p.getEstimatedVisitMinutes() : 90);
                    return wp;
                }).collect(Collectors.toList());

                Map<String, Object> routeRequest = new HashMap<>();
                routeRequest.put("waypoints", waypoints);
                routeRequest.put("travelMode", "DRIVING");
                routeRequest.put("optimizeFor", "TIME");

                ExternalRouteResponse routeResponse = externalDataServiceClient.optimizeRoute(routeRequest);

                if (routeResponse == null || routeResponse.getData() == null
                        || routeResponse.getData().getStops() == null) {
                    return null;  // trigger fallback
                }

                List<DayPlanDto.PlaceDto> dayPlan = buildPlacesFromRouteStops(
                        routeResponse.getData().getStops(), dayPlaces);

                String theme = buildThemeFromPlaces(dayPlaces);
                itinerary.add(DayPlanDto.builder()
                        .dayNumber(dayIdx + 1)
                        .date(date)
                        .theme(theme)
                        .places(dayPlan)
                        .sourceProvider(routeResponse.getSourceProvider())
                        .fallbackUsed(routeResponse.isFallbackUsed())
                        .build());
            }

            return itinerary.isEmpty() ? null : itinerary;

        } catch (Exception e) {
            log.warn("Live route optimization failed: {} — using CSV fallback", e.getMessage());
            return null;
        }
    }

    private List<DayPlanDto.PlaceDto> buildPlacesFromRouteStops(
            List<ExternalRouteResponse.RouteStop> stops,
            List<RouteOptimizeRequest.LivePlace> originalPlaces) {

        List<DayPlanDto.PlaceDto> places = new ArrayList<>();
        for (int i = 0; i < stops.size(); i++) {
            ExternalRouteResponse.RouteStop stop = stops.get(i);
            String visitTime = TIME_SLOTS[Math.min(i, TIME_SLOTS.length - 1)];

            // Find matching original place for category/notes
            RouteOptimizeRequest.LivePlace original = originalPlaces.stream()
                    .filter(p -> stop.getName() != null && stop.getName().equals(p.getName()))
                    .findFirst()
                    .orElse(i < originalPlaces.size() ? originalPlaces.get(i) : null);

            double avgHours = stop.getEstimatedVisitMinutes() != null
                    ? stop.getEstimatedVisitMinutes() / 60.0 : 1.5;

            String notes = stop.getTravelTimeFromPreviousMinutes() != null && stop.getTravelTimeFromPreviousMinutes() > 0
                    ? String.format("Travel from previous: %d min | Avg visit: %.1f hrs",
                        stop.getTravelTimeFromPreviousMinutes(), avgHours)
                    : String.format("Avg visit: %.1f hrs", avgHours);

            places.add(DayPlanDto.PlaceDto.builder()
                    .attractionId(null)
                    .externalPlaceId(stop.getPlaceId())
                    .name(stop.getName())
                    .category(original != null ? original.getCategory() : "attraction")
                    .visitTime(visitTime)
                    .avgVisitHours(avgHours)
                    .ticketCost(BigDecimal.ZERO)
                    .notes(notes)
                    .visitOrder(i + 1)
                    .lat(stop.getLat())
                    .lng(stop.getLng())
                    .travelTimeFromPreviousMinutes(stop.getTravelTimeFromPreviousMinutes())
                    .build());
        }
        return places;
    }

    private List<List<RouteOptimizeRequest.LivePlace>> splitIntoDays(
            List<RouteOptimizeRequest.LivePlace> places, int durationDays, int perDay) {
        List<List<RouteOptimizeRequest.LivePlace>> groups = new ArrayList<>();
        int idx = 0;
        for (int d = 0; d < durationDays; d++) {
            List<RouteOptimizeRequest.LivePlace> group = new ArrayList<>();
            while (group.size() < perDay && idx < places.size()) {
                group.add(places.get(idx++));
            }
            groups.add(group);
        }
        return groups;
    }

    private String buildThemeFromPlaces(List<RouteOptimizeRequest.LivePlace> places) {
        if (places.isEmpty()) return "Exploration Day";
        String cat = places.get(0).getCategory();
        if (cat == null) return "City Exploration";
        return switch (cat.toLowerCase()) {
            case "nature", "park" -> "Nature & Outdoors";
            case "temple", "religion", "historic" -> "Culture & Heritage";
            case "beach" -> "Beach & Relaxation";
            case "adventure" -> "Adventure & Thrills";
            case "food", "restaurant" -> "Food & Local Flavours";
            case "nightlife", "bar" -> "Nightlife & Entertainment";
            case "shopping" -> "Shopping & Markets";
            default -> "City Exploration";
        };
    }

    // ── CSV fallback path (existing behavior, unchanged) ─────────────────────

    private List<DayPlanDto> buildCsvItinerary(RouteOptimizeRequest request) {
        List<Attraction> allAttractions = attractionRepository
                .findByDestinationOrderByPriority(request.getDestination());

        if (allAttractions.isEmpty()) {
            log.warn("No attractions found for destination: {}", request.getDestination());
            return List.of();
        }

        List<Attraction> scored = scoreAndFilter(allAttractions, request.getInterests());
        return buildDayPlans(scored, request.getStartDate(), request.getDurationDays());
    }

    private List<Attraction> scoreAndFilter(List<Attraction> attractions, List<String> interests) {
        if (interests == null || interests.isEmpty()) return attractions;
        return attractions.stream()
                .sorted(Comparator.comparingDouble(a -> -computeInterestScore(a, interests)))
                .collect(Collectors.toList());
    }

    private double computeInterestScore(Attraction a, List<String> interests) {
        if (a.getSuitableForInterests() == null) return a.getPriorityScore();
        List<String> suitable = Arrays.asList(a.getSuitableForInterests().toLowerCase().split(","));
        long matches = interests.stream().filter(i -> suitable.contains(i.toLowerCase())).count();
        return (matches * 2.0 * 0.6) + (a.getPriorityScore() / 10.0 * 0.4);
    }

    private List<DayPlanDto> buildDayPlans(List<Attraction> attractions,
                                            LocalDate startDate, int durationDays) {
        List<DayPlanDto> days = new ArrayList<>();
        Map<String, List<Attraction>> byCluster = attractions.stream()
                .collect(Collectors.groupingBy(
                        a -> a.getDistanceCluster() != null ? a.getDistanceCluster() : "A"));

        List<Attraction> ordered = new ArrayList<>();
        for (String cluster : List.of("A", "B", "C")) {
            if (byCluster.containsKey(cluster)) ordered.addAll(byCluster.get(cluster));
        }

        int attractionIndex = 0;
        for (int day = 1; day <= durationDays; day++) {
            LocalDate date = startDate != null ? startDate.plusDays(day - 1) : LocalDate.now().plusDays(day - 1);
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
                        .notes(String.format("Avg visit: %.1f hrs | Category: %s",
                                a.getAvgVisitHours(), a.getCategory()))
                        .visitOrder(count + 1)
                        .build());
                count++;
            }

            if (places.isEmpty() && !ordered.isEmpty()) {
                attractionIndex = 0;
                Attraction a = ordered.get(attractionIndex++);
                places.add(DayPlanDto.PlaceDto.builder()
                        .attractionId(a.getId()).name(a.getName()).category(a.getCategory())
                        .visitTime(TIME_SLOTS[0]).avgVisitHours(a.getAvgVisitHours())
                        .ticketCost(BigDecimal.valueOf(a.getTicketCost())).visitOrder(1).build());
            }

            days.add(DayPlanDto.builder()
                    .dayNumber(day).date(date).theme(buildTheme(places)).places(places)
                    .sourceProvider("csv_dataset").fallbackUsed(true)
                    .build());
        }
        return days;
    }

    private String buildTheme(List<DayPlanDto.PlaceDto> places) {
        if (places.isEmpty()) return "Exploration Day";
        String cat = places.get(0).getCategory();
        return switch (cat.toLowerCase()) {
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

    // ── Synthetic itinerary fallback ──────────────────────────────────────────

    /**
     * Generates a complete synthetic itinerary when no CSV data is available.
     * Uses destination + interests + duration to produce realistic day plans.
     * Always returns at least 1 day. Never returns an empty list.
     */
    private List<DayPlanDto> buildSyntheticItinerary(RouteOptimizeRequest request) {
        String dest = request.getDestination() != null ? request.getDestination() : "your destination";
        int days = request.getDurationDays() != null ? Math.max(request.getDurationDays(), 1) : 1;
        LocalDate startDate = request.getStartDate() != null ? request.getStartDate() : LocalDate.now();
        List<String> interests = request.getInterests() != null ? request.getInterests() : List.of();

        // Build a pool of interest-based activity themes
        List<String[]> activityPool = buildActivityPool(dest, interests);

        List<DayPlanDto> itinerary = new ArrayList<>();
        int poolIdx = 0;

        for (int day = 1; day <= days; day++) {
            LocalDate date = startDate.plusDays(day - 1);
            List<DayPlanDto.PlaceDto> places = new ArrayList<>();

            if (day == 1) {
                // Arrival day: light exploration
                places.add(makeSyntheticPlace(1, dest + " City Centre", "sightseeing",
                        "09:00 AM", 2.0, 0.0, "Arrive and check in. Explore the city centre area."));
                places.add(makeSyntheticPlace(2, "Local Market — " + dest, "food",
                        "12:30 PM", 1.5, 0.0, "Lunch at a local market. Try regional specialties."));
                places.add(makeSyntheticPlace(3, dest + " Evening Walk", "leisure",
                        "06:00 PM", 2.0, 0.0, "Evening stroll and dinner at a local restaurant."));
                itinerary.add(DayPlanDto.builder()
                        .dayNumber(day).date(date).theme("Arrival & First Impressions")
                        .places(places).sourceProvider("TRIPFORGE_FALLBACK").fallbackUsed(true)
                        .build());

            } else if (day == days && days > 1) {
                // Departure day: final activity + checkout
                places.add(makeSyntheticPlace(1, "Morning at " + dest, "leisure",
                        "08:00 AM", 1.5, 0.0, "Final morning. Light breakfast and last-minute shopping."));
                places.add(makeSyntheticPlace(2, dest + " Souvenir Market", "shopping",
                        "10:00 AM", 1.0, 0.0, "Pick up souvenirs before checkout."));
                places.add(makeSyntheticPlace(3, "Departure from " + dest, "transit",
                        "12:00 PM", 0.5, 0.0, "Check out and head to airport/station."));
                itinerary.add(DayPlanDto.builder()
                        .dayNumber(day).date(date).theme("Final Day & Departure")
                        .places(places).sourceProvider("TRIPFORGE_FALLBACK").fallbackUsed(true)
                        .build());

            } else {
                // Middle days: rotate through interest-based activities
                String[] morning   = activityPool.get(poolIdx % activityPool.size());
                String[] afternoon = activityPool.get((poolIdx + 1) % activityPool.size());
                String[] evening   = activityPool.get((poolIdx + 2) % activityPool.size());
                poolIdx += 3;

                places.add(makeSyntheticPlace(1, morning[0], morning[1],
                        "09:00 AM", 2.5, 0.0, morning[2]));
                places.add(makeSyntheticPlace(2, afternoon[0], afternoon[1],
                        "01:00 PM", 2.0, 0.0, afternoon[2]));
                places.add(makeSyntheticPlace(3, evening[0], evening[1],
                        "06:00 PM", 2.0, 0.0, evening[2]));

                String theme = deriveTheme(morning[1]);
                itinerary.add(DayPlanDto.builder()
                        .dayNumber(day).date(date).theme(theme)
                        .places(places).sourceProvider("TRIPFORGE_FALLBACK").fallbackUsed(true)
                        .build());
            }
        }

        log.info("Generated synthetic itinerary: {} days for '{}'", itinerary.size(), dest);
        return itinerary;
    }

    private DayPlanDto.PlaceDto makeSyntheticPlace(int order, String name, String category,
                                                    String visitTime, double hours,
                                                    double ticketCost, String notes) {
        return DayPlanDto.PlaceDto.builder()
                .attractionId(null)
                .name(name)
                .category(category)
                .visitTime(visitTime)
                .avgVisitHours(hours)
                .ticketCost(BigDecimal.valueOf(ticketCost))
                .notes(notes)
                .visitOrder(order)
                .build();
    }

    /**
     * Builds a pool of activity suggestions based on destination and interests.
     * Each entry is [name, category, notes].
     */
    private List<String[]> buildActivityPool(String dest, List<String> interests) {
        List<String[]> pool = new ArrayList<>();

        // Interest-specific activities
        for (String interest : interests) {
            switch (interest.toLowerCase()) {
                case "beaches" -> {
                    pool.add(new String[]{dest + " Beach", "beach", "Spend the morning at the beach. Swim, relax, and enjoy the scenery."});
                    pool.add(new String[]{"Waterfront Promenade — " + dest, "beach", "Walk along the waterfront. Great views and photo opportunities."});
                }
                case "food" -> {
                    pool.add(new String[]{"Local Food Tour — " + dest, "food", "Guided food tour through local markets and street food stalls."});
                    pool.add(new String[]{dest + " Restaurant District", "food", "Dinner at a highly-rated local restaurant. Try the regional cuisine."});
                }
                case "temples", "culture" -> {
                    pool.add(new String[]{"Heritage Temple — " + dest, "temple", "Visit a historic temple or religious site. Dress modestly."});
                    pool.add(new String[]{dest + " Cultural Museum", "culture", "Explore local history and culture at the city museum."});
                }
                case "shopping" -> {
                    pool.add(new String[]{dest + " Shopping District", "shopping", "Browse local shops, boutiques, and markets."});
                    pool.add(new String[]{"Central Mall — " + dest, "shopping", "Modern shopping mall with local and international brands."});
                }
                case "nature" -> {
                    pool.add(new String[]{dest + " Nature Park", "nature", "Morning hike or nature walk. Bring water and comfortable shoes."});
                    pool.add(new String[]{"Botanical Garden — " + dest, "nature", "Peaceful garden visit. Great for photography."});
                }
                case "adventure" -> {
                    pool.add(new String[]{"Adventure Sports — " + dest, "adventure", "Try local adventure activities: trekking, water sports, or zip-lining."});
                    pool.add(new String[]{dest + " Outdoor Trail", "adventure", "Scenic outdoor trail. Moderate difficulty, 2–3 hours."});
                }
                case "nightlife" -> {
                    pool.add(new String[]{dest + " Night Market", "nightlife", "Vibrant night market with food, music, and local crafts."});
                    pool.add(new String[]{"Rooftop Bar — " + dest, "nightlife", "Sunset drinks at a rooftop bar with city views."});
                }
            }
        }

        // Always add generic fallbacks so pool is never empty
        pool.add(new String[]{dest + " City Tour", "sightseeing", "Guided city tour covering major landmarks and viewpoints."});
        pool.add(new String[]{"Old Town — " + dest, "sightseeing", "Explore the historic old town area. Architecture and local life."});
        pool.add(new String[]{"Local Café — " + dest, "food", "Breakfast at a popular local café. Try the local coffee or tea."});
        pool.add(new String[]{dest + " Viewpoint", "sightseeing", "Panoramic viewpoint. Best visited in the morning or at sunset."});
        pool.add(new String[]{"Art Gallery — " + dest, "culture", "Contemporary and traditional art from local artists."});
        pool.add(new String[]{dest + " Street Food Walk", "food", "Self-guided street food walk through the city's food lanes."});
        pool.add(new String[]{"Sunset Cruise / Walk — " + dest, "leisure", "Evening leisure activity. Sunset cruise or waterfront walk."});
        pool.add(new String[]{dest + " Spa & Wellness", "leisure", "Afternoon at a local spa or wellness centre. Relax and recharge."});

        // Shuffle for variety across days
        Collections.shuffle(pool);
        return pool;
    }

    private String deriveTheme(String category) {
        return switch (category.toLowerCase()) {
            case "beach" -> "Beach & Coastal Exploration";
            case "food" -> "Food & Local Flavours";
            case "temple", "culture" -> "Culture & Heritage";
            case "shopping" -> "Shopping & Markets";
            case "nature" -> "Nature & Outdoors";
            case "adventure" -> "Adventure & Thrills";
            case "nightlife" -> "Nightlife & Entertainment";
            default -> "City Exploration";
        };
    }
}
