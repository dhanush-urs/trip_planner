package com.tripforge.trip.service;

import com.tripforge.trip.client.AiOrchestratorClient;
import com.tripforge.trip.client.BudgetServiceClient;
import com.tripforge.trip.client.HotelServiceClient;
import com.tripforge.trip.client.PaymentServiceClient;
import com.tripforge.trip.client.RouteServiceClient;
import com.tripforge.trip.client.SplitServiceClient;
import com.tripforge.trip.dto.*;
import com.tripforge.trip.dto.ai.*;
import com.tripforge.trip.dto.payment.InitPaymentRequest;
import com.tripforge.trip.dto.payment.PaymentSummaryDto;
import com.tripforge.trip.entity.ItineraryDay;
import com.tripforge.trip.entity.Trip;
import com.tripforge.trip.entity.TripPlace;
import com.tripforge.trip.exception.ResourceNotFoundException;
import com.tripforge.trip.repository.TripRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Core trip orchestration service — Phase 9C upgrade.
 *
 * Changes from Phase 9A:
 *   - Passes mustVisitPlaces to route-service for live route optimization
 *   - Merges provider metadata (sourceProvider, fallbackUsed) from hotel + route
 *   - Computes providerMode: LIVE | MIXED | FALLBACK
 *   - Adds warnings[] to TripResponse
 *   - Stores currency on trip entity
 *   - All new fields are optional — existing frontend works unchanged
 */
