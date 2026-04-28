package com.tripforge.split.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;

import java.math.BigDecimal;

/**
 * Embeddable participant record within a split.
 * Phase 9F: added participantId and participantEmail.
 */
@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Participant {

    /** Optional external participant ID (from split request) */
    @Column
    private Long participantId;

    @Column(nullable = false, length = 100)
    private String name;

    /** Optional email for payment link delivery */
    @Column(length = 255)
    private String email;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private Double percentage;
}
