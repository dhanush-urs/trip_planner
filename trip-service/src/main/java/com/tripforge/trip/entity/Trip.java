package com.tripforge.trip.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Core Trip entity.
 * Stores trip metadata and owns the itinerary days.
 */
@Entity
@Table(name = "trips", schema = "trip")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trip {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** ID of the user who created this trip (from auth-service) */
    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 100)
    private String destination;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalBudget;

    @Column(nullable = false)
    private Integer travelers;

    /**
     * Comma-separated interests: nature,food,beaches,nightlife,temples,shopping,adventure
     */
    @Column(length = 500)
    private String interests;

    /**
     * Hotel preference: BUDGET, STANDARD, LUXURY
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private HotelPreference hotelPreference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private TripStatus status = TripStatus.PLANNED;

    /** ID of the currently selected hotel (from hotel-service) */
    @Column
    private Long selectedHotelId;

    /**
     * Currency code for this trip (e.g. INR, USD, EUR).
     * Defaults to INR for backward compatibility.
     */
    @Column(length = 10)
    @Builder.Default
    private String currencyCode = "INR";

    @OneToMany(mappedBy = "trip", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    @OrderBy("dayNumber ASC")
    private List<ItineraryDay> itineraryDays = new ArrayList<>();

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    public enum TripStatus {
        PLANNED, ACTIVE, COMPLETED, CANCELLED
    }

    public enum HotelPreference {
        BUDGET, STANDARD, LUXURY
    }
}
