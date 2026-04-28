package com.tripforge.payment.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "razorpay")
@Data
public class RazorpayProperties {
    private String keyId = "";
    private String keySecret = "";
    private String webhookSecret = "";
    private boolean enabled = true;
    private String baseUrl = "https://api.razorpay.com/v1";
    private int timeoutMs = 10000;

    public boolean isConfigured() {
        return enabled
                && keyId != null && !keyId.isBlank()
                && keySecret != null && !keySecret.isBlank();
    }

    public boolean isWebhookConfigured() {
        return webhookSecret != null && !webhookSecret.isBlank();
    }
}
