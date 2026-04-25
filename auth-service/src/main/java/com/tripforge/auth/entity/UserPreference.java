package com.tripforge.auth.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * UserPreference entity — stores travel preferences for a user.
 * One-to-one relationship with User.
 * Interests stored as a comma-separated string for simplicity
 * (e.g., "nature,food,beaches").
 */
@Entity
@Table(name = "user_preferences", schema = "auth")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPreference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    /**
     * Comma-separated list of interests.
     * Valid values: nature, nightlife, food, temples, shopping, adventure, beaches
     */
    @Column(length = 500)
    private String interests;

    /**
     * Hotel preference: BUDGET, STANDARD, LUXURY
     */
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    @Builder.Default
    private HotelPreference hotelPreference = HotelPreference.STANDARD;

    /**
     * Default budget in INR for trip planning
     */
    @Column(precision = 12, scale = 2)
    private BigDecimal defaultBudget;

    /**
     * Default number of travelers
     */
    @Column
    @Builder.Default
    private Integer defaultTravelers = 2;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum HotelPreference {
        BUDGET, STANDARD, LUXURY
    }
}
