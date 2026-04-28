package com.tripforge.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TripSummaryResponse {
    private String headline;
    private String shortSummary;
    private boolean fallbackUsed;
    @Builder.Default private String sourceProvider = "fallback";
}
