package com.tripforge.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class GatewayVerifyResult {
    private boolean success;
    private String message;
    private String gatewayPaymentId;
    private String gatewayOrderId;
    private String status;
}
