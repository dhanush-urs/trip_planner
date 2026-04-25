package com.tripforge.hotel.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Hotel entity backed by the curated hotels dataset.
 */
@Entity
@Table(name = "hotels", schema = "hotel")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Hotel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String destination;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(nullable = false)
    private Double pricePerNight;

    @Column(nullable = false)
    private Double rating;

    @Column(nullable = false)
    private Double distanceFromCenterKm;

    /** Comma-separated amenities: pool,wifi,gym,spa,restaurant,parking */
    @Column(length = 500)
    private String amenities;

    /** BUDGET, STANDARD, LUXURY */
    @Column(nullable = false, length = 20)
    private String category;

    @Column(nullable = false)
    private Double popularityScore;
}
