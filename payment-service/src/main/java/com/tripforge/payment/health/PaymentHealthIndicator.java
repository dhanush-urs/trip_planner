package com.tripforge.payment.health;

import com.tripforge.payment.config.RazorpayProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

/**
 * Custom Actuator health indicator for payment-service.
 * Reports Razorpay configuration status without leaking secrets.
 * Service is UP even when keys are absent (degraded mode).
 */
@Component
public class PaymentHealthIndicator implements HealthIndicator {

    @Autowired
    private RazorpayProperties props;

    @Override
    public Health health() {
        boolean configured = props.isConfigured();
        boolean webhookReady = props.isWebhookConfigured();

        if (configured) {
            return Health.up()
                    .withDetail("gateway", "RAZORPAY")
                    .withDetail("gateway_configured", true)
                    .withDetail("webhook_configured", webhookReady)
                    .withDetail("mode", "LIVE")
                    .build();
        }

        return Health.status("DEGRADED")
                .withDetail("gateway", "RAZORPAY")
                .withDetail("gateway_configured", false)
                .withDetail("webhook_configured", false)
                .withDetail("mode", "DEGRADED")
                .withDetail("note", "Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET to enable payments")
                .build();
    }
}
