package com.tripforge.trip.dto.payment;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PaymentSummaryDto {
    private Long tripId;
    private String currencyCode;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal amountRemaining;
    private String status;
    private List<ParticipantPaymentDto> participants;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ParticipantPaymentDto {
        private Long participantId;
        private String participantName;
        private String participantEmail;
        private BigDecimal allocatedAmount;
        private BigDecimal paidAmount;
        private String currencyCode;
        private String status;
        private String paymentLink;
    }
}
