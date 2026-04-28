package com.tripforge.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trip_payment_summary", schema = "payment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class TripPaymentSummary {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true) private Long tripId;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal totalAmount;
    @Column(nullable = false, length = 10) @Builder.Default private String currencyCode = "INR";
    @Column(nullable = false, precision = 14, scale = 2) @Builder.Default private BigDecimal amountPaid = BigDecimal.ZERO;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amountRemaining;
    @Column(nullable = false, length = 20) @Builder.Default private String status = "UNPAID";
    @Column private LocalDateTime lastPaymentAt;
    @UpdateTimestamp @Column(nullable = false) private LocalDateTime updatedAt;
}
