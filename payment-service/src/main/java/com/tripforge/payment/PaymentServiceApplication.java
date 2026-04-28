package com.tripforge.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * TripForge Payment Service
 *
 * Handles:
 *   - Razorpay order creation (full trip + per-share)
 *   - Payment signature verification
 *   - Webhook processing (idempotent)
 *   - Trip payment summary
 *   - Participant payment status tracking
 *   - Payment link generation
 *
 * Runs in DEGRADED mode if Razorpay keys are absent.
 * Trip planning NEVER fails because this service is unavailable.
 */
@SpringBootApplication(exclude = FlywayAutoConfiguration.class)
@EnableDiscoveryClient
@EnableFeignClients
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
