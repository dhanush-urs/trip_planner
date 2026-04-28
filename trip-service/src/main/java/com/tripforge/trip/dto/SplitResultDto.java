package com.tripforge.trip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SplitResultDto {
    private Long tripId;
    private BigDecimal totalAmount;
    private Integer travelers;
    private BigDecimal perPersonAmount;
    private List<ParticipantDto> participants;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ParticipantDto {
        private Long participantId;
        private String name;
        private String email;
        private BigDecimal amount;
        private Double percentage;
    }
}
