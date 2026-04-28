package com.tripforge.ai.provider;

import com.tripforge.ai.dto.*;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds structured prompts for Gemini.
 *
 * Rules:
 *   - Every prompt ends with "Return ONLY valid JSON. No markdown. No explanation."
 *   - Low temperature is set in GeminiClient (0.1) for determinism
 *   - Prompts are concise — Gemini 1.5 Flash handles short prompts well
 *   - JSON schema is always specified inline so Gemini knows the exact output shape
 */
@Component
public class GeminiPromptBuilder {

    public String buildParsePreferencesPrompt(ParsePreferencesRequest req) {
        return """
                You are a travel preference parser. Extract structured travel preferences from the user's text.
                
                Destination: %s
                User text: "%s"
                
                Return ONLY this JSON (no markdown, no explanation):
                {
                  "tripStyle": "<RELAXED|BALANCED|PACKED>",
                  "pace": "<RELAXED|BALANCED|PACKED>",
                  "budgetSensitivity": "<LOW|MEDIUM|HIGH>",
                  "hotelReasons": ["<CHEAPER|BETTER_RATING|CLOSER|PREMIUM|FAMILY_FRIENDLY|NIGHTLIFE_FRIENDLY|QUIETER>"],
                  "interests": ["<beaches|food|nightlife|nature|temples|shopping|adventure>"]
                }
                
                Rules:
                - tripStyle: RELAXED=chill/slow, BALANCED=moderate, PACKED=see everything
                - pace: same scale as tripStyle
                - budgetSensitivity: LOW=budget-conscious, MEDIUM=moderate, HIGH=willing to spend
                - hotelReasons: pick 1-3 that match the text
                - interests: pick 1-5 that match the text
                
                Return ONLY valid JSON. No markdown. No explanation.
                """.formatted(
                req.getDestination() != null ? req.getDestination() : "unspecified",
                req.getText());
    }

    public String buildHotelExplanationPrompt(HotelExplanationRequest req) {
        String reasons = req.getReasons() != null ? String.join(", ", req.getReasons()) : "general fit";
        return """
                You are a travel assistant. Write a concise explanation for why this hotel was selected.
                
                Hotel: %s
                Destination: %s
                Rating: %.1f/5
                Area: %s
                Distance from attractions: %.1f km
                Budget: %s
                Travelers: %d
                Selection reasons: %s
                Data mode: %s
                
                Return ONLY this JSON (no markdown, no explanation):
                {
                  "summary": "<one sentence, max 25 words>",
                  "bullets": ["<reason 1>", "<reason 2>", "<reason 3>"]
                }
                
                Keep bullets factual, concise, and specific to the data provided.
                Return ONLY valid JSON. No markdown. No explanation.
                """.formatted(
                req.getHotelName() != null ? req.getHotelName() : "Selected Hotel",
                req.getDestination() != null ? req.getDestination() : "destination",
                req.getRating() != null ? req.getRating() : 4.0,
                req.getAreaName() != null ? req.getAreaName() : "city center",
                req.getDistanceFromTripCentroid() != null ? req.getDistanceFromTripCentroid() : 2.0,
                req.getBudget() != null ? req.getBudget().toString() : "moderate",
                req.getTravelers() != null ? req.getTravelers() : 2,
                reasons,
                req.getProviderMode() != null ? req.getProviderMode() : "MIXED");
    }

    public String buildItineraryExplanationPrompt(ItineraryExplanationRequest req) {
        String placesList = req.getPlaces() != null
                ? req.getPlaces().stream()
                        .map(p -> p.getName() + " (" + p.getCategory() + ")")
                        .collect(Collectors.joining(", "))
                : "various attractions";

        return """
                You are a travel planner. Explain why this itinerary sequence makes sense.
                
                Destination: %s
                Duration: %d days
                Trip style: %s
                Pace: %s
                Places: %s
                Data mode: %s
                
                Return ONLY this JSON (no markdown, no explanation):
                {
                  "summary": "<one sentence, max 30 words>",
                  "bullets": ["<reason 1>", "<reason 2>", "<reason 3>"]
                }
                
                Focus on: geographic grouping, reduced travel time, interest alignment, pacing.
                Return ONLY valid JSON. No markdown. No explanation.
                """.formatted(
                req.getDestination() != null ? req.getDestination() : "destination",
                req.getDays() != null ? req.getDays() : 3,
                req.getTripStyle() != null ? req.getTripStyle() : "BALANCED",
                req.getPace() != null ? req.getPace() : "BALANCED",
                placesList,
                req.getProviderMode() != null ? req.getProviderMode() : "MIXED");
    }

    public String buildReplanFeedbackPrompt(ReplanFeedbackRequest req) {
        return """
                You are a hotel preference classifier. Map the user's feedback to canonical hotel change reasons.
                
                Valid reasons: CHEAPER, BETTER_RATING, CLOSER, PREMIUM, FAMILY_FRIENDLY, NIGHTLIFE_FRIENDLY, QUIETER
                
                User feedback: "%s"
                
                Return ONLY this JSON (no markdown, no explanation):
                {
                  "primaryReason": "<one of the valid reasons>",
                  "secondaryReasons": ["<optional additional reasons, max 2>"]
                }
                
                Rules:
                - primaryReason must be exactly one of the valid reasons
                - secondaryReasons can be empty array []
                - Map "expensive/costly/pricey" → CHEAPER
                - Map "far/distance/location" → CLOSER
                - Map "bad rating/reviews" → BETTER_RATING
                - Map "luxury/upgrade/premium" → PREMIUM
                - Map "family/kids/children" → FAMILY_FRIENDLY
                - Map "nightlife/party/bars" → NIGHTLIFE_FRIENDLY
                - Map "noisy/crowded/busy" → QUIETER
                
                Return ONLY valid JSON. No markdown. No explanation.
                """.formatted(req.getText());
    }

    public String buildTripSummaryPrompt(TripSummaryRequest req) {
        String places = req.getTopPlaces() != null
                ? String.join(", ", req.getTopPlaces()) : "local attractions";

        return """
                You are a travel copywriter. Write a concise, polished trip summary.
                
                Destination: %s
                Duration: %d days
                Hotel: %s
                Budget: %s %s
                Trip style: %s
                Pace: %s
                Top places: %s
                Data quality: %s
                
                Return ONLY this JSON (no markdown, no explanation):
                {
                  "headline": "<one punchy sentence, max 15 words>",
                  "shortSummary": "<two sentences max, 40 words max, factual and specific>"
                }
                
                Keep it factual, specific, and product-grade. No fluff.
                Return ONLY valid JSON. No markdown. No explanation.
                """.formatted(
                req.getDestination() != null ? req.getDestination() : "destination",
                req.getDays() != null ? req.getDays() : 3,
                req.getHotelName() != null ? req.getHotelName() : "selected hotel",
                req.getBudget() != null ? req.getBudget().toString() : "moderate",
                req.getCurrency() != null ? req.getCurrency() : "INR",
                req.getTripStyle() != null ? req.getTripStyle() : "BALANCED",
                req.getPace() != null ? req.getPace() : "BALANCED",
                places,
                req.getProviderMode() != null ? req.getProviderMode() : "MIXED");
    }
}
