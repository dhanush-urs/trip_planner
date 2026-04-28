package com.tripforge.ai.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/** Metadata about which AI provider served the response. */
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class AiProviderMeta {
    @Builder.Default private String sourceProvider = "fallback";
    @Builder.Default private boolean fallbackUsed = true;
    private List<String> warnings;

    public static AiProviderMeta gemini() {
        return AiProviderMeta.builder().sourceProvider("gemini").fallbackUsed(false).build();
    }
    public static AiProviderMeta fallback(String reason) {
        return AiProviderMeta.builder().sourceProvider("fallback")
                .fallbackUsed(true).warnings(List.of(reason)).build();
    }
}
