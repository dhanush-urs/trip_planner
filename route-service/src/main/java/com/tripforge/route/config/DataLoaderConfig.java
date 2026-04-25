package com.tripforge.route.config;

import com.tripforge.route.entity.Attraction;
import com.tripforge.route.repository.AttractionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class DataLoaderConfig {

    private static final Logger log = LoggerFactory.getLogger(DataLoaderConfig.class);

    @Autowired
    private AttractionRepository attractionRepository;

    @Bean
    public CommandLineRunner loadAttractionData() {
        return args -> {
            if (attractionRepository.count() > 0) {
                log.info("Attractions already loaded, skipping.");
                return;
            }
            log.info("Loading attractions dataset...");
            List<Attraction> attractions = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new ClassPathResource("dataset/attractions.csv").getInputStream()))) {

                String line;
                boolean first = true;
                while ((line = reader.readLine()) != null) {
                    if (first) { first = false; continue; }
                    if (line.isBlank()) continue;

                    String[] p = line.split(",", 8);
                    if (p.length < 7) continue;

                    attractions.add(Attraction.builder()
                            .destination(p[0].trim())
                            .name(p[1].trim())
                            .category(p[2].trim())
                            .avgVisitHours(Double.parseDouble(p[3].trim()))
                            .ticketCost(Double.parseDouble(p[4].trim()))
                            .priorityScore(Double.parseDouble(p[5].trim()))
                            .distanceCluster(p[6].trim())
                            .suitableForInterests(p.length > 7 ? p[7].trim() : "")
                            .build());
                }

                attractionRepository.saveAll(attractions);
                log.info("Loaded {} attractions.", attractions.size());
            } catch (Exception e) {
                log.error("Failed to load attractions: {}", e.getMessage(), e);
            }
        };
    }
}
