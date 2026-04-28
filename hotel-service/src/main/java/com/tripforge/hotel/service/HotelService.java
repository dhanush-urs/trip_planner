package com.tripforge.hotel.service;

import com.tripforge.hotel.client.ExternalDataServiceClient;
import com.tripforge.hotel.client.MlServiceClient;
import com.tripforge.hotel.dto.ExternalHotelSearchResponse;
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
 * Hotel recommendation service — Phase 9C upgrade.
 *
 * Recommendation pipeline:
 *   1. Try external-data-service for live hotel candidates (Google Places)
 *   2. If live candidates available → run ML ranking on them
 *   3. If live candidates empty / provider down → fall back to CSV dataset (existing behavior)
 *   4. Run ML ranking on CSV candidates
 *   5. Return top-N ranked hotels with provider metadata
 *
 * Zero-key fallback: if external-data-service is unreachable or returns empty,
 * the service behaves exactly as before Phase 9C.
 */
@Service
public class HotelService {

    private static final Logger log = LoggerFactory.getLogger(HotelService.class);
    private static final int MAX_RECOMMENDATIONS = 5;

    @Autowired private HotelRepository hotelRepository;
    @Autowired private MlServiceClient mlServiceClient;
    @Autowired private ExternalDataServiceClient externalDataServiceClient;

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Returns ranked hotel recommendations.
     * Tries live provider first, falls back to CSV dataset, then synthetic fallback.
     * NEVER returns an empty list — always returns at least one hotel.
     */
    public List<HotelDto> recommendHotels(String destination, BigDecimal budget,
                                           Integer durationDays, Integer travelers,
                                           String hotelPreference) {
        return recommendHotels(destination, null, null, budget, durationDays,
                travelers, hotelPreference, "INR");
    }

    /**
     * Returns ranked hotel recommendations with coordinates and currency.
     * Phase 10D: uses lat/lon for Overpass OSM global hotel search.
     */
    public List<HotelDto> recommendHotels(String destination, Double lat, Double lng,
                                           BigDecimal budget, Integer durationDays,
                                           Integer travelers, String hotelPreference,
                                           String currency) {
        log.info("Recommending hotels for {} | lat={} lng={} | budget={} | days={} | pref={} | currency={}",
                destination, lat, lng, budget, durationDays, hotelPreference, currency);

        // ── Step 1: Try live hotel candidates from external-data-service ──────
        List<HotelDto> liveResults = fetchLiveHotels(destination, lat, lng, budget,
                durationDays, travelers, hotelPreference, currency);
        if (!liveResults.isEmpty()) {
            log.info("Using {} live hotel candidates for '{}'", liveResults.size(), destination);
            return liveResults;
        }

        // ── Step 2: Fall back to CSV dataset (existing behavior) ──────────────
        log.info("No live hotels available for '{}' — using CSV fallback", destination);
        List<HotelDto> csvResults = recommendFromCsv(destination, budget, durationDays,
                travelers, hotelPreference);
        if (!csvResults.isEmpty()) {
            return csvResults;
        }

        // ── Step 3: Synthetic fallback — always returns a hotel ───────────────
        log.warn("No CSV hotels for '{}' — generating synthetic fallback hotel", destination);
        return List.of(generateSyntheticHotel(destination, budget, durationDays,
                travelers, hotelPreference, currency));
    }

    /**
     * Returns alternative hotels based on a change reason.
     * Uses CSV dataset (change flow doesn't need live data).
     */
    public List<HotelDto> changeHotel(HotelChangeRequest request) {
        log.info("Hotel change requested for trip {} | reason={}", request.getTripId(), request.getReason());

        List<Hotel> allHotels = hotelRepository.findByDestinationIgnoreCase(request.getDestination());

        List<Hotel> candidates = allHotels.stream()
                .filter(h -> !h.getId().equals(request.getCurrentHotelId()))
                .collect(Collectors.toList());

        if (candidates.isEmpty()) return List.of();

        candidates = applyReasonFilter(candidates, request.getReason(), request.getCurrentHotelId());
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
                .map(h -> mapCsvToDto(h, computeRuleScore(h)))
                .collect(Collectors.toList());
    }

