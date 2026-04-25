package com.tripforge.route.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Attraction entity backed by the curated attractions dataset.
 */
@Entity
@Table(name = "attractions", schema = "route")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Attraction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String destination;

    @Column(nullable = false, length = 200)
    private String name;

    /** nature, temple, food, nightlife, shopping, adventure, beach */
    @Column(nullable = false, length = 100)
    private String category;

    @Column(nullable = false)
    private Double avgVisitHours;

    @Column(nullable = false)
    private Double ticketCost;

    /** 1-10 priority score */
    @Column(nullable = false)
    private Double priorityScore;

    /** A, B, C — for clustering nearby attractions */
    @Column(length = 5)
    private String distanceCluster;

    /**
     * Comma-separated interests this attraction suits.
     * e.g. "nature,adventure"
     */
    @Column(length = 300)
    private String suitableForInterests;
}
