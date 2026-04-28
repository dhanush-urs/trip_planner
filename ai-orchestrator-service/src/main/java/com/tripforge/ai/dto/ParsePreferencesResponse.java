package com.tripforge.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ParsePreferencesResponse {
    private String tripStyle;
    private String pace;
    private String budgetSensitivity;
    private List<String> hotelReasons;
    private List<String> interests;
    private List<String> warnings;
    private boolean fallbackUsed;
    @Builder.Default private String sourceProvider = "fallback";
}
