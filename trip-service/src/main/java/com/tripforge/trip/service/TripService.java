package com.tripforge.trip.service;

import com.tripforge.trip.client.BudgetServiceClient;
import com.tripforge.trip.client.HotelServiceClient;
import com.tripforge.trip.client.RouteServiceClient;
import com.tripforge.trip.client.SplitServiceClient;
import com.tripforge.trip.dto.*;
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
 * Core trip orchestration service.
 * Coordinates hotel, route, budget, and split services to produce a complete trip plan.
 */
@Service
public class TripService {

    private static final Logger log = LoggerFactory.getLogger(TripService.class);

    @Autowired private TripRepository tripRepository;
    @Autowired private HotelServiceClient hotelServiceClient;
    @Autowired private RouteServiceClient routeServiceClient;
    @Autowired private BudgetServiceClient budgetServiceClient;
    @Autowired private SplitServiceClient splitServiceClient;

    /**
     * Creates a new trip and orchestrates the full planning flow.
     */
    @Transactional
    public TripResponse createTrip(Long userId, TripCreateRequest request) {
        log.info("Creating trip for user {} to {}", userId, request.getDestination());

        int durationDays = (int) ChronoUnit.DAYS.between(request.getStartDate(), request.getEndDate());

        // 1. Persist the trip skeleton
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
                .build();

        trip = tripRepository.save(trip);
        final Long tripId = trip.getId();
        log.info("Trip saved with id: {}", tripId);

        // 2. Get hotel recommendations
        List<HotelDto> hotels = fetchHotels(request, durationDays);
        HotelDto selectedHotel = hotels.isEmpty() ? null : hotels.get(0);
        List<HotelDto> alternatives = hotels.size() > 1 ? hotels.subList(1, Math.min(4, hotels.size())) : List.of();

        if (selectedHotel != null) {
            trip.setSelectedHotelId(selectedHotel.getId());
            tripRepository.save(trip);
        }

        // 3. Get day-wise itinerary from route-service
        List<ItineraryDayDto> itinerary = fetchItinerary(request, tripId, durationDays);

        // 4. Persist itinerary days
        persistItinerary(trip, itinerary);

        // 5. Calculate budget breakdown
        BudgetBreakdownDto budget = fetchBudget(request, tripId, selectedHotel, durationDays, itinerary);

        // 6. Calculate split
        SplitResultDto split = fetchSplit(tripId, budget, request.getTravelers());

        log.info("Trip planning complete for tripId: {}", tripId);

        return buildTripResponse(trip, itinerary, selectedHotel, alternatives, budget, split, durationDays);
    }

    /**
     * Fetches a full trip by ID.
     */
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

        return buildTripResponse(trip, itinerary, null, List.of(), null, null, durationDays);
    }

    /**
     * Returns a list of trip summaries for a user.
     */
    @Transactional(readOnly = true)
    public List<TripSummaryDto> getUserTrips(Long userId) {
        return tripRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToSummary)
                .collect(Collectors.toList());
    }

    /**
     * Replans a trip with a new hotel — recalculates budget and split.
     */
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
                .map(this::mapDayToDto)
                .collect(Collectors.toList());

        // Re-fetch budget with new hotel
        TripCreateRequest fakeRequest = buildRequestFromTrip(trip);
        BudgetBreakdownDto budget = fetchBudget(fakeRequest, trip.getId(), null, durationDays, itinerary);
        SplitResultDto split = fetchSplit(trip.getId(), budget, trip.getTravelers());

        return buildTripResponse(trip, itinerary, null, List.of(), budget, split, durationDays);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private List<HotelDto> fetchHotels(TripCreateRequest req, int durationDays) {
        try {
            ApiResponse<List<HotelDto>> response = hotelServiceClient.recommendHotels(
                    req.getDestination(),
                    req.getTotalBudget(),
                    durationDays,
                    req.getTravelers(),
                    req.getHotelPreference() != null ? req.getHotelPreference().name() : "STANDARD"
            );
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

            ApiResponse<SplitResultDto> response = splitServiceClient.splitEqual(splitRequest);
            return response.getData();
        } catch (Exception e) {
            log.warn("Split service call failed: {}", e.getMessage());
            return null;
        }
    }

    private void persistItinerary(Trip trip, List<ItineraryDayDto> itinerary) {
        // Clear existing days
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

    private TripResponse buildTripResponse(Trip trip, List<ItineraryDayDto> itinerary,
                                            HotelDto selectedHotel, List<HotelDto> alternatives,
                                            BudgetBreakdownDto budget, SplitResultDto split,
                                            int durationDays) {
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
        if (trip.getInterests() != null && !trip.getInterests().isBlank()) {
            req.setInterests(Arrays.asList(trip.getInterests().split(",")));
        }
        return req;
    }
}
