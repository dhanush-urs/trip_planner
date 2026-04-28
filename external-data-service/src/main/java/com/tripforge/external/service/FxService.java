package com.tripforge.external.service;

import com.tripforge.external.dto.FxRateDto;
import com.tripforge.external.dto.ProviderResponse;
import com.tripforge.external.provider.FrankfurterFxProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Currency exchange rate service.
 *
 * Supported currencies: INR, USD, EUR, GBP, AED, SGD
 *
 * Fallback order:
 *   1. Frankfurter API (live ECB rates)
 *   2. Redis-cached rate (from previous successful call)
 *   3. In-memory last-known rate (hardcoded approximations)
 *
 * Rates are cached in Redis for 6 hours.
 * A scheduled job refreshes rates every 4 hours to keep cache warm.
 */
@Service
public class FxService {

    private static final Logger log = LoggerFactory.getLogger(FxService.class);

    private static final List<String> SUPPORTED_CURRENCIES =
            List.of("INR", "USD", "EUR", "GBP", "AED", "SGD");

    @Autowired
    private FrankfurterFxProvider frankfurter;

    /**
     * Convert an amount from one currency to another.
     * Cached in Redis for 6 hours.
     */
    @Cacheable(value = "fx-rates",
               key = "#from + '_' + #to + '_' + #amount",
               unless = "#result.data == null")
    public ProviderResponse<FxRateDto> convert(String from, String to, BigDecimal amount) {
        log.debug("FX convert: {} {} → {}", amount, from, to);

        FxRateDto rate = frankfurter.convert(from, to, amount);

        if (rate.isFallbackRate()) {
            return ProviderResponse.fallback(rate, rate.getSourceProvider(),
                    "Live FX rate unavailable — using last-known cached rate");
        }

        return ProviderResponse.of(rate, rate.getSourceProvider());
    }

    /**
     * Get exchange rate only (no amount conversion).
     * Cached in Redis for 6 hours.
     */
    @Cacheable(value = "fx-rates",
               key = "#from + '_' + #to + '_rate'",
               unless = "#result.data == null")
    public ProviderResponse<FxRateDto> getRate(String from, String to) {
        return convert(from, to, null);
    }

    /**
     * Scheduled job: warm the FX cache every 4 hours for all supported currency pairs.
     * Runs at startup and every 4 hours thereafter.
     */
    @Scheduled(fixedRateString = "14400000", initialDelayString = "30000")
    public void warmFxCache() {
        log.info("Warming FX rate cache for {} supported currencies", SUPPORTED_CURRENCIES.size());
        for (String from : SUPPORTED_CURRENCIES) {
            for (String to : SUPPORTED_CURRENCIES) {
                if (!from.equals(to)) {
                    try {
                        frankfurter.getRate(from, to);
                    } catch (Exception e) {
                        log.debug("FX cache warm failed for {}/{}: {}", from, to, e.getMessage());
                    }
                }
            }
        }
        log.info("FX rate cache warm complete");
    }

    public List<String> getSupportedCurrencies() {
        return SUPPORTED_CURRENCIES;
    }
}
