package com.tripforge.trip;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

/**
 * TripForge Trip Service
 * Orchestrates the full trip planning flow by calling hotel, route,
 * budget, and split services via OpenFeign, then merging results.
 */
@SpringBootApplication(exclude = FlywayAutoConfiguration.class)
@EnableDiscoveryClient
@EnableFeignClients
public class TripServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(TripServiceApplication.class, args);
    }
}
