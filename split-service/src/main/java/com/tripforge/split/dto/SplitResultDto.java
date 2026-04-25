package com.tripforge.split.dto;

import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class SplitResultDto {
    private Long tripId;
    private BigDecimal totalAmount;
    private Integer travelers;
    private BigDecimal perPersonAmount;
    private List<ParticipantDto> participants;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class ParticipantDto {
        private String name;
        private BigDecimal amount;
        private Double percentage;
    }
}
