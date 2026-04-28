package com.tripforge.payment.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CreateOrderResponse {
    private String gatewayProvider;
    private String paymentType;
    private Long tripId;
    private Long transactionId;
    private String orderId;
    private BigDecimal amount;
    private String currency;
    private long displayAmountMinor;
    /** Public key only — never the secret */
    private String keyId;
    private String participantName;
    private Long participantId;
}
