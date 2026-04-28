package com.tripforge.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "participant_payment_status", schema = "payment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ParticipantPaymentStatus {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long tripId;
    @Column private Long participantId;
    @Column(nullable = false, length = 150) private String participantName;
    @Column(length = 255) private String participantEmail;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal allocatedAmount;
    @Column(nullable = false, precision = 14, scale = 2) @Builder.Default private BigDecimal paidAmount = BigDecimal.ZERO;
    @Column(nullable = false, length = 10) @Builder.Default private String currencyCode = "INR";
    @Column(nullable = false, length = 20) @Builder.Default private String status = "UNPAID";
    @Column(length = 1000) private String paymentLink;
    @Column private LocalDateTime lastPaymentAt;
    @UpdateTimestamp @Column(nullable = false) private LocalDateTime updatedAt;
}
