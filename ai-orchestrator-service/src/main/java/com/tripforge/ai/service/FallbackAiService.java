package com.tripforge.ai.service;

import com.tripforge.ai.dto.*;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Deterministic fallback AI service.
 *
 * Used when Gemini is unavailable, not configured, or returns unparseable output.
 * Every method returns a valid, sensible response — never null, never throws.
 *
 * This is the safety net that ensures trip creation NEVER fails due to AI unavailability.
 */
@Component
public class FallbackAiService {

    private static final String FALLBACK_PROVIDER = "keyword_fallback";

    // ── Parse Preferences ─────────────────────────────────────────────────────

    public ParsePreferencesResponse parsePreferences(String text) {
        String lower = text != null ? text.toLowerCase() : "";

        String tripStyle = detectTripStyle(lower);
        String pace = detectPace(lower);
        String budgetSensitivity = detectBudgetSensitivity(lower);
        List<String> hotelReasons = detectHotelReasons(lower);
        List<String> interests = detectInterests(lower);

        return ParsePreferencesResponse.builder()
                .tripStyle(tripStyle)
                .pace(pace)
                .budgetSensitivity(budgetSensitivity)
                .hotelReasons(hotelReasons)
                .interests(interests)
                .fallbackUsed(true)
                .sourceProvider(FALLBACK_PROVIDER)
                .build();
    }

    // ── Hotel Explanation ─────────────────────────────────────────────────────

    public HotelExplanationResponse explainHotelChoice(HotelExplanationRequest req) {
        String hotelName = req.getHotelName() != null ? req.getHotelName() : "this hotel";
        double rating = req.getRating() != null ? req.getRating() : 4.0;
        double distance = req.getDistanceFromTripCentroid() != null ? req.getDistanceFromTripCentroid() : 2.0;

        String summary = String.format(
                "Selected for its %.1f/5 rating, %s location, and fit within your budget.",
                rating,
                distance < 2.0 ? "central" : distance < 5.0 ? "convenient" : "accessible");

        List<String> bullets = new ArrayList<>();
        bullets.add(String.format("Rating of %.1f/5 — above average for the destination", rating));
        if (distance < 3.0) {
            bullets.add("Close to key attractions, reducing daily travel time");
        } else {
            bullets.add("Accessible location with good transport links");
        }
        if (req.getHotelPreference() != null) {
            bullets.add(String.format("Matches your %s hotel preference", req.getHotelPreference().toLowerCase()));
        } else {
            bullets.add("Good balance of comfort and value");
        }

        return HotelExplanationResponse.builder()
                .summary(summary)
                .bullets(bullets)
                .fallbackUsed(true)
                .sourceProvider(FALLBACK_PROVIDER)
                .build();
    }

    // ── Itinerary Explanation ─────────────────────────────────────────────────

    public ItineraryExplanationResponse explainItinerary(ItineraryExplanationRequest req) {
        int days = req.getDays() != null ? req.getDays() : 3;
        String pace = req.getPace() != null ? req.getPace() : "BALANCED";

        String summary = String.format(
                "The %d-day plan groups nearby stops together and maintains a %s pace throughout.",
                days, pace.toLowerCase());

        List<String> bullets = List.of(
                "Nearby places grouped to reduce unnecessary travel between stops",
                "High-interest attractions prioritized in the first half of the trip",
                "Pacing adjusted to match your " + pace.toLowerCase() + " travel style"
        );

        return ItineraryExplanationResponse.builder()
                .summary(summary)
                .bullets(bullets)
                .fallbackUsed(true)
                .sourceProvider(FALLBACK_PROVIDER)
                .build();
    }

    // ── Replan Feedback ───────────────────────────────────────────────────────

