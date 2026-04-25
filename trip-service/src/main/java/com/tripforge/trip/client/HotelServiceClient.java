package com.tripforge.trip.client;

import com.tripforge.trip.dto.ApiResponse;
import com.tripforge.trip.dto.HotelDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.math.BigDecimal;
import java.util.List;

/**
 * Feign client for hotel-service.
 * Uses Eureka service discovery (name = "hotel-service").
 */
@FeignClient(name = "hotel-service", path = "/api/hotels")
public interface HotelServiceClient {

    @GetMapping("/recommend")
    ApiResponse<List<HotelDto>> recommendHotels(
            @RequestParam String destination,
            @RequestParam BigDecimal budget,
            @RequestParam Integer durationDays,
            @RequestParam Integer travelers,
            @RequestParam String hotelPreference
    );
}
