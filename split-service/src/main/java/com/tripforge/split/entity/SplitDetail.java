package com.tripforge.split.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Stores the split result for a trip.
 * Participants are stored as an embedded JSON-like structure via @ElementCollection.
 */
@Entity
@Table(name = "split_details", schema = "split_schema")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class SplitDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private Long tripId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private Integer travelers;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal perPersonAmount;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "split_participants",
            schema = "split_schema",
            joinColumns = @JoinColumn(name = "split_detail_id"))
    @Builder.Default
    private List<Participant> participants = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime calculatedAt;
}