    public ReplanFeedbackResponse interpretReplanFeedback(String text) {
        String lower = text != null ? text.toLowerCase() : "";
        String primary = "BETTER_RATING"; // safe default
        List<String> secondary = new ArrayList<>();

        if (containsAny(lower, "expensive", "costly", "pricey", "cheap", "budget", "afford", "price")) {
            primary = "CHEAPER";
        } else if (containsAny(lower, "far", "distance", "location", "close", "near", "center")) {
            primary = "CLOSER";
        } else if (containsAny(lower, "luxury", "premium", "upgrade", "better", "nicer", "upscale")) {
            primary = "PREMIUM";
        } else if (containsAny(lower, "family", "kids", "children", "child")) {
            primary = "FAMILY_FRIENDLY";
        } else if (containsAny(lower, "nightlife", "party", "bar", "club", "night")) {
            primary = "NIGHTLIFE_FRIENDLY";
        } else if (containsAny(lower, "quiet", "peaceful", "noisy", "crowd", "busy")) {
            primary = "QUIETER";
        } else if (containsAny(lower, "rating", "review", "score", "quality")) {
            primary = "BETTER_RATING";
        }

        // Secondary reasons
        if (!primary.equals("CHEAPER") && containsAny(lower, "expensive", "price", "cost")) {
            secondary.add("CHEAPER");
        }
        if (!primary.equals("CLOSER") && containsAny(lower, "far", "distance", "location")) {
            secondary.add("CLOSER");
        }

        return ReplanFeedbackResponse.builder()
                .primaryReason(primary)
                .secondaryReasons(secondary)
                .fallbackUsed(true)
                .sourceProvider(FALLBACK_PROVIDER)
                .build();
    }

    // ── Trip Summary ──────────────────────────────────────────────────────────

    public TripSummaryResponse summarizeTrip(TripSummaryRequest req) {
        String destination = req.getDestination() != null ? req.getDestination() : "your destination";
        int days = req.getDays() != null ? req.getDays() : 3;
        String style = req.getTripStyle() != null ? req.getTripStyle().toLowerCase() : "balanced";
        String hotel = req.getHotelName() != null ? req.getHotelName() : "a well-rated hotel";

        String headline = String.format("A %s %d-day %s getaway with smart planning.",
                style, days, destination);

        String shortSummary = String.format(
                "Staying at %s, this plan covers your top interests with an efficient route. " +
                "Budget-aware choices keep costs in check while maximising your experience.",
                hotel);

        return TripSummaryResponse.builder()
                .headline(headline)
                .shortSummary(shortSummary)
                .fallbackUsed(true)
                .sourceProvider(FALLBACK_PROVIDER)
                .build();
    }

    // ── Private keyword helpers ───────────────────────────────────────────────

    private String detectTripStyle(String text) {
        if (containsAny(text, "relaxed", "chill", "slow", "easy", "lazy", "leisure")) return "RELAXED";
        if (containsAny(text, "packed", "everything", "maximum", "cover all", "busy")) return "PACKED";
        return "BALANCED";
    }

    private String detectPace(String text) {
        if (containsAny(text, "slow", "relaxed", "easy", "chill")) return "RELAXED";
        if (containsAny(text, "fast", "packed", "rush", "maximum")) return "PACKED";
        return "BALANCED";
    }

    private String detectBudgetSensitivity(String text) {
        if (containsAny(text, "cheap", "budget", "affordable", "low cost", "save money")) return "LOW";
        if (containsAny(text, "luxury", "premium", "splurge", "expensive", "high end")) return "HIGH";
        return "MEDIUM";
    }

    private List<String> detectHotelReasons(String text) {
        List<String> reasons = new ArrayList<>();
        if (containsAny(text, "nightlife", "party", "bar", "club")) reasons.add("NIGHTLIFE_FRIENDLY");
        if (containsAny(text, "quiet", "peaceful", "not crowded", "calm")) reasons.add("QUIETER");
        if (containsAny(text, "family", "kids", "children")) reasons.add("FAMILY_FRIENDLY");
        if (containsAny(text, "luxury", "premium", "upscale")) reasons.add("PREMIUM");
        if (containsAny(text, "cheap", "budget", "affordable")) reasons.add("CHEAPER");
        if (reasons.isEmpty()) reasons.add("BETTER_RATING");
        return reasons;
    }

    private List<String> detectInterests(String text) {
        List<String> interests = new ArrayList<>();
        if (containsAny(text, "beach", "sea", "ocean", "coast")) interests.add("beaches");
        if (containsAny(text, "food", "eat", "restaurant", "cuisine", "dining")) interests.add("food");
        if (containsAny(text, "nightlife", "party", "bar", "club", "night")) interests.add("nightlife");
        if (containsAny(text, "nature", "outdoor", "hike", "trek", "forest")) interests.add("nature");
        if (containsAny(text, "temple", "heritage", "culture", "history", "monument")) interests.add("temples");
        if (containsAny(text, "shop", "market", "mall", "buy")) interests.add("shopping");
        if (containsAny(text, "adventure", "sport", "thrill", "extreme")) interests.add("adventure");
        if (interests.isEmpty()) interests.add("nature");
        return interests;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String kw : keywords) {
            if (text.contains(kw)) return true;
        }
        return false;
    }
}
