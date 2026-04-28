package com.tripforge.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripforge.ai.dto.*;
import com.tripforge.ai.provider.GeminiClient;
import com.tripforge.ai.provider.GeminiPromptBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Core AI orchestration service.
 *
 * Pattern for every method:
 *   1. Build prompt via GeminiPromptBuilder
 *   2. Call GeminiClient.generate() → Optional<String>
 *   3. If present: parse JSON → return Gemini result
 *   4. If empty (any failure): return FallbackAiService result
 *
 * Trip creation NEVER fails because of this service.
 * All methods are safe to call even with no Gemini key.
 */
@Service
public class AiOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AiOrchestratorService.class);

    @Autowired private GeminiClient geminiClient;
    @Autowired private GeminiPromptBuilder promptBuilder;
    @Autowired private FallbackAiService fallback;
    @Autowired private ObjectMapper objectMapper;

    // ── Parse Preferences ─────────────────────────────────────────────────────

    public ParsePreferencesResponse parsePreferences(ParsePreferencesRequest req) {
        log.info("parsePreferences: destination={}", req.getDestination());

        String prompt = promptBuilder.buildParsePreferencesPrompt(req);
        Optional<String> geminiText = geminiClient.generate(prompt);

        if (geminiText.isPresent()) {
            try {
                JsonNode node = objectMapper.readTree(cleanJson(geminiText.get()));
                ParsePreferencesResponse result = ParsePreferencesResponse.builder()
                        .tripStyle(textOrDefault(node, "tripStyle", "BALANCED"))
                        .pace(textOrDefault(node, "pace", "BALANCED"))
                        .budgetSensitivity(textOrDefault(node, "budgetSensitivity", "MEDIUM"))
                        .hotelReasons(listOrDefault(node, "hotelReasons"))
                        .interests(listOrDefault(node, "interests"))
                        .fallbackUsed(false)
                        .sourceProvider("gemini")
                        .build();
                log.info("parsePreferences: Gemini success");
                return result;
            } catch (Exception e) {
                log.warn("parsePreferences: Gemini JSON parse failed — using fallback: {}", e.getMessage());
            }
        }

        log.info("parsePreferences: using keyword fallback");
        return fallback.parsePreferences(req.getText());
    }

    // ── Hotel Explanation ─────────────────────────────────────────────────────

    public HotelExplanationResponse explainHotelChoice(HotelExplanationRequest req) {
        log.info("explainHotelChoice: hotel={}", req.getHotelName());

        String prompt = promptBuilder.buildHotelExplanationPrompt(req);
        Optional<String> geminiText = geminiClient.generate(prompt);

        if (geminiText.isPresent()) {
            try {
                JsonNode node = objectMapper.readTree(cleanJson(geminiText.get()));
                HotelExplanationResponse result = HotelExplanationResponse.builder()
                        .summary(textOrDefault(node, "summary", "Selected based on rating and location."))
                        .bullets(listOrDefault(node, "bullets"))
                        .fallbackUsed(false)
                        .sourceProvider("gemini")
                        .build();
                log.info("explainHotelChoice: Gemini success");
                return result;
            } catch (Exception e) {
                log.warn("explainHotelChoice: Gemini JSON parse failed — using fallback: {}", e.getMessage());
            }
        }

        log.info("explainHotelChoice: using template fallback");
        return fallback.explainHotelChoice(req);
    }

    // ── Itinerary Explanation ─────────────────────────────────────────────────

    public ItineraryExplanationResponse explainItinerary(ItineraryExplanationRequest req) {
        log.info("explainItinerary: destination={} days={}", req.getDestination(), req.getDays());

        String prompt = promptBuilder.buildItineraryExplanationPrompt(req);
        Optional<String> geminiText = geminiClient.generate(prompt);

        if (geminiText.isPresent()) {
            try {
                JsonNode node = objectMapper.readTree(cleanJson(geminiText.get()));
                ItineraryExplanationResponse result = ItineraryExplanationResponse.builder()
                        .summary(textOrDefault(node, "summary", "Route optimized for your interests and pace."))
                        .bullets(listOrDefault(node, "bullets"))
                        .fallbackUsed(false)
                        .sourceProvider("gemini")
                        .build();
                log.info("explainItinerary: Gemini success");
                return result;
            } catch (Exception e) {
                log.warn("explainItinerary: Gemini JSON parse failed — using fallback: {}", e.getMessage());
            }
        }

        log.info("explainItinerary: using template fallback");
        return fallback.explainItinerary(req);
    }

    // ── Replan Feedback ───────────────────────────────────────────────────────

    public ReplanFeedbackResponse interpretReplanFeedback(ReplanFeedbackRequest req) {
        log.info("interpretReplanFeedback: text length={}", req.getText().length());

        String prompt = promptBuilder.buildReplanFeedbackPrompt(req);
        Optional<String> geminiText = geminiClient.generate(prompt);

        if (geminiText.isPresent()) {
            try {
                JsonNode node = objectMapper.readTree(cleanJson(geminiText.get()));
                String primary = textOrDefault(node, "primaryReason", "BETTER_RATING");
                // Validate primary is a known reason
                if (!isValidReplanReason(primary)) primary = "BETTER_RATING";

                List<String> secondary = listOrDefault(node, "secondaryReasons");
                secondary.removeIf(r -> !isValidReplanReason(r));

                ReplanFeedbackResponse result = ReplanFeedbackResponse.builder()
                        .primaryReason(primary)
                        .secondaryReasons(secondary)
                        .fallbackUsed(false)
                        .sourceProvider("gemini")
                        .build();
                log.info("interpretReplanFeedback: Gemini success, primary={}", primary);
                return result;
            } catch (Exception e) {
                log.warn("interpretReplanFeedback: Gemini JSON parse failed — using fallback: {}", e.getMessage());
            }
        }

        log.info("interpretReplanFeedback: using keyword fallback");
        return fallback.interpretReplanFeedback(req.getText());
    }

    // ── Trip Summary ──────────────────────────────────────────────────────────

    public TripSummaryResponse summarizeTrip(TripSummaryRequest req) {
        log.info("summarizeTrip: destination={} days={}", req.getDestination(), req.getDays());

        String prompt = promptBuilder.buildTripSummaryPrompt(req);
        Optional<String> geminiText = geminiClient.generate(prompt);

        if (geminiText.isPresent()) {
            try {
                JsonNode node = objectMapper.readTree(cleanJson(geminiText.get()));
                TripSummaryResponse result = TripSummaryResponse.builder()
                        .headline(textOrDefault(node, "headline", "Your trip is ready."))
                        .shortSummary(textOrDefault(node, "shortSummary", "A well-planned trip awaits."))
                        .fallbackUsed(false)
                        .sourceProvider("gemini")
                        .build();
                log.info("summarizeTrip: Gemini success");
                return result;
            } catch (Exception e) {
                log.warn("summarizeTrip: Gemini JSON parse failed — using fallback: {}", e.getMessage());
            }
        }

        log.info("summarizeTrip: using template fallback");
        return fallback.summarizeTrip(req);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String cleanJson(String text) {
        String cleaned = text.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
        }
        return cleaned;
    }

    private String textOrDefault(JsonNode node, String field, String defaultVal) {
        JsonNode n = node.path(field);
        return (!n.isMissingNode() && !n.isNull() && !n.asText().isBlank())
                ? n.asText() : defaultVal;
    }

    private List<String> listOrDefault(JsonNode node, String field) {
        JsonNode arr = node.path(field);
        List<String> result = new ArrayList<>();
        if (arr.isArray()) {
            arr.forEach(item -> {
                if (!item.asText().isBlank()) result.add(item.asText());
            });
        }
        return result;
    }

    private boolean isValidReplanReason(String reason) {
        return List.of("CHEAPER", "BETTER_RATING", "CLOSER", "PREMIUM",
                "FAMILY_FRIENDLY", "NIGHTLIFE_FRIENDLY", "QUIETER").contains(reason);
    }
}
