package com.tripforge.split;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(exclude = FlywayAutoConfiguration.class)
@EnableDiscoveryClient
public class SplitServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(SplitServiceApplication.class, args);
    }
}
