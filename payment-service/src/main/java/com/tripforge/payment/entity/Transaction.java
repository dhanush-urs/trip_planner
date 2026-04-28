package com.tripforge.payment.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "transactions", schema = "payment")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Transaction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long tripId;
    @Column(nullable = false) private Long userId;
    @Column private Long participantId;
    @Column(length = 150) private String participantName;
    @Column(length = 255) private String participantEmail;

    @Column(nullable = false, length = 50)
    @Builder.Default private String gatewayProvider = "RAZORPAY";

    @Column(nullable = false, length = 255) private String gatewayOrderId;
    @Column(length = 255) private String gatewayPaymentId;
    @Column(length = 512) private String gatewaySignature;

    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(nullable = false, length = 10) @Builder.Default private String currencyCode = "INR";

    @Column(nullable = false, length = 20) private String paymentType;  // FULL / SHARE

    @Column(nullable = false, length = 20)
    @Builder.Default private String status = "CREATED";

    @Column(length = 1000) private String paymentLink;
    @Column(length = 255) private String idempotencyKey;

    @Column(columnDefinition = "TEXT") private String rawGatewayPayloadJson;

    @CreationTimestamp @Column(nullable = false, updatable = false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(nullable = false) private LocalDateTime updatedAt;
}
