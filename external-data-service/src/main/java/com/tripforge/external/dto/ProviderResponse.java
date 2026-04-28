package com.tripforge.external.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Standard wrapper for all external-data-service responses.
 *
 * Every response includes:
 *   - sourceProvider: which provider actually served the data
 *   - fallbackUsed:   true if a fallback provider was used
 *   - degradedMode:   true if data quality is reduced (e.g. cached stale data)
 *   - warnings:       human-readable notes about data quality
 *   - data:           the actual payload
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderResponse<T> {

    private T data;
    private String sourceProvider;

    @Builder.Default
    private boolean fallbackUsed = false;

    @Builder.Default
    private boolean degradedMode = false;

    @Builder.Default
    private List<String> warnings = new ArrayList<>();

    public static <T> ProviderResponse<T> of(T data, String provider) {
        return ProviderResponse.<T>builder()
                .data(data)
                .sourceProvider(provider)
                .fallbackUsed(false)
                .degradedMode(false)
                .build();
    }

    public static <T> ProviderResponse<T> fallback(T data, String provider, String warning) {
        List<String> warnings = new ArrayList<>();
        warnings.add(warning);
        return ProviderResponse.<T>builder()
                .data(data)
                .sourceProvider(provider)
                .fallbackUsed(true)
                .degradedMode(false)
                .warnings(warnings)
                .build();
    }

    public static <T> ProviderResponse<T> degraded(T data, String provider, String warning) {
        List<String> warnings = new ArrayList<>();
        warnings.add(warning);
        return ProviderResponse.<T>builder()
                .data(data)
                .sourceProvider(provider)
                .fallbackUsed(true)
                .degradedMode(true)
                .warnings(warnings)
                .build();
    }
}
