package com.tripforge.external;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * TripForge External Data Service
 *
 * Centralizes all external provider integrations:
 *   - Google Places API  (hotels, attractions, POIs)
 *   - OpenTripMap API    (fallback attractions)
 *   - Google Directions  (route optimization, travel times)
 *   - OpenRouteService   (fallback routing)
 *   - Frankfurter API    (currency exchange rates)
 *
 * All responses are normalized into internal DTOs.
 * Provider-specific details never leak to callers.
 * Redis caching reduces provider API calls and latency.
 * Graceful fallback chains ensure the system never hard-fails.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableCaching
@EnableScheduling
public class ExternalDataServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ExternalDataServiceApplication.class, args);
    }
}
