package com.tripforge.trip.dto;

import com.tripforge.trip.entity.Trip;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Request DTO for creating a new trip.
 */
@Data
public class TripCreateRequest {

    @NotBlank(message = "Destination is required")
    @Size(max = 100)
    private String destination;

    @NotNull(message = "Start date is required")
    @FutureOrPresent(message = "Start date must be today or in the future")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    private LocalDate endDate;

    @NotNull(message = "Budget is required")
    @DecimalMin(value = "1000.0", message = "Budget must be at least ₹1000")
    private BigDecimal totalBudget;

    @NotNull(message = "Number of travelers is required")
    @Min(value = 1, message = "At least 1 traveler required")
    @Max(value = 20, message = "Maximum 20 travelers supported")
    private Integer travelers;

    private List<String> interests;

    private Trip.HotelPreference hotelPreference;

    @AssertTrue(message = "End date must be after start date")
    public boolean isDateRangeValid() {
        if (startDate == null || endDate == null) return true;
        return endDate.isAfter(startDate);
    }
}
