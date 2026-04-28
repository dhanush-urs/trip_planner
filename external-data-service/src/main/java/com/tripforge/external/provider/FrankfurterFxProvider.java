package com.tripforge.external.provider;

import com.tripforge.external.config.ProviderProperties;
import com.tripforge.external.dto.FxRateDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Frankfurter API provider for currency exchange rates.
 *
 * Free, no API key required: https://www.frankfurter.app
 * Provides ECB (European Central Bank) rates.
 *
 * Includes an in-memory last-known-rate cache as a final fallback
 * when the provider is unreachable.
 */
@Component
public class FrankfurterFxProvider {

    private static final Logger log = LoggerFactory.getLogger(FrankfurterFxProvider.class);
    private static final String PROVIDER_NAME = "frankfurter";

    /** In-memory last-known rates: "FROM_TO" → rate */
    private final Map<String, BigDecimal> lastKnownRates = new ConcurrentHashMap<>();

    private final ProviderProperties props;
    private final RestTemplate fxRestTemplate;

    public FrankfurterFxProvider(ProviderProperties props,
                                  @Qualifier("fxRestTemplate") RestTemplate fxRestTemplate) {
        this.props = props;
        this.fxRestTemplate = fxRestTemplate;
    }

    /**
     * Convert an amount from one currency to another.
     * Falls back to last-known cached rate if provider is unavailable.
     */
    public FxRateDto convert(String from, String to, BigDecimal amount) {
        if (from.equalsIgnoreCase(to)) {
            return FxRateDto.builder()
                    .fromCurrency(from.toUpperCase())
                    .toCurrency(to.toUpperCase())
                    .rate(BigDecimal.ONE)
                    .originalAmount(amount)
                    .convertedAmount(amount)
                    .rateDate(LocalDate.now().toString())
                    .sourceProvider(PROVIDER_NAME)
                    .cachedRate(false)
                    .fallbackRate(false)
                    .build();
        }

        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(props.getFrankfurter().getBaseUrl() + "/latest")
                    .queryParam("from", from.toUpperCase())
                    .queryParam("to", to.toUpperCase())
                    .build()
                    .toUriString();

            @SuppressWarnings("unchecked")
            Map<String, Object> response = fxRestTemplate.getForObject(url, Map.class);
            if (response == null) return fallbackRate(from, to, amount);

            @SuppressWarnings("unchecked")
            Map<String, Object> rates = (Map<String, Object>) response.get("rates");
            if (rates == null || !rates.containsKey(to.toUpperCase())) {
                return fallbackRate(from, to, amount);
            }

            BigDecimal rate = new BigDecimal(rates.get(to.toUpperCase()).toString());
            BigDecimal converted = amount != null
                    ? amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                    : null;

            // Update last-known cache
            lastKnownRates.put(cacheKey(from, to), rate);

            log.debug("FX rate {}/{} = {}", from, to, rate);

            return FxRateDto.builder()
                    .fromCurrency(from.toUpperCase())
                    .toCurrency(to.toUpperCase())
                    .rate(rate)
                    .originalAmount(amount)
                    .convertedAmount(converted)
                    .rateDate(LocalDate.now().toString())
                    .sourceProvider(PROVIDER_NAME)
                    .cachedRate(false)
                    .fallbackRate(false)
                    .build();

        } catch (Exception e) {
            log.warn("Frankfurter FX call failed for {}/{}: {}", from, to, e.getMessage());
            return fallbackRate(from, to, amount);
        }
    }

    /**
     * Get the exchange rate without converting an amount.
     */
    public FxRateDto getRate(String from, String to) {
        return convert(from, to, null);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private FxRateDto fallbackRate(String from, String to, BigDecimal amount) {
        String key = cacheKey(from, to);
        BigDecimal rate = lastKnownRates.get(key);

        if (rate == null) {
            // Hardcoded approximate rates as absolute last resort
            rate = getHardcodedRate(from, to);
            log.warn("Using hardcoded fallback rate for {}/{}: {}", from, to, rate);
        } else {
            log.warn("Using last-known cached rate for {}/{}: {}", from, to, rate);
        }

        BigDecimal converted = amount != null
                ? amount.multiply(rate).setScale(2, RoundingMode.HALF_UP)
                : null;

        return FxRateDto.builder()
                .fromCurrency(from.toUpperCase())
                .toCurrency(to.toUpperCase())
                .rate(rate)
                .originalAmount(amount)
                .convertedAmount(converted)
                .rateDate(LocalDate.now().toString())
                .sourceProvider(PROVIDER_NAME + "_fallback")
                .cachedRate(true)
                .fallbackRate(true)
                .build();
    }

    /**
     * Approximate hardcoded rates (INR base) as absolute last resort.
     * These are rough approximations — only used when all live sources fail.
     */
    private BigDecimal getHardcodedRate(String from, String to) {
        Map<String, Double> toInr = Map.of(
                "INR", 1.0, "USD", 83.0, "EUR", 90.0,
                "GBP", 105.0, "AED", 22.6, "SGD", 62.0
        );
        double fromRate = toInr.getOrDefault(from.toUpperCase(), 1.0);
        double toRate = toInr.getOrDefault(to.toUpperCase(), 1.0);
        double rate = fromRate / toRate;
        return BigDecimal.valueOf(rate).setScale(6, RoundingMode.HALF_UP);
    }

    private String cacheKey(String from, String to) {
        return from.toUpperCase() + "_" + to.toUpperCase();
    }

    public String getProviderName() { return PROVIDER_NAME; }
}
