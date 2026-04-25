package com.tripforge.split.service;

import com.tripforge.split.dto.SplitResultDto;
import com.tripforge.split.entity.Participant;
import com.tripforge.split.entity.SplitDetail;
import com.tripforge.split.repository.SplitDetailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Expense split service.
 * Supports equal split and custom percentage split.
 */
@Service
public class SplitService {

    private static final Logger log = LoggerFactory.getLogger(SplitService.class);

    @Autowired
    private SplitDetailRepository splitDetailRepository;

    /**
     * Equal split — divides total amount evenly among all travelers.
     * Generates generic participant names (Traveler 1, Traveler 2, ...).
     */
    @Transactional
    public SplitResultDto splitEqual(Map<String, Object> requestMap) {
        Long tripId = requestMap.get("tripId") != null
                ? ((Number) requestMap.get("tripId")).longValue() : null;
        BigDecimal totalAmount = requestMap.get("totalAmount") != null
                ? new BigDecimal(requestMap.get("totalAmount").toString()) : BigDecimal.ZERO;
        int travelers = requestMap.get("travelers") != null
                ? ((Number) requestMap.get("travelers")).intValue() : 1;

        log.info("Equal split for trip {} | total={} | travelers={}", tripId, totalAmount, travelers);

        BigDecimal perPerson = totalAmount.divide(
                BigDecimal.valueOf(travelers), 2, RoundingMode.HALF_UP);
        double percentageEach = 100.0 / travelers;

        List<Participant> participants = new ArrayList<>();
        for (int i = 1; i <= travelers; i++) {
            participants.add(Participant.builder()
                    .name("Traveler " + i)
                    .amount(perPerson)
                    .percentage(Math.round(percentageEach * 100.0) / 100.0)
                    .build());
        }

        return persistAndReturn(tripId, totalAmount, travelers, perPerson, participants);
    }

    /**
     * Custom split — accepts a list of participants with custom percentages.
     * Percentages must sum to 100.
     */
    @Transactional
    public SplitResultDto splitCustom(Map<String, Object> requestMap) {
        Long tripId = requestMap.get("tripId") != null
                ? ((Number) requestMap.get("tripId")).longValue() : null;
        BigDecimal totalAmount = requestMap.get("totalAmount") != null
                ? new BigDecimal(requestMap.get("totalAmount").toString()) : BigDecimal.ZERO;

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> participantData =
                (List<Map<String, Object>>) requestMap.get("participants");

        if (participantData == null || participantData.isEmpty()) {
            throw new IllegalArgumentException("Participants list is required for custom split");
        }

        List<Participant> participants = new ArrayList<>();
        for (Map<String, Object> p : participantData) {
            String name = (String) p.get("name");
            double pct = ((Number) p.get("percentage")).doubleValue();
            BigDecimal amount = totalAmount.multiply(BigDecimal.valueOf(pct / 100.0))
                    .setScale(2, RoundingMode.HALF_UP);
            participants.add(Participant.builder()
                    .name(name)
                    .amount(amount)
                    .percentage(pct)
                    .build());
        }

        int travelers = participants.size();
        BigDecimal perPerson = totalAmount.divide(BigDecimal.valueOf(travelers), 2, RoundingMode.HALF_UP);

        return persistAndReturn(tripId, totalAmount, travelers, perPerson, participants);
    }

    public SplitResultDto getByTripId(Long tripId) {
        return splitDetailRepository.findByTripId(tripId)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Split not found for trip: " + tripId));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private SplitResultDto persistAndReturn(Long tripId, BigDecimal totalAmount,
                                             int travelers, BigDecimal perPerson,
                                             List<Participant> participants) {
        SplitDetail detail = SplitDetail.builder()
                .tripId(tripId)
                .totalAmount(totalAmount)
                .travelers(travelers)
                .perPersonAmount(perPerson)
                .participants(participants)
                .build();

        // Upsert
        splitDetailRepository.findByTripId(tripId)
                .ifPresent(existing -> detail.setId(existing.getId()));
        splitDetailRepository.save(detail);

        return mapToDto(detail);
    }

    private SplitResultDto mapToDto(SplitDetail d) {
        List<SplitResultDto.ParticipantDto> pDtos = d.getParticipants().stream()
                .map(p -> SplitResultDto.ParticipantDto.builder()
                        .name(p.getName())
                        .amount(p.getAmount())
                        .percentage(p.getPercentage())
                        .build())
                .collect(Collectors.toList());

        return SplitResultDto.builder()
                .tripId(d.getTripId())
                .totalAmount(d.getTotalAmount())
                .travelers(d.getTravelers())
                .perPersonAmount(d.getPerPersonAmount())
                .participants(pDtos)
                .build();
    }
}
