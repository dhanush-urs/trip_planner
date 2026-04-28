package com.tripforge.external.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Normalized currency exchange rate DTO.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FxRateDto {

    /** Source currency code (e.g. "INR") */
    private String fromCurrency;

    /** Target currency code (e.g. "USD") */
    private String toCurrency;

    /** Exchange rate: 1 unit of fromCurrency = rate units of toCurrency */
    private BigDecimal rate;

    /** Converted amount (if amount was provided in the request) */
    private BigDecimal convertedAmount;

    /** Original amount */
    private BigDecimal originalAmount;

    /** Date the rate was fetched (ISO-8601 string, e.g. "2026-04-27") */
    private String rateDate;

    /** Provider that served this rate */
    private String sourceProvider;

    /** Whether this is a cached/stale rate */
    private boolean cachedRate;

    /** Whether this is a last-known fallback rate */
    private boolean fallbackRate;
}
