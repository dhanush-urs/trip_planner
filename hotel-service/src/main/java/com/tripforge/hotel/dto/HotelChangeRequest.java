package com.tripforge.hotel.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request to change a hotel with a stated reason.
 * Reason drives the ML re-ranking weights.
 */
@Data
public class HotelChangeRequest {

    @NotNull(message = "Trip ID is required")
    private Long tripId;

    @NotNull(message = "Current hotel ID is required")
    private Long currentHotelId;

    /**
     * Reason for change: CHEAPER, BETTER_RATING, CLOSER, PREMIUM
     */
    @NotBlank(message = "Change reason is required")
    private String reason;

    private String destination;
    private Double budget;
    private Integer durationDays;
    private Integer travelers;
}
