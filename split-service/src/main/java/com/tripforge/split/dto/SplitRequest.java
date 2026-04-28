package com.tripforge.split.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Typed request DTO for all split endpoints.
 * Replaces the raw Map<String,Object> pattern for Phase 9F.
 */
@Data
public class SplitRequest {

    @NotNull(message = "tripId is required")
    private Long tripId;

    @NotNull(message = "totalAmount is required")
    private BigDecimal totalAmount;

    private String currencyCode = "INR";

    /** Participant list — required for custom splits, optional for equal split */
    private List<ParticipantInput> participants;

    @Data
    public static class ParticipantInput {
        private Long participantId;
        private String participantName;
        private String participantEmail;
        /** Used for CUSTOM_PERCENTAGE split */
        private Double percentage;
        /** Used for CUSTOM_AMOUNT split */
        private BigDecimal amount;
    }
}
