package com.tripforge.payment.gateway;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripforge.payment.config.RazorpayProperties;
import com.tripforge.payment.dto.GatewayOrderResult;
import com.tripforge.payment.dto.GatewayVerifyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Razorpay payment gateway implementation.
 *
 * Uses raw HTTP (RestTemplate) to call Razorpay REST API.
 * No Razorpay SDK dependency — keeps the service lean and avoids SDK version conflicts.
 *
 * Razorpay API docs: https://razorpay.com/docs/api/
 *
 * Key behaviors:
 *   - createOrder: POST /orders with amount in paise (INR) or smallest unit
 *   - verifyPayment: HMAC-SHA256 signature verification
 *   - verifyWebhook: HMAC-SHA256 webhook signature verification
 *   - Returns degraded results if not configured — never throws to callers
 */
@Component
public class RazorpayGatewayProvider implements PaymentGateway {

    private static final Logger log = LoggerFactory.getLogger(RazorpayGatewayProvider.class);
    private static final String PROVIDER = "RAZORPAY";
    private static final String HMAC_ALGO = "HmacSHA256";

    @Autowired private RazorpayProperties props;
    @Autowired private ObjectMapper objectMapper;

    private final RestTemplate restTemplate = new RestTemplate();

    @Override
    public String getProviderName() { return PROVIDER; }

    @Override
    public boolean isConfigured() { return props.isConfigured(); }

    @Override
    public GatewayOrderResult createOrder(BigDecimal amount, String currency,
                                           String receipt, String notes) {
        if (!isConfigured()) {
            log.warn("Razorpay not configured — cannot create order");
            return GatewayOrderResult.builder()
                    .orderId("DEGRADED_" + UUID.randomUUID().toString().substring(0, 8))
                    .currency(currency)
                    .amount(amount)
                    .amountMinor(toMinorUnits(amount, currency))
                    .receipt(receipt)
                    .status("DEGRADED")
                    .gatewayProvider(PROVIDER)
                    .build();
        }

        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("amount", toMinorUnits(amount, currency));
            body.put("currency", currency.toUpperCase());
            body.put("receipt", receipt);
            if (notes != null) body.put("notes", Map.of("info", notes));

            HttpHeaders headers = buildAuthHeaders();
            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                    props.getBaseUrl() + "/orders", entity, String.class);

            JsonNode node = objectMapper.readTree(response.getBody());
            String orderId = node.path("id").asText();
            String status = node.path("status").asText("created");

            log.info("Razorpay order created: {} amount={} {}", orderId, amount, currency);

            return GatewayOrderResult.builder()
                    .orderId(orderId)
                    .currency(currency)
                    .amount(amount)
                    .amountMinor(toMinorUnits(amount, currency))
                    .receipt(receipt)
                    .status(status)
                    .gatewayProvider(PROVIDER)
                    .build();

        } catch (Exception e) {
            log.error("Razorpay createOrder failed: {}", e.getMessage());
            throw new RuntimeException("Payment gateway error: " + e.getMessage(), e);
        }
    }

    @Override
    public GatewayVerifyResult verifyPayment(String orderId, String paymentId, String signature) {
        if (!isConfigured()) {
            return GatewayVerifyResult.builder()
                    .success(false)
                    .message("Payment gateway not configured")
                    .gatewayOrderId(orderId)
                    .gatewayPaymentId(paymentId)
                    .status("DEGRADED")
                    .build();
        }

        try {
            // Razorpay signature = HMAC-SHA256(orderId + "|" + paymentId, keySecret)
            String payload = orderId + "|" + paymentId;
            String expectedSignature = hmacSha256(payload, props.getKeySecret());
            boolean valid = expectedSignature.equals(signature);

            log.info("Razorpay payment verification: orderId={} paymentId={} valid={}",
                    orderId, paymentId, valid);

            return GatewayVerifyResult.builder()
                    .success(valid)
                    .message(valid ? "Payment verified" : "Signature mismatch — payment not verified")
                    .gatewayOrderId(orderId)
                    .gatewayPaymentId(paymentId)
                    .status(valid ? "PAID" : "FAILED")
                    .build();

        } catch (Exception e) {
            log.error("Razorpay verifyPayment failed: {}", e.getMessage());
            return GatewayVerifyResult.builder()
                    .success(false)
                    .message("Verification error: " + e.getMessage())
                    .gatewayOrderId(orderId)
                    .gatewayPaymentId(paymentId)
                    .status("FAILED")
                    .build();
        }
    }

    @Override
    public boolean verifyWebhookSignature(String payload, String signature) {
        if (!props.isWebhookConfigured()) {
            log.warn("Webhook secret not configured — skipping signature verification");
            return false;
        }
        try {
            String expected = hmacSha256(payload, props.getWebhookSecret());
            boolean valid = expected.equals(signature);
            if (!valid) log.warn("Webhook signature mismatch");
            return valid;
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Convert amount to minor units (paise for INR, cents for USD/EUR/GBP/SGD, fils for AED).
     * Razorpay requires amounts in smallest currency unit.
     */
    private long toMinorUnits(BigDecimal amount, String currency) {
        // All supported currencies use 2 decimal places (100 minor units per major)
        return amount.multiply(BigDecimal.valueOf(100))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private HttpHeaders buildAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        // Razorpay uses HTTP Basic Auth: key_id:key_secret
        String credentials = props.getKeyId() + ":" + props.getKeySecret();
        String encoded = Base64.getEncoder().encodeToString(
                credentials.getBytes(StandardCharsets.UTF_8));
        headers.set("Authorization", "Basic " + encoded);
        return headers;
    }

    private String hmacSha256(String data, String secret) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGO);
        SecretKeySpec keySpec = new SecretKeySpec(
                secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO);
        mac.init(keySpec);
        byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder sb = new StringBuilder();
        for (byte b : hash) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
