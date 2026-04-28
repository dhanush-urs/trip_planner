package com.tripforge.ai.controller;

import com.tripforge.ai.dto.*;
import com.tripforge.ai.service.AiOrchestratorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * AI Orchestrator REST controller.
 * All endpoints return success even when Gemini is unavailable (fallback mode).
 */
@RestController
@RequestMapping("/api/ai")
public class AiController {

    @Autowired
    private AiOrchestratorService aiService;

    /**
     * POST /api/ai/parse-preferences
     * Parse free-form user text into structured trip constraints.
     */
    @PostMapping("/parse-preferences")
    public ResponseEntity<ApiResponse<ParsePreferencesResponse>> parsePreferences(
            @Valid @RequestBody ParsePreferencesRequest request) {
        ParsePreferencesResponse result = aiService.parsePreferences(request);
        return ResponseEntity.ok(ApiResponse.success("Preferences parsed", result));
    }

    /**
     * POST /api/ai/explain-hotel-choice
     * Explain why a hotel was selected.
     */
    @PostMapping("/explain-hotel-choice")
    public ResponseEntity<ApiResponse<HotelExplanationResponse>> explainHotelChoice(
            @RequestBody HotelExplanationRequest request) {
        HotelExplanationResponse result = aiService.explainHotelChoice(request);
        return ResponseEntity.ok(ApiResponse.success("Hotel explanation generated", result));
    }

    /**
     * POST /api/ai/explain-itinerary
     * Explain why the itinerary sequence makes sense.
     */
    @PostMapping("/explain-itinerary")
    public ResponseEntity<ApiResponse<ItineraryExplanationResponse>> explainItinerary(
            @RequestBody ItineraryExplanationRequest request) {
        ItineraryExplanationResponse result = aiService.explainItinerary(request);
        return ResponseEntity.ok(ApiResponse.success("Itinerary explanation generated", result));
    }

    /**
     * POST /api/ai/replan-hotel-feedback
     * Convert free-form hotel rejection text into canonical replan reasons.
     */
    @PostMapping("/replan-hotel-feedback")
    public ResponseEntity<ApiResponse<ReplanFeedbackResponse>> replanFeedback(
            @Valid @RequestBody ReplanFeedbackRequest request) {
        ReplanFeedbackResponse result = aiService.interpretReplanFeedback(request);
        return ResponseEntity.ok(ApiResponse.success("Replan reason interpreted", result));
    }

    /**
     * POST /api/ai/summarize-trip
     * Generate a concise trip summary.
     */
    @PostMapping("/summarize-trip")
    public ResponseEntity<ApiResponse<TripSummaryResponse>> summarizeTrip(
            @RequestBody TripSummaryRequest request) {
        TripSummaryResponse result = aiService.summarizeTrip(request);
        return ResponseEntity.ok(ApiResponse.success("Trip summary generated", result));
    }
}
