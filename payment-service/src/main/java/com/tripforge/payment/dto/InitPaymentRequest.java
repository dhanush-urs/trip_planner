package com.tripforge.payment.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

/**
 * Request to initialize payment tracking for a trip.
 * Called by trip-service after trip creation to set up payment summary.
 */
@Data
public class InitPaymentRequest {
    private Long tripId;
    private BigDecimal totalAmount;
    private String currencyCode;
    private List<ParticipantInfo> participants;

    @Data
    public static class ParticipantInfo {
        private Long participantId;
        private String participantName;
        private String participantEmail;
        private BigDecimal allocatedAmount;
    }
}
