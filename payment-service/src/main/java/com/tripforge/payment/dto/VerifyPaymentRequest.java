package com.tripforge.payment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VerifyPaymentRequest {
    @NotNull  private Long tripId;
    @NotNull  private Long transactionId;
    @NotBlank private String gatewayOrderId;
    @NotBlank private String gatewayPaymentId;
    @NotBlank private String gatewaySignature;
}