@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);

    @Autowired private TripRepository tripRepository;
    @Autowired private HotelServiceClient hotelServiceClient;
    @Autowired private RouteServiceClient routeServiceClient;
    @Autowired private BudgetServiceClient budgetServiceClient;
    @Autowired private SplitServiceClient splitServiceClient;
    @Autowired private AiOrchestratorClient aiOrchestratorClient;
    @Autowired private PaymentServiceClient paymentServiceClient;

    // ── Create Trip ───────────────────────────────────────────────────────────

    @Transactional
    public TripResponse createTrip(Long userId, TripCreateRequest request) {
        log.info("Creating trip for user {} to {}", userId, request.getDestination());

        int durationDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());
        String currency = request.getCurrency() != null ? request.getCurrency() : "INR";

        // 1. Persist trip skeleton
        Trip trip = Trip.builder()
                .userId(userId)
                .destination(request.getDestination())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .totalBudget(request.getTotalBudget())
                .travelers(request.getTravelers())
                .interests(request.getInterests() != null
                        ? String.join(",", request.getInterests()) : "")
                .hotelPreference(request.getHotelPreference() != null
                        ? request.getHotelPreference() : Trip.HotelPreference.STANDARD)
                .status(Trip.TripStatus.PLANNED)
                .currencyCode(currency)
                .build();

        trip = tripRepository.save(trip);
        final Long tripId = trip.getId();
        log.info("Trip saved with id: {}", tripId);

        // 2. Hotel recommendations (live first, CSV fallback)
        List<HotelDto> hotels = fetchHotels(request, durationDays);
        HotelDto selectedHotel = hotels.isEmpty() ? null : hotels.get(0);
        List<HotelDto> alternatives = hotels.size() > 1
                ? hotels.subList(1, Math.min(4, hotels.size())) : List.of();

        if (selectedHotel != null) {
            trip.setSelectedHotelId(selectedHotel.getId());
            tripRepository.save(trip);
        }

        // 3. Itinerary (live route optimization first, CSV fallback)
        List<ItineraryDayDto> itinerary = fetchItinerary(request, tripId, durationDays);

        // 4. Persist itinerary
        persistItinerary(trip, itinerary);

        // 5. Budget breakdown
        BudgetBreakdownDto budget = fetchBudget(request, tripId, selectedHotel, durationDays, itinerary);

        // 6. Split
        SplitResultDto split = fetchSplit(tripId, budget, request.getTravelers());

        // 7. Compute provider metadata
        String providerMode = computeProviderMode(selectedHotel, itinerary);
        String providerSummary = buildProviderSummary(selectedHotel, itinerary);
        List<String> warnings = collectWarnings(selectedHotel, itinerary, hotels);

        // 8. AI enrichment — non-blocking, never fails trip creation
        AiEnrichment ai = fetchAiEnrichment(
                request, selectedHotel, itinerary, providerMode, durationDays);

        // 9. Initialize payment tracking — non-blocking, never fails trip creation
        PaymentSummaryDto paymentSummary = initPaymentTracking(
                tripId, budget, split, currency);

        log.info("Trip planning complete for tripId: {} | providerMode: {} | aiEnriched: {}",
                tripId, providerMode, ai.enriched);

        return buildTripResponse(trip, itinerary, selectedHotel, alternatives,
                budget, split, durationDays, providerMode, providerSummary, warnings,
                currency, ai, paymentSummary);
    }

    // ── Get Trip ──────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public TripResponse getTrip(Long tripId, Long userId) {
        Trip trip = tripRepository.findById(tripId)
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + tripId));

        if (!trip.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Trip not found: " + tripId);
        }

        List<ItineraryDayDto> itinerary = trip.getItineraryDays().stream()
                .map(this::mapDayToDto)
                .collect(Collectors.toList());

        int durationDays = (int) ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate());
        HotelDto selectedHotel = fetchHotelById(trip.getSelectedHotelId());
        String tripCurrency = trip.getCurrencyCode() != null ? trip.getCurrencyCode() : "INR";

        return buildTripResponse(trip, itinerary, selectedHotel, List.of(),
                null, null, durationDays, null, null, null, tripCurrency, new AiEnrichment());
    }

    // ── Get User Trips ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<TripSummaryDto> getUserTrips(Long userId) {
        return tripRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(this::mapToSummary).collect(Collectors.toList());
    }

    // ── Replan ────────────────────────────────────────────────────────────────

    @Transactional
    public TripResponse replan(Long userId, ReplanRequest request) {
        Trip trip = tripRepository.findById(request.getTripId())
                .orElseThrow(() -> new ResourceNotFoundException("Trip not found: " + request.getTripId()));

        if (!trip.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Trip not found: " + request.getTripId());
        }

        log.info("Replanning trip {} with new hotel {}", trip.getId(), request.getNewHotelId());

        trip.setSelectedHotelId(request.getNewHotelId());
        tripRepository.save(trip);

        int durationDays = (int) ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate());
        List<ItineraryDayDto> itinerary = trip.getItineraryDays().stream()
                .map(this::mapDayToDto).collect(Collectors.toList());

        HotelDto newHotel = fetchHotelById(request.getNewHotelId());
        TripCreateRequest fakeRequest = buildRequestFromTrip(trip);
        BudgetBreakdownDto budget = fetchBudget(fakeRequest, trip.getId(), newHotel, durationDays, itinerary);
        SplitResultDto split = fetchSplit(trip.getId(), budget, trip.getTravelers());

        return buildTripResponse(trip, itinerary, newHotel, List.of(),
                budget, split, durationDays, null, null, null,
                trip.getCurrencyCode() != null ? trip.getCurrencyCode() : "INR");
    }

    // ── Private: fetch helpers ────────────────────────────────────────────────

    private HotelDto fetchHotelById(Long hotelId) {
        if (hotelId == null) return null;
        try {
            ApiResponse<HotelDto> response = hotelServiceClient.getHotelById(hotelId);
            return response.getData();
        } catch (Exception e) {
            log.warn("Could not fetch hotel by id {}: {}", hotelId, e.getMessage());
            return null;
        }
    }

    private List<HotelDto> fetchHotels(TripCreateRequest req, int durationDays) {
        try {
            ApiResponse<List<HotelDto>> response = hotelServiceClient.recommendHotels(
                    req.getDestination(), req.getTotalBudget(), durationDays,
                    req.getTravelers(),
                    req.getHotelPreference() != null ? req.getHotelPreference().name() : "STANDARD",
                    req.getDestinationLat(),
                    req.getDestinationLng(),
                    req.getCurrency() != null ? req.getCurrency() : "INR");
            return response.getData() != null ? response.getData() : List.of();
        } catch (Exception e) {
            log.warn("Hotel service call failed: {}", e.getMessage());
            return List.of();
        }
    }

    private List<ItineraryDayDto> fetchItinerary(TripCreateRequest req, Long tripId, int durationDays) {
        try {
            Map<String, Object> routeRequest = new HashMap<>();
            routeRequest.put("tripId", tripId);
            routeRequest.put("destination", req.getDestination());
            routeRequest.put("startDate", req.getStartDate().toString());
            routeRequest.put("durationDays", durationDays);
            routeRequest.put("interests", req.getInterests() != null ? req.getInterests() : List.of());

            // Phase 9C: pass live places if user provided them
            if (req.getMustVisitPlaces() != null && !req.getMustVisitPlaces().isEmpty()) {
                List<Map<String, Object>> livePlaces = req.getMustVisitPlaces().stream()
                        .map(p -> {
                            Map<String, Object> wp = new HashMap<>();
                            wp.put("placeId", p.getPlaceId());
                            wp.put("name", p.getName());
                            wp.put("category", p.getCategory());
                            wp.put("lat", p.getLat());
                            wp.put("lng", p.getLng());
                            wp.put("estimatedVisitMinutes",
                                    p.getEstimatedVisitMinutes() != null ? p.getEstimatedVisitMinutes() : 90);
                            return wp;
                        }).collect(Collectors.toList());
                routeRequest.put("livePlaces", livePlaces);
            }

            ApiResponse<List<ItineraryDayDto>> response = routeServiceClient.optimizeRoute(routeRequest);
            return response.getData() != null ? response.getData() : List.of();
        } catch (Exception e) {
            log.warn("Route service call failed: {}", e.getMessage());
            return List.of();
        }
    }

    private BudgetBreakdownDto fetchBudget(TripCreateRequest req, Long tripId,
                                            HotelDto hotel, int durationDays,
                                            List<ItineraryDayDto> itinerary) {
        try {
            Map<String, Object> budgetRequest = new HashMap<>();
            budgetRequest.put("tripId", tripId);
            budgetRequest.put("destination", req.getDestination());
            budgetRequest.put("durationDays", durationDays);
            budgetRequest.put("travelers", req.getTravelers());
            budgetRequest.put("totalBudget", req.getTotalBudget());
            budgetRequest.put("hotelPricePerNight", hotel != null ? hotel.getPricePerNight() : null);
            budgetRequest.put("hotelCategory", hotel != null ? hotel.getCategory() : "STANDARD");
            budgetRequest.put("itinerary", itinerary);
            // Phase 9E: pass currency
            budgetRequest.put("currencyCode", req.getCurrency() != null ? req.getCurrency() : "INR");

            ApiResponse<BudgetBreakdownDto> response = budgetServiceClient.calculateBudget(budgetRequest);
            return response.getData();
        } catch (Exception e) {
            log.warn("Budget service call failed: {}", e.getMessage());
            return null;
        }
    }

    private SplitResultDto fetchSplit(Long tripId, BudgetBreakdownDto budget, int travelers) {
        try {
            Map<String, Object> splitRequest = new HashMap<>();
            splitRequest.put("tripId", tripId);
            splitRequest.put("totalAmount", budget != null ? budget.getTotalEstimated() : 0);
            splitRequest.put("travelers", travelers);
            // Phase 9E: pass currency from budget (already converted)
            splitRequest.put("currencyCode", budget != null && budget.getCurrencyCode() != null
                    ? budget.getCurrencyCode() : "INR");

            ApiResponse<SplitResultDto> response = splitServiceClient.splitEqual(splitRequest);
            return response.getData();
        } catch (Exception e) {
            log.warn("Split service call failed: {}", e.getMessage());
            return null;
        }
    }

    // ── Private: provider metadata ────────────────────────────────────────────

    /**
     * Determines the overall provider mode for the trip response.
     * LIVE   = hotel is live AND itinerary has at least one live day
     * MIXED  = one of hotel/itinerary is live, the other is fallback
     * FALLBACK = both hotel and itinerary used CSV/heuristic fallback
     */
    private String computeProviderMode(HotelDto hotel, List<ItineraryDayDto> itinerary) {
        boolean hotelLive = hotel != null && !hotel.isFallbackUsed()
                && !"csv_dataset".equals(hotel.getSourceProvider());
        boolean itineraryLive = itinerary != null && itinerary.stream()
                .anyMatch(d -> !d.isFallbackUsed());

        if (hotelLive && itineraryLive) return "LIVE";
        if (hotelLive || itineraryLive) return "MIXED";
        return "FALLBACK";
    }

    private String buildProviderSummary(HotelDto hotel, List<ItineraryDayDto> itinerary) {
        StringBuilder sb = new StringBuilder();

        String hotelProvider = hotel != null
                ? (hotel.isFallbackUsed() ? "CSV dataset" : hotel.getSourceProvider())
                : "none";
        sb.append("Hotel: ").append(hotelProvider).append(". ");

        if (itinerary != null && !itinerary.isEmpty()) {
            boolean anyLive = itinerary.stream().anyMatch(d -> !d.isFallbackUsed());
            sb.append("Itinerary: ").append(anyLive ? "live route optimization" : "CSV heuristic").append(".");
        }

        return sb.toString();
    }

    private List<String> collectWarnings(HotelDto hotel, List<ItineraryDayDto> itinerary,
                                          List<HotelDto> allHotels) {
        List<String> warnings = new ArrayList<>();

        if (hotel == null) {
            warnings.add("No hotel recommendation available for this destination.");
        } else if (hotel.isFallbackUsed()) {
            warnings.add("Hotel recommendations sourced from local dataset (live provider unavailable).");
        }

        if (itinerary == null || itinerary.isEmpty()) {
            warnings.add("No itinerary generated for this destination.");
        } else {
            boolean allFallback = itinerary.stream().allMatch(DayPlanDto -> DayPlanDto.isFallbackUsed());
            if (allFallback) {
                warnings.add("Itinerary generated from local attraction dataset (live route optimization unavailable).");
            }
        }

        // Collect warnings from hotel DTOs
        if (hotel != null && hotel.getWarnings() != null) {
            warnings.addAll(hotel.getWarnings());
        }

        return warnings.isEmpty() ? null : warnings;
    }

    // ── Private: persistence ──────────────────────────────────────────────────

    private void persistItinerary(Trip trip, List<ItineraryDayDto> itinerary) {
        trip.getItineraryDays().clear();

        for (ItineraryDayDto dayDto : itinerary) {
            ItineraryDay day = new ItineraryDay();
            day.setTrip(trip);
            day.setDayNumber(dayDto.getDayNumber());
            day.setDate(dayDto.getDate());
            day.setTheme(dayDto.getTheme());

            if (dayDto.getPlaces() != null) {
                for (TripPlaceDto placeDto : dayDto.getPlaces()) {
                    TripPlace place = new TripPlace();
                    place.setItineraryDay(day);
                    place.setAttractionId(placeDto.getAttractionId());
                    place.setName(placeDto.getName());
                    place.setCategory(placeDto.getCategory());
                    place.setVisitTime(placeDto.getVisitTime());
                    place.setAvgVisitHours(placeDto.getAvgVisitHours());
                    place.setTicketCost(placeDto.getTicketCost());
                    place.setNotes(placeDto.getNotes());
                    place.setVisitOrder(placeDto.getVisitOrder());
                    day.getPlaces().add(place);
                }
            }
            trip.getItineraryDays().add(day);
        }
        tripRepository.save(trip);
    }

    // ── Private: response builders ────────────────────────────────────────────

    private TripResponse buildTripResponse(Trip trip, List<ItineraryDayDto> itinerary,
                                            HotelDto selectedHotel, List<HotelDto> alternatives,
                                            BudgetBreakdownDto budget, SplitResultDto split,
                                            int durationDays, String providerMode,
                                            String providerSummary, List<String> warnings,
                                            String currency) {
        return buildTripResponse(trip, itinerary, selectedHotel, alternatives,
                budget, split, durationDays, providerMode, providerSummary, warnings,
                currency, new AiEnrichment(), null);
    }

    private TripResponse buildTripResponse(Trip trip, List<ItineraryDayDto> itinerary,
                                            HotelDto selectedHotel, List<HotelDto> alternatives,
                                            BudgetBreakdownDto budget, SplitResultDto split,
                                            int durationDays, String providerMode,
                                            String providerSummary, List<String> warnings,
                                            String currency, AiEnrichment ai) {
        return buildTripResponse(trip, itinerary, selectedHotel, alternatives,
                budget, split, durationDays, providerMode, providerSummary, warnings,
                currency, ai, null);
    }

    private TripResponse buildTripResponse(Trip trip, List<ItineraryDayDto> itinerary,
                                            HotelDto selectedHotel, List<HotelDto> alternatives,
                                            BudgetBreakdownDto budget, SplitResultDto split,
                                            int durationDays, String providerMode,
                                            String providerSummary, List<String> warnings,
                                            String currency, AiEnrichment ai,
                                            PaymentSummaryDto paymentSummary) {
        List<String> interests = (trip.getInterests() != null && !trip.getInterests().isBlank())
                ? Arrays.asList(trip.getInterests().split(",")) : List.of();

        return TripResponse.builder()
                .tripId(trip.getId())
                .userId(trip.getUserId())
                .destination(trip.getDestination())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .durationDays(durationDays)
                .totalBudget(trip.getTotalBudget())
                .travelers(trip.getTravelers())
                .interests(interests)
                .hotelPreference(trip.getHotelPreference() != null ? trip.getHotelPreference().name() : null)
                .status(trip.getStatus().name())
                .createdAt(trip.getCreatedAt())
                .itinerary(itinerary)
                .selectedHotel(selectedHotel)
                .alternativeHotels(alternatives)
                .budgetBreakdown(budget)
                .splitResult(split)
                .providerMode(providerMode)
                .providerSummary(providerSummary)
                .warnings(warnings)
                .currency(currency)
                // Phase 9D AI enrichment fields
                .aiHeadline(ai.headline)
                .aiSummary(ai.summary)
                .hotelExplanation(ai.hotelExplanation)
                .itineraryExplanation(ai.itineraryExplanation)
                .aiEnriched(ai.enriched)
                .aiProvider(ai.provider.equals("none") ? null : ai.provider)
                .paymentSummary(paymentSummary)
                .paymentAvailable(paymentSummary != null)
                .build();
    }

    private ItineraryDayDto mapDayToDto(ItineraryDay day) {
        List<TripPlaceDto> places = day.getPlaces().stream()
                .map(p -> TripPlaceDto.builder()
                        .attractionId(p.getAttractionId())
                        .name(p.getName())
                        .category(p.getCategory())
                        .visitTime(p.getVisitTime())
                        .avgVisitHours(p.getAvgVisitHours())
                        .ticketCost(p.getTicketCost())
                        .notes(p.getNotes())
                        .visitOrder(p.getVisitOrder())
                        .build())
                .collect(Collectors.toList());

        return ItineraryDayDto.builder()
                .dayNumber(day.getDayNumber())
                .date(day.getDate())
                .theme(day.getTheme())
                .places(places)
                .sourceProvider("csv_dataset")
                .fallbackUsed(true)
                .build();
    }

    private TripSummaryDto mapToSummary(Trip trip) {
        int days = (int) ChronoUnit.DAYS.between(trip.getStartDate(), trip.getEndDate());
        return TripSummaryDto.builder()
                .tripId(trip.getId())
                .destination(trip.getDestination())
                .startDate(trip.getStartDate())
                .endDate(trip.getEndDate())
                .durationDays(days)
                .totalBudget(trip.getTotalBudget())
                .travelers(trip.getTravelers())
                .status(trip.getStatus().name())
                .createdAt(trip.getCreatedAt())
                .build();
    }

    private TripCreateRequest buildRequestFromTrip(Trip trip) {
        TripCreateRequest req = new TripCreateRequest();
        req.setDestination(trip.getDestination());
        req.setStartDate(trip.getStartDate());
        req.setEndDate(trip.getEndDate());
        req.setTotalBudget(trip.getTotalBudget());
        req.setTravelers(trip.getTravelers());
        req.setHotelPreference(trip.getHotelPreference());
        req.setCurrency(trip.getCurrencyCode() != null ? trip.getCurrencyCode() : "INR");
        if (trip.getInterests() != null && !trip.getInterests().isBlank()) {
            req.setInterests(Arrays.asList(trip.getInterests().split(",")));
        }
        return req;
    }

    // ── Payment tracking ──────────────────────────────────────────────────────

    /**
     * Initializes payment tracking in payment-service after trip creation.
     * Non-blocking — never fails trip creation.
     * Returns the payment summary if initialization succeeded, null otherwise.
     */
    private PaymentSummaryDto initPaymentTracking(Long tripId, BudgetBreakdownDto budget,
                                                   SplitResultDto split, String currency) {
        try {
            if (budget == null || budget.getTotalEstimated() == null) return null;

            // Build participant list from split result
            List<InitPaymentRequest.ParticipantInfo> participants = new ArrayList<>();
            if (split != null && split.getParticipants() != null) {
                long pid = 1;
                for (SplitResultDto.ParticipantDto p : split.getParticipants()) {
                    participants.add(InitPaymentRequest.ParticipantInfo.builder()
                            .participantId(p.getParticipantId() != null ? p.getParticipantId() : pid++)
                            .participantName(p.getName())
                            .participantEmail(p.getEmail())
                            .allocatedAmount(p.getAmount())
                            .build());
                }
            }

            InitPaymentRequest initReq = InitPaymentRequest.builder()
                    .tripId(tripId)
                    .totalAmount(budget.getTotalEstimated())
                    .currencyCode(currency)
                    .participants(participants)
                    .build();

            paymentServiceClient.initPayment(initReq);

            // Fetch and return the initialized summary
            ApiResponse<PaymentSummaryDto> summaryResp =
                    paymentServiceClient.getTripPaymentSummary(tripId);
            return summaryResp != null ? summaryResp.getData() : null;

        } catch (Exception e) {
            log.warn("Payment tracking init failed (non-fatal): {} — trip creation continues",
                    e.getMessage());
            return null;
        }
    }

    /** Holds all AI-generated enrichment fields for a trip response. */
    private static class AiEnrichment {
        String headline;
        String summary;
        String hotelExplanation;
        String itineraryExplanation;
        boolean enriched = false;
        String provider = "none";
    }

    /**
     * Calls ai-orchestrator-service for hotel explanation, itinerary explanation,
     * and trip summary. Wrapped in try/catch — never throws, never blocks trip creation.
     */
    private AiEnrichment fetchAiEnrichment(TripCreateRequest request, HotelDto hotel,
                                            List<ItineraryDayDto> itinerary,
                                            String providerMode, int durationDays) {
        AiEnrichment ai = new AiEnrichment();
        try {
            // Hotel explanation
            if (hotel != null) {
                HotelExplanationRequest hotelReq = HotelExplanationRequest.builder()
                        .destination(request.getDestination())
                        .hotelName(hotel.getName())
                        .hotelPreference(request.getHotelPreference() != null
                                ? request.getHotelPreference().name() : "STANDARD")
                        .budget(request.getTotalBudget() != null
                                ? request.getTotalBudget().doubleValue() : null)
                        .travelers(request.getTravelers())
                        .rating(hotel.getRating())
                        .areaName(hotel.getAreaName())
                        .distanceFromTripCentroid(hotel.getDistanceFromCenterKm())
                        .providerMode(providerMode)
                        .build();

                ApiResponse<HotelExplanationResponse> hotelResp =
                        aiOrchestratorClient.explainHotelChoice(hotelReq);
                if (hotelResp != null && hotelResp.getData() != null) {
                    ai.hotelExplanation = hotelResp.getData().getSummary();
                    ai.provider = hotelResp.getData().getSourceProvider();
                    ai.enriched = !hotelResp.getData().isFallbackUsed();
                }
            }

            // Itinerary explanation
            if (itinerary != null && !itinerary.isEmpty()) {
                List<ItineraryExplanationRequest.PlaceInfo> places = itinerary.stream()
                        .flatMap(d -> d.getPlaces() != null ? d.getPlaces().stream() : java.util.stream.Stream.empty())
                        .limit(6)
                        .map(p -> ItineraryExplanationRequest.PlaceInfo.builder()
                                .name(p.getName())
                                .category(p.getCategory())
                                .build())
                        .collect(Collectors.toList());

                ItineraryExplanationRequest itinReq = ItineraryExplanationRequest.builder()
                        .destination(request.getDestination())
                        .days(durationDays)
                        .tripStyle(request.getTripStyle())
                        .pace(request.getPace())
                        .places(places)
                        .providerMode(providerMode)
                        .build();

                ApiResponse<ItineraryExplanationResponse> itinResp =
                        aiOrchestratorClient.explainItinerary(itinReq);
                if (itinResp != null && itinResp.getData() != null) {
                    ai.itineraryExplanation = itinResp.getData().getSummary();
                    if (!ai.enriched) {
                        ai.enriched = !itinResp.getData().isFallbackUsed();
                        ai.provider = itinResp.getData().getSourceProvider();
                    }
                }
            }

            // Trip summary
            List<String> topPlaces = itinerary != null ? itinerary.stream()
                    .flatMap(d -> d.getPlaces() != null ? d.getPlaces().stream() : java.util.stream.Stream.empty())
                    .map(TripPlaceDto::getName)
                    .filter(n -> n != null && !n.isBlank())
                    .distinct().limit(4)
                    .collect(Collectors.toList()) : List.of();

            TripSummaryRequest summaryReq = TripSummaryRequest.builder()
                    .destination(request.getDestination())
                    .days(durationDays)
                    .hotelName(hotel != null ? hotel.getName() : null)
                    .budget(request.getTotalBudget() != null
                            ? request.getTotalBudget().doubleValue() : null)
                    .currency(request.getCurrency())
                    .tripStyle(request.getTripStyle())
                    .pace(request.getPace())
                    .providerMode(providerMode)
                    .topPlaces(topPlaces)
                    .build();

            ApiResponse<TripSummaryResponse> summaryResp =
                    aiOrchestratorClient.summarizeTrip(summaryReq);
            if (summaryResp != null && summaryResp.getData() != null) {
                ai.headline = summaryResp.getData().getHeadline();
                ai.summary = summaryResp.getData().getShortSummary();
                if (!ai.enriched) {
                    ai.enriched = !summaryResp.getData().isFallbackUsed();
                    ai.provider = summaryResp.getData().getSourceProvider();
                }
            }

            log.info("AI enrichment complete: provider={} enriched={}", ai.provider, ai.enriched);

        } catch (Exception e) {
            log.warn("AI enrichment failed (non-fatal): {} — trip creation continues", e.getMessage());
        }
        return ai;
    }
}
