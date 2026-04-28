package com.tripforge.external.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Provider health status DTO.
 * Returned by GET /api/external/providers/health
 * Shows which providers are configured and reachable.
 * Never leaks API keys.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProviderHealthDto {

    private Map<String, ProviderStatus> providers;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProviderStatus {
        /** Whether the API key is configured (non-empty) */
        private boolean configured;
        /** Whether the provider is enabled in config */
        private boolean enabled;
        /** Last known reachability status */
        private String status;  // UP / DOWN / UNKNOWN
        /** Role: PRIMARY or FALLBACK */
        private String role;
        /** What this provider is used for */
        private String purpose;
    }
}
