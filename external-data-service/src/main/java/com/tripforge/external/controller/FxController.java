package com.tripforge.external.controller;

import com.tripforge.external.dto.FxRateDto;
import com.tripforge.external.dto.ProviderResponse;
import com.tripforge.external.service.FxService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

/**
 * Currency exchange rate controller.
 *
 * GET /api/external/fx/convert  — convert an amount between currencies
 * GET /api/external/fx/rate     — get exchange rate only
 * GET /api/external/fx/currencies — list supported currencies
 */
@RestController
@RequestMapping("/api/external/fx")
public class FxController {

    @Autowired
    private FxService fxService;

    /**
     * Convert an amount from one currency to another.
     *
     * @param from   source currency code (e.g. "INR")
     * @param to     target currency code (e.g. "USD")
     * @param amount amount to convert
     *
     * Example: GET /api/external/fx/convert?from=INR&to=USD&amount=50000
     */
    @GetMapping("/convert")
    public ResponseEntity<ProviderResponse<FxRateDto>> convert(
            @RequestParam String from,
            @RequestParam String to,
            @RequestParam BigDecimal amount) {

        ProviderResponse<FxRateDto> response = fxService.convert(
                from.toUpperCase(), to.toUpperCase(), amount);
        return ResponseEntity.ok(response);
    }

    /**
     * Get exchange rate without converting an amount.
     *
     * Example: GET /api/external/fx/rate?from=INR&to=USD
     */
    @GetMapping("/rate")
    public ResponseEntity<ProviderResponse<FxRateDto>> getRate(
            @RequestParam String from,
            @RequestParam String to) {

        ProviderResponse<FxRateDto> response = fxService.getRate(
                from.toUpperCase(), to.toUpperCase());
        return ResponseEntity.ok(response);
    }

    /**
     * List all supported currencies.
     */
    @GetMapping("/currencies")
    public ResponseEntity<List<String>> getSupportedCurrencies() {
        return ResponseEntity.ok(fxService.getSupportedCurrencies());
    }
}
