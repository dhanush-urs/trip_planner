package com.tripforge.trip.client;

import com.tripforge.trip.dto.ApiResponse;
import com.tripforge.trip.dto.ai.HotelExplanationRequest;
import com.tripforge.trip.dto.ai.HotelExplanationResponse;
import com.tripforge.trip.dto.ai.ItineraryExplanationRequest;
import com.tripforge.trip.dto.ai.ItineraryExplanationResponse;
import com.tripforge.trip.dto.ai.TripSummaryRequest;
import com.tripforge.trip.dto.ai.TripSummaryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for ai-orchestrator-service.
 *
 * All calls are wrapped in try/catch in TripService.
 * If ai-orchestrator-service is down, trip creation continues without AI enrichment.
 */
@FeignClient(name = "ai-orchestrator-service", path = "/api/ai")
public interface AiOrchestratorClient {

    @PostMapping("/explain-hotel-choice")
    ApiResponse<HotelExplanationResponse> explainHotelChoice(
            @RequestBody HotelExplanationRequest request);

    @PostMapping("/explain-itinerary")
    ApiResponse<ItineraryExplanationResponse> explainItinerary(
            @RequestBody ItineraryExplanationRequest request);

    @PostMapping("/summarize-trip")
    ApiResponse<TripSummaryResponse> summarizeTrip(
            @RequestBody TripSummaryRequest request);
}
