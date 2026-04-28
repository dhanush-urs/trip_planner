package com.tripforge.budget.client;

import com.tripforge.budget.dto.FxRateResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;

/**
 * Feign client for external-data-service FX endpoints.
 * Used by budget-service to convert INR base rates to selected currency.
 *
 * Falls back gracefully — if external-data-service is down,
 * budget-service uses INR and marks fxFallbackUsed=true.
 */
@FeignClient(name = "external-data-service", path = "/api/external/fx")
public interface FxServiceClient {

    @GetMapping("/convert")
    FxRateResponse convert(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount
    );

    @GetMapping("/rate")
    FxRateResponse getRate(
            @RequestParam String from,
            @RequestParam String to
    );
}
