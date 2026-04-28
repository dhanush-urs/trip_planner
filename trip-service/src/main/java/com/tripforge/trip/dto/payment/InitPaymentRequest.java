package com.tripforge.trip.dto.payment;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder
public class InitPaymentRequest {
    private Long tripId;
    private BigDecimal totalAmount;
    private String currencyCode;
    private List<ParticipantInfo> participants;

    @Data @Builder
    public static class ParticipantInfo {
        private Long participantId;
        private String participantName;
        private String participantEmail;
        private BigDecimal allocatedAmount;
    }
}
