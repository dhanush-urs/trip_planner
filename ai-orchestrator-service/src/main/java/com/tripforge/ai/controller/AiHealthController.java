package com.tripforge.ai.controller;

import com.tripforge.ai.config.GeminiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI service health endpoint.
 * GET /api/ai/health
 *
 * Returns Gemini configuration status without leaking the API key.
 */
@RestController
@RequestMapping("/api/ai")
public class AiHealthController {

    @Autowired
    private GeminiProperties geminiProps;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> status = new LinkedHashMap<>();
        status.put("status", "UP");
        status.put("service", "ai-orchestrator-service");

        Map<String, Object> gemini = new LinkedHashMap<>();
        gemini.put("enabled", geminiProps.isEnabled());
        gemini.put("configured", geminiProps.isConfigured());  // true/false only — no key leak
        gemini.put("model", geminiProps.getModel());
        gemini.put("mode", geminiProps.isConfigured() ? "LIVE" : "FALLBACK");
        gemini.put("note", geminiProps.isConfigured()
                ? "Gemini API key configured — live AI responses active"
                : "No Gemini API key — all endpoints use deterministic fallback");
        status.put("gemini", gemini);

        status.put("fallbackAvailable", true);
        status.put("degradedMode", !geminiProps.isConfigured());

        return ResponseEntity.ok(status);
    }
}
