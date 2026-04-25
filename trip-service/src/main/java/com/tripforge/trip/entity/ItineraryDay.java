package com.tripforge.trip.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single day in a trip itinerary.
 */
@Entity
@Table(name = "itinerary_days", schema = "trip")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ItineraryDay {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private Integer dayNumber;

    @Column(nullable = false)
    private LocalDate date;

    @Column(length = 200)
    private String theme;

    @OneToMany(mappedBy = "itineraryDay", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    @OrderBy("visitOrder ASC")
    private List<TripPlace> places = new ArrayList<>();
}
