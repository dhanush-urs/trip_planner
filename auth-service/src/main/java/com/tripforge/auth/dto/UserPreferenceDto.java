package com.tripforge.auth.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for reading and updating user travel preferences.
 *
 * Sample request (PUT /api/users/preferences):
 * {
 *   "interests": ["nature", "food", "beaches"],
 *   "hotelPreference": "STANDARD",
 *   "defaultBudget": 50000.00,
 *   "defaultTravelers": 2
 * }
 *
 * Sample response (GET /api/users/preferences):
 * {
 *   "interests": ["nature", "food", "beaches"],
 *   "hotelPreference": "STANDARD",
 *   "defaultBudget": 50000.00,
 *   "defaultTravelers": 2
 * }
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserPreferenceDto {

    private List<String> interests;

    @Pattern(regexp = "BUDGET|STANDARD|LUXURY",
             message = "Hotel preference must be BUDGET, STANDARD, or LUXURY")
    private String hotelPreference;

    @DecimalMin(value = "0.0", inclusive = false, message = "Budget must be positive")
    private BigDecimal defaultBudget;

    @Min(value = 1, message = "Travelers must be at least 1")
    private Integer defaultTravelers;
}