    public HotelDto getHotelById(Long id) {
        Hotel hotel = hotelRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Hotel not found: " + id));
        return mapCsvToDto(hotel, computeRuleScore(hotel));
    }

    // ── Live provider path ────────────────────────────────────────────────────

    /**
     * Fetches live hotel candidates from external-data-service,
     * normalizes them, runs ML ranking, and returns results.
     * Returns empty list on any failure — caller falls back to CSV.
     *
     * Phase 10D: passes lat/lon from destination metadata and correct currency.
     */
    private List<HotelDto> fetchLiveHotels(String destination, BigDecimal budget,
                                             Integer durationDays, Integer travelers,
                                             String hotelPreference) {
        return fetchLiveHotels(destination, null, null, budget, durationDays,
                travelers, hotelPreference, "INR");
    }

    private List<HotelDto> fetchLiveHotels(String destination, Double lat, Double lng,
                                             BigDecimal budget, Integer durationDays,
                                             Integer travelers, String hotelPreference,
                                             String currency) {
        try {
            double budgetPerNight = budget.doubleValue() * 0.30 / Math.max(durationDays, 1);

            ExternalHotelSearchResponse response = externalDataServiceClient.searchHotels(
                    destination, lat, lng, budgetPerNight, currency != null ? currency : "INR");

            if (response == null || response.getData() == null || response.getData().isEmpty()) {
                log.debug("External hotel search returned empty for '{}'", destination);
                return List.of();
            }

            if (response.isDegradedMode()) {
                log.debug("External hotel search degraded for '{}' — using CSV fallback", destination);
                return List.of();
            }

            // Normalize live candidates and run ML ranking
            List<HotelDto> candidates = normalizeLiveCandidates(
                    response.getData(), destination, budget, durationDays,
                    travelers, hotelPreference, response.getSourceProvider());

            if (candidates.isEmpty()) return List.of();

            // Run ML ranking on live candidates
            List<HotelDto> ranked = rankLiveCandidatesWithMl(
                    candidates, destination, budget, durationDays, travelers, hotelPreference);

            return ranked.stream().limit(MAX_RECOMMENDATIONS).collect(Collectors.toList());

        } catch (Exception e) {
            log.warn("Live hotel fetch failed for '{}': {} — falling back to CSV",
                    destination, e.getMessage());
            return List.of();
        }
    }

