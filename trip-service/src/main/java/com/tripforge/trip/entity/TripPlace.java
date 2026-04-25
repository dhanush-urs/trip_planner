package com.tripforge.trip.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

/**
 * A single attraction/place within a day's itinerary.
 */
@Entity
@Table(name = "trip_places", schema = "trip")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TripPlace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "itinerary_day_id", nullable = false)
    private ItineraryDay itineraryDay;

    /** Reference to attraction in route-service dataset */
    @Column
    private Long attractionId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 100)
    private String category;

    /** Suggested visit time e.g. "09:00 AM" */
    @Column(length = 20)
    private String visitTime;

    /** Estimated hours to spend */
    @Column
    private Double avgVisitHours;

    @Column(precision = 8, scale = 2)
    private BigDecimal ticketCost;

    @Column(length = 500)
    private String notes;

    @Column(nullable = false)
    @Builder.Default
    private Integer visitOrder = 1;
}
