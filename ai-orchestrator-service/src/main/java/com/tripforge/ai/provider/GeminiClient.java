package com.tripforge.ai.provider;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripforge.ai.config.GeminiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Gemini API client adapter.
 *
 * Responsibilities:
 *   - Build the Gemini generateContent request
 *   - Call the API with explicit timeout
 *   - Extract the text response
 *   - Parse JSON from the response text
 *   - Return Optional.empty() on any failure (never throws to callers)
 *
 * This class is the ONLY place that talks to Gemini.
 * All callers receive Optional<String> — they handle the empty case.
 */
@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    @Autowired private GeminiProperties props;
    @Autowired private WebClient geminiWebClient;
    @Autowired private ObjectMapper objectMapper;

    /**
     * Send a prompt to Gemini and return the raw text response.
     * Returns Optional.empty() on any failure — callers use fallback logic.
     *
     * @param prompt the full prompt string (should instruct Gemini to return JSON only)
     * @return Optional containing the response text, or empty on failure
     */
    public Optional<String> generate(String prompt) {
        if (!props.isConfigured()) {
            log.debug("Gemini not configured — skipping API call");
            return Optional.empty();
        }

        try {
            // Build Gemini generateContent request body
            Map<String, Object> requestBody = Map.of(
                    "contents", List.of(
                            Map.of("parts", List.of(
                                    Map.of("text", prompt)
                            ))
                    ),
                    "generationConfig", Map.of(
                            "temperature", props.getTemperature(),
                            "maxOutputTokens", 512,
                            "responseMimeType", "application/json"
                    )
            );

            String url = "/models/" + props.getModel() + ":generateContent?key=" + props.getApiKey();

            String responseBody = geminiWebClient.post()
                    .uri(url)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .timeout(Duration.ofMillis(props.getTimeoutMs()))
                    .block();

            if (responseBody == null) {
                log.warn("Gemini returned null response body");
                return Optional.empty();
            }

            // Extract text from Gemini response structure:
            // { "candidates": [{ "content": { "parts": [{ "text": "..." }] } }] }
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode text = root.path("candidates").path(0)
                    .path("content").path("parts").path(0).path("text");

            if (text.isMissingNode() || text.asText().isBlank()) {
                log.warn("Gemini response missing text content");
                return Optional.empty();
            }

            String rawText = text.asText().trim();
            log.debug("Gemini raw response (first 200 chars): {}",
                    rawText.length() > 200 ? rawText.substring(0, 200) + "..." : rawText);
            return Optional.of(rawText);

        } catch (Exception e) {
            log.warn("Gemini API call failed: {} — using fallback", e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Parse a JSON string from Gemini response into a target class.
     * Strips markdown code fences if present (Gemini sometimes wraps JSON in ```json).
     * Returns Optional.empty() if parsing fails.
     */
    public <T> Optional<T> parseJson(String jsonText, Class<T> targetClass) {
        try {
            // Strip markdown code fences if present
            String cleaned = jsonText.trim();
            if (cleaned.startsWith("```")) {
                cleaned = cleaned.replaceAll("^```[a-zA-Z]*\\n?", "").replaceAll("```$", "").trim();
            }
            return Optional.of(objectMapper.readValue(cleaned, targetClass));
        } catch (Exception e) {
            log.warn("Failed to parse Gemini JSON response: {} — text was: {}",
                    e.getMessage(), jsonText.length() > 100 ? jsonText.substring(0, 100) : jsonText);
            return Optional.empty();
        }
    }
}
