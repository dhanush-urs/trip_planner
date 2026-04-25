package com.tripforge.hotel.config;

import com.tripforge.hotel.entity.Hotel;
import com.tripforge.hotel.repository.HotelRepository;
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

/**
 * Loads the hotels CSV dataset into the database on startup if the table is empty.
 */
@Configuration
public class DataLoaderConfig {

    private static final Logger log = LoggerFactory.getLogger(DataLoaderConfig.class);

    @Autowired
    private HotelRepository hotelRepository;

    @Bean
    public CommandLineRunner loadHotelData() {
        return args -> {
            if (hotelRepository.count() > 0) {
                log.info("Hotels already loaded ({} records), skipping.", hotelRepository.count());
                return;
            }

            log.info("Loading hotels dataset from CSV...");
            List<Hotel> hotels = new ArrayList<>();

            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(
                            new ClassPathResource("dataset/hotels.csv").getInputStream()))) {

                String line;
                boolean firstLine = true;
                while ((line = reader.readLine()) != null) {
                    if (firstLine) { firstLine = false; continue; } // skip header
                    if (line.isBlank()) continue;

                    String[] parts = line.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
                    if (parts.length < 8) continue;

                    Hotel hotel = Hotel.builder()
                            .destination(parts[0].trim())
                            .name(parts[1].trim())
                            .pricePerNight(Double.parseDouble(parts[2].trim()))
                            .rating(Double.parseDouble(parts[3].trim()))
                            .distanceFromCenterKm(Double.parseDouble(parts[4].trim()))
                            .amenities(parts[5].trim())
                            .category(parts[6].trim())
                            .popularityScore(Double.parseDouble(parts[7].trim()))
                            .build();

                    hotels.add(hotel);
                }

                hotelRepository.saveAll(hotels);
                log.info("Loaded {} hotels into database.", hotels.size());

            } catch (Exception e) {
                log.error("Failed to load hotel dataset: {}", e.getMessage(), e);
            }
        };
    }
}
