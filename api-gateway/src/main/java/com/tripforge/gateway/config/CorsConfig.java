package com.tripforge.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Global CORS configuration for the API Gateway.
 * Allows the React frontend (Vite dev server on port 5173) to communicate
 * with the backend through the gateway.
 *
 * In production, replace localhost:5173 with the actual frontend domain.
 */
@Configuration
public class CorsConfig {

    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();

        // Allowed origins — React Vite dev server + Docker nginx frontend
        // The browser's Origin header reflects the HOST port, not the container port.
        // Frontend container maps 3000:80, so browser sends Origin: http://localhost:3000
        corsConfig.setAllowedOrigins(List.of(
                "http://localhost:5173",   // Vite dev server (npm run dev)
                "http://localhost:3000",   // Docker nginx (host port 3000 → container port 80)
                "http://localhost:80",     // Direct nginx access on port 80
                "http://localhost"         // Bare localhost (no port)
        ));

        // Allowed HTTP methods
        corsConfig.setAllowedMethods(List.of(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));

        // Allowed headers
        corsConfig.setAllowedHeaders(List.of(
                "Authorization",
                "Content-Type",
                "Accept",
                "X-Requested-With",
                "Origin",
                "Access-Control-Request-Method",
                "Access-Control-Request-Headers"
        ));

        // Expose these headers to the browser
        corsConfig.setExposedHeaders(List.of(
                "Authorization",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials"
        ));

        // Allow cookies / credentials
        corsConfig.setAllowCredentials(true);

        // Cache preflight response for 1 hour
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
