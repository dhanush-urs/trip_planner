package com.tripforge.payment.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateOrderRequest {
    @NotNull private Long tripId;
    @NotNull private Long userId;
    private String currency = "INR";
    // For share payments
    private Long participantId;
}
