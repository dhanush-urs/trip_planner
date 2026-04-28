package com.tripforge.payment.controller;

import com.tripforge.payment.service.PaymentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Webhook controller — public endpoint, signature-verified.
 * Razorpay sends POST requests here for payment events.
 */
@RestController
@RequestMapping("/api/payments")
public class WebhookController {

    private static final Logger log = LoggerFactory.getLogger(WebhookController.class);

    @Autowired private PaymentService paymentService;

    /**
     * POST /api/payments/webhook
     * Razorpay webhook endpoint.
     * Must be public (no JWT) but signature-verified.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        log.info("Webhook received, signature present: {}", signature != null);

        if (signature == null || signature.isBlank()) {
            log.warn("Webhook received without signature — rejecting");
            return ResponseEntity.badRequest().body("Missing signature");
        }

        try {
            paymentService.processWebhook(payload, signature);
            return ResponseEntity.ok("OK");
        } catch (SecurityException e) {
            log.warn("Webhook rejected: invalid signature");
            return ResponseEntity.status(401).body("Invalid signature");
        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage());
            // Return 200 to prevent Razorpay from retrying on processing errors
            return ResponseEntity.ok("Processed with errors");
        }
    }
}
