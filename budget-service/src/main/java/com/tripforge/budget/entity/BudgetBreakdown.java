package com.tripforge.budget.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "budget_breakdowns", schema = "budget")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BudgetBreakdown {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long tripId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal hotelCost;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal foodCost;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal transportCost;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal attractionCost;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal miscCost;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalEstimated;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalBudget;

    @Column(nullable = false)
    private boolean overBudget;

    /** Phase 9E: currency fields */
    @Column(length = 10)
    @Builder.Default
    private String currencyCode = "INR";

    @Column(precision = 18, scale = 8)
    private java.math.BigDecimal exchangeRateUsed;

    @Column(length = 50)
    private String fxSourceProvider;

    @Column(nullable = false)
    @Builder.Default
    private boolean fxFallbackUsed = false;

    @CreationTimestamp
    private LocalDateTime calculatedAt;
}
