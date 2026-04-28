package com.tripforge.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PaymentSummaryDto {
    private Long tripId;
    private String currencyCode;
    private BigDecimal totalAmount;
    private BigDecimal amountPaid;
    private BigDecimal amountRemaining;
    /** UNPAID / PARTIALLY_PAID / FULLY_PAID */
    private String status;
    private List<ParticipantPaymentDto> participants;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParticipantPaymentDto {
        private Long participantId;
        private String participantName;
        private String participantEmail;
        private BigDecimal allocatedAmount;
        private BigDecimal paidAmount;
        private String currencyCode;
        /** UNPAID / PARTIAL / PAID */
        private String status;
        private String paymentLink;
    }
}
