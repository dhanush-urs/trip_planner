package com.tripforge.payment.controller;

import com.tripforge.payment.dto.*;
import com.tripforge.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Payment REST controller.
 * All endpoints require JWT (enforced by API Gateway).
 * Webhook endpoint is public but signature-verified.
 */
@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private static final Logger log = LoggerFactory.getLogger(PaymentController.class);

    @Autowired private PaymentService paymentService;

    /** POST /api/payments/order/full — create full trip payment order */
    @PostMapping("/order/full")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createFullOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        try {
            CreateOrderResponse result = paymentService.createFullOrder(request);
            return ResponseEntity.ok(ApiResponse.success("Full trip payment order created", result));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "PAYMENT_NOT_CONFIGURED"));
        }
    }

    /** POST /api/payments/order/share — create participant share payment order */
    @PostMapping("/order/share")
    public ResponseEntity<ApiResponse<CreateOrderResponse>> createShareOrder(
            @Valid @RequestBody CreateOrderRequest request) {
        try {
            CreateOrderResponse result = paymentService.createShareOrder(request);
            return ResponseEntity.ok(ApiResponse.success("Share payment order created", result));
        } catch (IllegalStateException | IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage(), "ORDER_ERROR"));
        }
    }

    /** POST /api/payments/verify — verify payment after frontend checkout */
    @PostMapping("/verify")
    public ResponseEntity<ApiResponse<PaymentSummaryDto>> verifyPayment(
            @Valid @RequestBody VerifyPaymentRequest request) {
        return ResponseEntity.ok(paymentService.verifyPayment(request));
    }

    /** GET /api/payments/trip/{tripId} — get trip payment summary */
    @GetMapping("/trip/{tripId}")
    public ResponseEntity<ApiResponse<PaymentSummaryDto>> getTripSummary(
            @PathVariable Long tripId) {
        PaymentSummaryDto summary = paymentService.getTripPaymentSummary(tripId);
        return ResponseEntity.ok(ApiResponse.success("Payment summary fetched", summary));
    }

    /** POST /api/payments/trip/{tripId}/links — generate participant payment links */
    @PostMapping("/trip/{tripId}/links")
    public ResponseEntity<ApiResponse<PaymentSummaryDto>> generateLinks(
            @PathVariable Long tripId) {
        PaymentSummaryDto result = paymentService.generatePaymentLinks(tripId);
        return ResponseEntity.ok(ApiResponse.success("Payment links generated", result));
    }

    /** POST /api/payments/trip/{tripId}/refresh — refresh payment summary */
    @PostMapping("/trip/{tripId}/refresh")
    public ResponseEntity<ApiResponse<PaymentSummaryDto>> refreshSummary(
            @PathVariable Long tripId) {
        PaymentSummaryDto result = paymentService.refreshSummary(tripId);
        return ResponseEntity.ok(ApiResponse.success("Payment summary refreshed", result));
    }

    /** POST /api/payments/init — initialize payment tracking for a trip */
    @PostMapping("/init")
    public ResponseEntity<ApiResponse<Void>> initPayment(
            @RequestBody InitPaymentRequest request) {
        paymentService.initializePaymentTracking(request);
        return ResponseEntity.ok(ApiResponse.success("Payment tracking initialized", null));
    }
}
