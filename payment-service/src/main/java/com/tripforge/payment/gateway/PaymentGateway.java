package com.tripforge.payment.gateway;

import com.tripforge.payment.dto.GatewayOrderResult;
import com.tripforge.payment.dto.GatewayVerifyResult;

import java.math.BigDecimal;

/**
 * Payment gateway abstraction.
 * Implement this interface to add Stripe, PayU, or any other gateway.
 */
public interface PaymentGateway {

    /** Provider name: "RAZORPAY", "STRIPE", etc. */
    String getProviderName();

    /** True if the gateway is configured with valid credentials */
    boolean isConfigured();

    /**
     * Create a payment order on the gateway.
     *
     * @param amount       amount in the given currency
     * @param currency     ISO currency code (e.g. "INR", "USD")
     * @param receipt      internal reference (e.g. "trip_123_full")
     * @param notes        optional metadata map as JSON string
     * @return GatewayOrderResult with orderId and display amount
     */
    GatewayOrderResult createOrder(BigDecimal amount, String currency,
                                   String receipt, String notes);

    /**
     * Verify a payment after frontend checkout completes.
     *
     * @param orderId   gateway order ID
     * @param paymentId gateway payment ID
     * @param signature gateway signature from frontend callback
     * @return GatewayVerifyResult indicating success/failure
     */
    GatewayVerifyResult verifyPayment(String orderId, String paymentId, String signature);

    /**
     * Verify a webhook payload signature.
     *
     * @param payload   raw webhook body as string
     * @param signature X-Razorpay-Signature header value
     * @return true if signature is valid
     */
    boolean verifyWebhookSignature(String payload, String signature);
}