    /**
     * Normalizes live HotelCandidateDto list into HotelDto list.
     * Applies budget and preference filtering.
     * Sets truthfulness fields: sourceType, priceType, providerName.
     */
    private List<HotelDto> normalizeLiveCandidates(
            List<ExternalHotelSearchResponse.HotelCandidateDto> candidates,
            String destination, BigDecimal budget, Integer durationDays,
            Integer travelers, String hotelPreference, String sourceProvider) {

        double budgetPerNight = budget.doubleValue() * 0.30 / Math.max(durationDays, 1);

        // Determine truthfulness fields based on provider
        // All current providers (Geoapify, Overpass/OSM, OTM) give real place names
        // but NEVER live booking prices — prices are always estimated
        boolean isGeoapify = sourceProvider != null && sourceProvider.contains("geoapify");
        boolean isOsm      = sourceProvider != null && (sourceProvider.contains("overpass") || sourceProvider.contains("osm"));
        boolean isOtm      = sourceProvider != null && sourceProvider.contains("opentripmap");

        String sourceType   = "BASIC_PLACE_DATA";   // all current providers = real place data
        String priceType    = "ESTIMATED_PRICE";     // none return live booking prices
        String providerName = isGeoapify ? "GEOAPIFY"
                            : isOsm     ? "OVERPASS_OSM"
                            : isOtm     ? "OPENTRIPMAP"
                            : sourceProvider != null
                                ? sourceProvider.toUpperCase().replace("-", "_").replace(" ", "_")
                                : "UNKNOWN";

        return candidates.stream()
                .filter(c -> c.getName() != null && !c.getName().isBlank())
                .filter(c -> matchesPreference(c.getCategory(), hotelPreference))
                .map(c -> {
                    // Estimate price if provider didn't return one
                    double price = c.getPricePerNight() != null
                            ? c.getPricePerNight()
                            : estimatePriceFromCategory(c.getCategory(), destination);

                    double rating = c.getRating() != null ? c.getRating() : 3.5;
                    double distance = c.getDistanceFromCenterKm() != null
                            ? c.getDistanceFromCenterKm() : 2.0;
                    double popularity = c.getPopularityScore() != null
                            ? c.getPopularityScore() : computePopularityFromRating(rating, c.getReviewCount());

                    return HotelDto.builder()
                            .id(null)
                            .externalHotelId(c.getExternalHotelId())
                            .name(c.getName())
                            .destination(destination)
                            .areaName(c.getAreaName())
                            .pricePerNight(price)
                            .rating(rating)
                            .distanceFromCenterKm(distance)
                            .amenities(c.getAmenities() != null ? c.getAmenities() : List.of())
                            .category(c.getCategory() != null ? c.getCategory() : "STANDARD")
                            .popularityScore(popularity)
                            .relevanceScore(0.0)
                            .imageUrl(c.getPhotoUrl())
                            .reviewCount(c.getReviewCount())
                            .lat(c.getLat())
                            .lng(c.getLng())
                            // Truthfulness contract
                            .sourceType(sourceType)
                            .priceType(priceType)
                            .providerName(providerName)
                            .sourceProvider(sourceProvider != null ? sourceProvider : "overpass_osm")
                            .fallbackUsed(false)
                            .isSynthetic(false)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /**
     * Runs ML ranking on live hotel candidates.
     * Uses externalHotelId as the key since live hotels have no DB ID.
     */
    private List<HotelDto> rankLiveCandidatesWithMl(List<HotelDto> candidates,
                                                      String destination, BigDecimal budget,
                                                      Integer durationDays, Integer travelers,
                                                      String preference) {
        try {
            // Build ML request using index as hotel_id (live hotels have no DB ID)
            List<Map<String, Object>> hotelFeatures = new ArrayList<>();
            for (int i = 0; i < candidates.size(); i++) {
                HotelDto h = candidates.get(i);
                Map<String, Object> f = new HashMap<>();
                f.put("hotel_id", i);   // use index as temporary ID
                f.put("price_per_night", h.getPricePerNight());
                f.put("rating", h.getRating());
                f.put("distance_from_center_km", h.getDistanceFromCenterKm());
                f.put("category", h.getCategory());
                f.put("popularity_score", h.getPopularityScore());
                hotelFeatures.add(f);
            }

            Map<String, Object> mlRequest = new HashMap<>();
            mlRequest.put("destination", destination);
            mlRequest.put("budget", budget);
            mlRequest.put("duration_days", durationDays);
            mlRequest.put("travelers", travelers);
            mlRequest.put("hotel_preference", preference);
            mlRequest.put("hotels", hotelFeatures);

            Map<String, Object> mlResponse = mlServiceClient.rankHotels(mlRequest);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rankedHotels = (List<Map<String, Object>>) mlResponse.get("ranked_hotels");

            if (rankedHotels != null) {
                Map<Integer, Double> scoreMap = new HashMap<>();
                for (Map<String, Object> rh : rankedHotels) {
                    int idx = ((Number) rh.get("hotel_id")).intValue();
                    double score = ((Number) rh.get("relevance_score")).doubleValue();
                    scoreMap.put(idx, score);
                }

                List<HotelDto> result = new ArrayList<>();
                for (int i = 0; i < candidates.size(); i++) {
                    double score = scoreMap.getOrDefault(i, computeRuleScoreFromDto(candidates.get(i)));
                    HotelDto h = candidates.get(i);
                    h.setRelevanceScore(Math.round(score * 1000.0) / 1000.0);
                    result.add(h);
                }
                result.sort(Comparator.comparingDouble(h -> -h.getRelevanceScore()));
                return result;
            }
        } catch (Exception e) {
            log.warn("ML ranking failed for live candidates: {} — using rule-based sort", e.getMessage());
        }

        // Rule-based fallback sort
        candidates.forEach(h -> h.setRelevanceScore(
                Math.round(computeRuleScoreFromDto(h) * 1000.0) / 1000.0));
        candidates.sort(Comparator.comparingDouble(h -> -h.getRelevanceScore()));
        return candidates;
    }

    // ── Synthetic fallback hotel ──────────────────────────────────────────────

    /**
     * Generates a synthetic hotel recommendation when no real data is available.
     * Uses destination + budget + preference to produce realistic estimates.
     * Always returns a non-null HotelDto.
     */
    private HotelDto generateSyntheticHotel(String destination, BigDecimal budget,
                                             Integer durationDays, Integer travelers,
                                             String hotelPreference) {
        return generateSyntheticHotel(destination, budget, durationDays, travelers,
                hotelPreference, "INR");
    }

    private HotelDto generateSyntheticHotel(String destination, BigDecimal budget,
                                             Integer durationDays, Integer travelers,
                                             String hotelPreference, String currency) {
        int days = durationDays != null ? Math.max(durationDays, 1) : 1;
        double totalBudget = budget != null ? budget.doubleValue() : 30000.0;

        // Hotel budget slice: 30% of total budget
        double hotelBudgetTotal = totalBudget * 0.30;
        double nightlyBudget = hotelBudgetTotal / days;

        // Tier-based nightly rate and rating
        String pref = hotelPreference != null ? hotelPreference.toUpperCase() : "STANDARD";
        double nightlyRate;
        double rating;
        double distance;
        String tierLabel;
        String amenitiesStr;

        switch (pref) {
            case "LUXURY" -> {
                nightlyRate = Math.max(nightlyBudget * 0.9, 8000.0);
                rating = 4.5 + (Math.random() * 0.4);   // 4.5–4.9
                distance = 1.0 + Math.random();
                tierLabel = "Luxury Suites";
                amenitiesStr = "pool,wifi,spa,restaurant,gym,concierge";
            }
            case "BUDGET" -> {
                nightlyRate = Math.min(nightlyBudget * 0.7, 2500.0);
                rating = 3.5 + (Math.random() * 0.5);   // 3.5–4.0
                distance = 2.5 + Math.random() * 2;
                tierLabel = "Budget Stay";
                amenitiesStr = "wifi,parking";
            }
            default -> {
                nightlyRate = nightlyBudget * 0.75;
                rating = 4.0 + (Math.random() * 0.4);   // 4.0–4.4
                distance = 1.5 + Math.random();
                tierLabel = "Standard Stay";
                amenitiesStr = "pool,wifi,restaurant,gym";
            }
        }

        // Round nightly rate to nearest 100
        nightlyRate = Math.round(nightlyRate / 100.0) * 100.0;
        rating = Math.round(rating * 10.0) / 10.0;
        distance = Math.round(distance * 10.0) / 10.0;

        // Build a readable hotel name
        String cityPart = destination.length() > 20
                ? destination.substring(0, 20).trim() : destination;
        String hotelName = "TripForge " + tierLabel + " — " + cityPart;

        String note = String.format(
                "Estimated stay based on your budget. Actual availability may vary. " +
                "Estimated nightly rate: %.0f for %d night(s).", nightlyRate, days);

        return HotelDto.builder()
                .id(null)
                .name(hotelName)
                .destination(destination)
                .areaName("City Centre")
                .pricePerNight(nightlyRate)
                .rating(rating)
                .distanceFromCenterKm(distance)
                .amenities(Arrays.asList(amenitiesStr.split(",")))
                .category(pref)
                .popularityScore(Math.round(rating * 1.8 * 10.0) / 10.0)
                .relevanceScore(0.75)
                // Truthfulness contract
                .sourceType("SYNTHETIC")
                .priceType("ESTIMATED_PRICE")
                .providerName("SYNTHETIC")
                .sourceProvider("TRIPFORGE_FALLBACK")
                .fallbackUsed(true)
                .warnings(List.of(
                        "Hotel generated by TripForge heuristic — no live provider data available for " + destination,
                        note))
                .build();
    }

    // ── CSV fallback path (existing behavior, unchanged) ─────────────────────

    private List<HotelDto> recommendFromCsv(String destination, BigDecimal budget,
                                              Integer durationDays, Integer travelers,
                                              String hotelPreference) {
        List<Hotel> candidates = filterCandidates(destination, budget, durationDays, hotelPreference);

        if (candidates.isEmpty()) {
            log.warn("No hotels found in CSV for destination: {}", destination);
            return List.of();
        }

        List<HotelDto> ranked = rankWithMl(candidates, destination, budget, durationDays,
                travelers, hotelPreference);
        return ranked.stream().limit(MAX_RECOMMENDATIONS).collect(Collectors.toList());
    }

    private List<Hotel> filterCandidates(String destination, BigDecimal budget,
                                          Integer durationDays, String hotelPreference) {
        double budgetPerNight = budget.doubleValue() * 0.30 / Math.max(durationDays, 1);
        List<Hotel> byDestination = hotelRepository.findByDestinationIgnoreCase(destination);
        List<Hotel> byCategory = byDestination.stream()
                .filter(h -> matchesPreference(h.getCategory(), hotelPreference))
                .collect(Collectors.toList());
        List<Hotel> pool = byCategory.isEmpty() ? byDestination : byCategory;
        List<Hotel> affordable = pool.stream()
                .filter(h -> h.getPricePerNight() <= budgetPerNight * 1.2)
                .collect(Collectors.toList());
        return affordable.isEmpty() ? pool : affordable;
    }

    private List<HotelDto> rankWithMl(List<Hotel> candidates, String destination,
                                       BigDecimal budget, Integer durationDays,
                                       Integer travelers, String preference) {
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
            mlRequest.put("destination", destination);
            mlRequest.put("budget", budget);
            mlRequest.put("duration_days", durationDays);
            mlRequest.put("travelers", travelers);
            mlRequest.put("hotel_preference", preference);
            mlRequest.put("hotels", hotelFeatures);

            Map<String, Object> mlResponse = mlServiceClient.rankHotels(mlRequest);

            @SuppressWarnings("unchecked")
            List<Map<String, Object>> rankedHotels = (List<Map<String, Object>>) mlResponse.get("ranked_hotels");

            if (rankedHotels != null) {
                Map<Long, Double> scoreMap = new HashMap<>();
                for (Map<String, Object> rh : rankedHotels) {
                    scoreMap.put(((Number) rh.get("hotel_id")).longValue(),
                            ((Number) rh.get("relevance_score")).doubleValue());
                }
                return candidates.stream()
                        .sorted(Comparator.comparingDouble(
                                h -> -scoreMap.getOrDefault(h.getId(), computeRuleScore(h))))
                        .map(h -> mapCsvToDto(h, scoreMap.getOrDefault(h.getId(), computeRuleScore(h))))
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("ML ranking failed, falling back to rule-based: {}", e.getMessage());
        }
        return candidates.stream()
                .sorted(Comparator.comparingDouble(h -> -computeRuleScore(h)))
                .map(h -> mapCsvToDto(h, computeRuleScore(h)))
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
                        .map(h -> mapCsvToDto(h, scoreMap.getOrDefault(h.getId(), 0.0)))
                        .limit(4)
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("ML re-ranking failed: {}", e.getMessage());
        }
        return applyReasonSort(candidates, request.getReason()).stream()
                .map(h -> mapCsvToDto(h, computeRuleScore(h)))
                .limit(4)
                .collect(Collectors.toList());
    }

    // ── Shared helpers ────────────────────────────────────────────────────────

    private boolean matchesPreference(String category, String preference) {
        if (preference == null || category == null) return true;
        return switch (preference.toUpperCase()) {
            case "BUDGET" -> "BUDGET".equals(category);
            case "LUXURY" -> "LUXURY".equals(category);
            default -> true;
        };
    }

    private double computeRuleScore(Hotel h) {
        double ratingScore = h.getRating() / 5.0;
        double popularityScore = h.getPopularityScore() / 10.0;
        double distanceScore = 1.0 / (1.0 + h.getDistanceFromCenterKm());
        return (ratingScore * 0.4) + (popularityScore * 0.35) + (distanceScore * 0.25);
    }

    private double computeRuleScoreFromDto(HotelDto h) {
        double ratingScore = (h.getRating() != null ? h.getRating() : 3.5) / 5.0;
        double popularityScore = (h.getPopularityScore() != null ? h.getPopularityScore() : 5.0) / 10.0;
        double distanceScore = 1.0 / (1.0 + (h.getDistanceFromCenterKm() != null ? h.getDistanceFromCenterKm() : 2.0));
        return (ratingScore * 0.4) + (popularityScore * 0.35) + (distanceScore * 0.25);
    }

    private double estimatePriceFromCategory(String category, String destination) {
        // Rough price estimates by category when provider doesn't return price
        double base = switch (destination.toLowerCase()) {
            case "goa", "manali" -> 5000.0;
            case "bangalore" -> 6000.0;
            default -> 3500.0;
        };
        return switch (category != null ? category.toUpperCase() : "STANDARD") {
            case "LUXURY" -> base * 3.0;
            case "BUDGET" -> base * 0.4;
            default -> base;
        };
    }

    private double computePopularityFromRating(double rating, Integer reviewCount) {
        double r = rating / 5.0 * 7.0;
        double rc = reviewCount != null ? Math.min(reviewCount / 1000.0, 1.0) * 3.0 : 0.0;
        return Math.round((r + rc) * 10.0) / 10.0;
    }

    private List<Hotel> applyReasonFilter(List<Hotel> hotels, String reason, Long currentHotelId) {
        Hotel current = hotelRepository.findById(currentHotelId).orElse(null);
        if (current == null) return hotels;
        return switch (reason.toUpperCase()) {
            case "CHEAPER" -> hotels.stream().filter(h -> h.getPricePerNight() < current.getPricePerNight()).collect(Collectors.toList());
            case "BETTER_RATING" -> hotels.stream().filter(h -> h.getRating() > current.getRating()).collect(Collectors.toList());
            case "CLOSER" -> hotels.stream().filter(h -> h.getDistanceFromCenterKm() < current.getDistanceFromCenterKm()).collect(Collectors.toList());
            case "PREMIUM" -> hotels.stream().filter(h -> "LUXURY".equals(h.getCategory())).collect(Collectors.toList());
            default -> hotels;
        };
    }

    private List<Hotel> applyReasonSort(List<Hotel> hotels, String reason) {
        return switch (reason.toUpperCase()) {
            case "CHEAPER" -> hotels.stream().sorted(Comparator.comparingDouble(Hotel::getPricePerNight)).collect(Collectors.toList());
            case "BETTER_RATING" -> hotels.stream().sorted(Comparator.comparingDouble(Hotel::getRating).reversed()).collect(Collectors.toList());
            case "CLOSER" -> hotels.stream().sorted(Comparator.comparingDouble(Hotel::getDistanceFromCenterKm)).collect(Collectors.toList());
            case "PREMIUM" -> hotels.stream().sorted(Comparator.comparingDouble(Hotel::getPricePerNight).reversed()).collect(Collectors.toList());
            default -> hotels;
        };
    }

    /** Maps a CSV Hotel entity to HotelDto with truthfulness metadata. */
    private HotelDto mapCsvToDto(Hotel h, double relevanceScore) {
        List<String> amenitiesList = (h.getAmenities() != null && !h.getAmenities().isBlank())
                ? Arrays.asList(h.getAmenities().split(",")) : List.of();
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
                // Truthfulness contract
                .sourceType("DATASET")
                .priceType("DATASET_PRICE")
                .providerName("CSV")
                .sourceProvider("csv_dataset")
                .fallbackUsed(true)
                .isSynthetic(false)
                .build();
    }
}
