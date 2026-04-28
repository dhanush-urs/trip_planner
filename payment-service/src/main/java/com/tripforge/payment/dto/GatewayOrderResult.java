package com.tripforge.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GatewayOrderResult {
    private String orderId;
    private String currency;
    private BigDecimal amount;
    /** Amount in minor units (paise for INR, cents for USD) for Razorpay checkout */
    private long amountMinor;
    private String receipt;
    private String status;
    private String gatewayProvider;
}
