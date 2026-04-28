package com.tripforge.route;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(exclude = FlywayAutoConfiguration.class)
@EnableDiscoveryClient
@EnableFeignClients
public class RouteServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(RouteServiceApplication.class, args);
    }
}
