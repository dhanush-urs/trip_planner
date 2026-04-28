package com.tripforge.trip.client;

import com.tripforge.trip.dto.ApiResponse;
import com.tripforge.trip.dto.payment.InitPaymentRequest;
import com.tripforge.trip.dto.payment.PaymentSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * Feign client for payment-service.
 * All calls are wrapped in try/catch in TripService.
 * Trip creation NEVER fails because payment-service is unavailable.
 */
@FeignClient(name = "payment-service", path = "/api/payments")
public interface PaymentServiceClient {

    @PostMapping("/init")
    ApiResponse<Void> initPayment(@RequestBody InitPaymentRequest request);

    @GetMapping("/trip/{tripId}")
    ApiResponse<PaymentSummaryDto> getTripPaymentSummary(@PathVariable Long tripId);
}
