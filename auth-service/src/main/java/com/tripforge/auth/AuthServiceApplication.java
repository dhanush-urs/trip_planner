package com.tripforge.auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * TripForge Auth Service
 * Handles user registration, login, JWT token issuance,
 * user profile management, and preference storage.
 */
@SpringBootApplication(exclude = FlywayAutoConfiguration.class)
@EnableDiscoveryClient
public class AuthServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
