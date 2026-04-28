package com.tripforge.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItineraryExplanationResponse {
    private String summary;
    private List<String> bullets;
    private boolean fallbackUsed;
    @Builder.Default private String sourceProvider = "fallback";
}
