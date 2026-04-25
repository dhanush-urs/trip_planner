package com.tripforge.trip.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * Request to replan a trip with a different hotel.
 */
@Data
public class ReplanRequest {

    @NotNull(message = "Trip ID is required")
    private Long tripId;

    @NotNull(message = "New hotel ID is required")
    private Long newHotelId;

    /** Reason for hotel change: CHEAPER, BETTER_RATING, CLOSER, PREMIUM */
    private String changeReason;
}
