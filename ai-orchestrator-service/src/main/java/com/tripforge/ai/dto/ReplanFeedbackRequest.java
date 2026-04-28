package com.tripforge.ai.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ReplanFeedbackRequest {
    @NotBlank(message = "Feedback text is required")
    private String text;
}
