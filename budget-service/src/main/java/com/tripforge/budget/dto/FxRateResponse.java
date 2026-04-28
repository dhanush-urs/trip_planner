package com.tripforge.budget.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.math.BigDecimal;

/**
 * Mirrors the ProviderResponse<FxRateDto> shape from external-data-service.
 * Uses @JsonIgnoreProperties for resilience against future field additions.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class FxRateResponse {

    private FxRateData data;
    private String sourceProvider;
    private boolean fallbackUsed;
    private boolean degradedMode;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class FxRateData {
        private String fromCurrency;
        private String toCurrency;
        private BigDecimal rate;
        private BigDecimal convertedAmount;
        private BigDecimal originalAmount;
        private boolean cachedRate;
        private boolean fallbackRate;
    }
}
