package com.tripforge.split.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;
import java.math.BigDecimal;
import java.util.List;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class SplitResultDto {
    private Long tripId;
    private BigDecimal totalAmount;
    private Integer travelers;
    private BigDecimal perPersonAmount;
    private List<ParticipantDto> participants;

    @Builder.Default
    private String currencyCode = "INR";

    /** EQUAL / CUSTOM_PERCENTAGE / CUSTOM_AMOUNT */
    @Builder.Default
    private String splitMode = "EQUAL";

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static class ParticipantDto {
        private Long participantId;
        private String name;
        private String email;
        private BigDecimal amount;
        private Double percentage;
        private String currencyCode;
    }
}
