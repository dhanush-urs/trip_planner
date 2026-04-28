package com.tripforge.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ParsePreferencesRequest {
    private String destination;
    @NotBlank(message = "Preference text is required")
    private String text;
}
