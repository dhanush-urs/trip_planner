package com.tripforge.split.service;

import com.tripforge.split.dto.SplitRequest;
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
 * Expense split service — Phase 9F upgrade.
 *
 * Supports three split modes:
 *   EQUAL            — divide total evenly, largest-remainder rounding
 *   CUSTOM_PERCENTAGE — each participant specifies their %, must sum to 100
 *   CUSTOM_AMOUNT    — each participant specifies their amount, must sum to total
 *
 * All modes:
 *   - accept participant names and emails
 *   - carry currencyCode through
 *   - guarantee sum(participant.amount) == totalAmount
 */
@Service
public class SplitService {

    private static final Logger log = LoggerFactory.getLogger(SplitService.class);

    @Autowired
    private SplitDetailRepository splitDetailRepository;

    // ── Equal Split ───────────────────────────────────────────────────────────

    /**
     * Equal split — typed request.
     * If participants list is provided, uses their names/emails.
     * Otherwise generates generic "Traveler N" names.
     */
    @Transactional
    public SplitResultDto splitEqual(SplitRequest request) {
        int travelers = request.getParticipants() != null && !request.getParticipants().isEmpty()
                ? request.getParticipants().size()
                : 2;
        String currency = request.getCurrencyCode() != null ? request.getCurrencyCode() : "INR";
        BigDecimal total = request.getTotalAmount();

        log.info("Equal split: tripId={} total={} travelers={} currency={}",
                request.getTripId(), total, travelers, currency);

        // Largest-remainder method
        BigDecimal perPersonFloor = total.divide(BigDecimal.valueOf(travelers), 2, RoundingMode.FLOOR);
        BigDecimal remainder = total.subtract(perPersonFloor.multiply(BigDecimal.valueOf(travelers)));
        int extraCents = remainder.movePointRight(2).intValue();
        double pctEach = 100.0 / travelers;

        List<Participant> participants = new ArrayList<>();
        for (int i = 0; i < travelers; i++) {
            BigDecimal amount = i < extraCents
                    ? perPersonFloor.add(new BigDecimal("0.01"))
                    : perPersonFloor;

            String name = "Traveler " + (i + 1);
            String email = null;
            Long pid = null;
            if (request.getParticipants() != null && i < request.getParticipants().size()) {
                SplitRequest.ParticipantInput p = request.getParticipants().get(i);
                if (p.getParticipantName() != null) name = p.getParticipantName();
                email = p.getParticipantEmail();
                pid = p.getParticipantId();
            }

            participants.add(Participant.builder()
                    .participantId(pid)
                    .name(name)
                    .email(email)
                    .amount(amount)
                    .percentage(Math.round(pctEach * 100.0) / 100.0)
                    .build());
        }

        return persistAndReturn(request.getTripId(), total, travelers,
                perPersonFloor, participants, currency, "EQUAL");
    }

    /**
     * Equal split — legacy Map-based (backward-compatible with trip-service Feign calls).
     */
    @Transactional
    public SplitResultDto splitEqual(Map<String, Object> requestMap) {
        SplitRequest req = new SplitRequest();
        req.setTripId(requestMap.get("tripId") != null
                ? ((Number) requestMap.get("tripId")).longValue() : null);
        req.setTotalAmount(requestMap.get("totalAmount") != null
                ? new BigDecimal(requestMap.get("totalAmount").toString()) : BigDecimal.ZERO);
        req.setCurrencyCode(requestMap.get("currencyCode") != null
                ? (String) requestMap.get("currencyCode") : "INR");

        // travelers count from map (legacy)
        int travelers = requestMap.get("travelers") != null
                ? ((Number) requestMap.get("travelers")).intValue() : 2;

        // Build generic participants
        List<SplitRequest.ParticipantInput> parts = new ArrayList<>();
        for (int i = 1; i <= travelers; i++) {
            SplitRequest.ParticipantInput p = new SplitRequest.ParticipantInput();
            p.setParticipantName("Traveler " + i);
            parts.add(p);
        }
        req.setParticipants(parts);
        return splitEqual(req);
    }

    // ── Custom Percentage Split ───────────────────────────────────────────────

    @Transactional
    public SplitResultDto splitCustomPercentage(SplitRequest request) {
        if (request.getParticipants() == null || request.getParticipants().isEmpty()) {
            throw new IllegalArgumentException("Participants required for custom percentage split");
        }

        // Validate percentages sum to 100
        double totalPct = request.getParticipants().stream()
                .mapToDouble(p -> p.getPercentage() != null ? p.getPercentage() : 0.0)
                .sum();
        if (Math.abs(totalPct - 100.0) > 0.01) {
            throw new IllegalArgumentException(
                    "Percentages must sum to 100. Got: " + totalPct);
        }

        String currency = request.getCurrencyCode() != null ? request.getCurrencyCode() : "INR";
        BigDecimal total = request.getTotalAmount();
        int travelers = request.getParticipants().size();

        log.info("Custom % split: tripId={} total={} participants={} currency={}",
                request.getTripId(), total, travelers, currency);

        List<Participant> participants = new ArrayList<>();
        BigDecimal allocated = BigDecimal.ZERO;

        for (int i = 0; i < travelers; i++) {
            SplitRequest.ParticipantInput p = request.getParticipants().get(i);
            double pct = p.getPercentage() != null ? p.getPercentage() : 0.0;

            BigDecimal amount;
            if (i == travelers - 1) {
                // Last participant gets the remainder to ensure exact sum
                amount = total.subtract(allocated).setScale(2, RoundingMode.HALF_UP);
            } else {
                amount = total.multiply(BigDecimal.valueOf(pct / 100.0))
                        .setScale(2, RoundingMode.HALF_UP);
                allocated = allocated.add(amount);
            }

            participants.add(Participant.builder()
                    .participantId(p.getParticipantId())
                    .name(p.getParticipantName() != null ? p.getParticipantName() : "Participant " + (i + 1))
                    .email(p.getParticipantEmail())
                    .amount(amount)
                    .percentage(pct)
                    .build());
        }

        BigDecimal perPerson = total.divide(BigDecimal.valueOf(travelers), 2, RoundingMode.HALF_UP);
        return persistAndReturn(request.getTripId(), total, travelers,
                perPerson, participants, currency, "CUSTOM_PERCENTAGE");
    }

    // ── Custom Amount Split ───────────────────────────────────────────────────

    @Transactional
    public SplitResultDto splitCustomAmount(SplitRequest request) {
        if (request.getParticipants() == null || request.getParticipants().isEmpty()) {
            throw new IllegalArgumentException("Participants required for custom amount split");
        }

        BigDecimal total = request.getTotalAmount();
        String currency = request.getCurrencyCode() != null ? request.getCurrencyCode() : "INR";
        int travelers = request.getParticipants().size();

        // Validate amounts sum to total
        BigDecimal sumAmounts = request.getParticipants().stream()
                .map(p -> p.getAmount() != null ? p.getAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (sumAmounts.subtract(total).abs().compareTo(new BigDecimal("0.02")) > 0) {
            throw new IllegalArgumentException(
                    "Participant amounts must sum to total. Got: " + sumAmounts + ", expected: " + total);
        }

        log.info("Custom amount split: tripId={} total={} participants={} currency={}",
                request.getTripId(), total, travelers, currency);

        List<Participant> participants = new ArrayList<>();
        for (int i = 0; i < travelers; i++) {
            SplitRequest.ParticipantInput p = request.getParticipants().get(i);
            BigDecimal amount = p.getAmount() != null
                    ? p.getAmount().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO;
            double pct = total.compareTo(BigDecimal.ZERO) > 0
                    ? amount.divide(total, 6, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue()
                    : 0.0;

            participants.add(Participant.builder()
                    .participantId(p.getParticipantId())
                    .name(p.getParticipantName() != null ? p.getParticipantName() : "Participant " + (i + 1))
                    .email(p.getParticipantEmail())
                    .amount(amount)
                    .percentage(Math.round(pct * 100.0) / 100.0)
                    .build());
        }

        BigDecimal perPerson = total.divide(BigDecimal.valueOf(travelers), 2, RoundingMode.HALF_UP);
        return persistAndReturn(request.getTripId(), total, travelers,
                perPerson, participants, currency, "CUSTOM_AMOUNT");
    }

    // ── Legacy custom split (Map-based, backward-compatible) ─────────────────

    @Transactional
    public SplitResultDto splitCustom(Map<String, Object> requestMap) {
        Long tripId = requestMap.get("tripId") != null
                ? ((Number) requestMap.get("tripId")).longValue() : null;
        BigDecimal totalAmount = requestMap.get("totalAmount") != null
                ? new BigDecimal(requestMap.get("totalAmount").toString()) : BigDecimal.ZERO;
        String currencyCode = requestMap.get("currencyCode") != null
                ? (String) requestMap.get("currencyCode") : "INR";

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
                    .name(name != null ? name : "Participant")
                    .amount(amount)
                    .percentage(pct)
                    .build());
        }

        int travelers = participants.size();
        BigDecimal perPerson = totalAmount.divide(BigDecimal.valueOf(travelers), 2, RoundingMode.HALF_UP);
        return persistAndReturn(tripId, totalAmount, travelers, perPerson,
                participants, currencyCode, "CUSTOM_PERCENTAGE");
    }

    // ── Get by trip ───────────────────────────────────────────────────────────

    public SplitResultDto getByTripId(Long tripId) {
        return splitDetailRepository.findByTripId(tripId)
                .map(this::mapToDto)
                .orElseThrow(() -> new RuntimeException("Split not found for trip: " + tripId));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private SplitResultDto persistAndReturn(Long tripId, BigDecimal totalAmount,
                                             int travelers, BigDecimal perPerson,
                                             List<Participant> participants,
                                             String currencyCode, String splitMode) {
        SplitDetail detail = SplitDetail.builder()
                .tripId(tripId)
                .totalAmount(totalAmount)
                .travelers(travelers)
                .perPersonAmount(perPerson)
                .participants(participants)
                .currencyCode(currencyCode != null ? currencyCode : "INR")
                .splitMode(splitMode)
                .build();

        splitDetailRepository.findByTripId(tripId)
                .ifPresent(existing -> detail.setId(existing.getId()));
        splitDetailRepository.save(detail);

        return mapToDto(detail);
    }

    private SplitResultDto mapToDto(SplitDetail d) {
        String currency = d.getCurrencyCode() != null ? d.getCurrencyCode() : "INR";
        List<SplitResultDto.ParticipantDto> pDtos = d.getParticipants().stream()
                .map(p -> SplitResultDto.ParticipantDto.builder()
                        .participantId(p.getParticipantId())
                        .name(p.getName())
                        .email(p.getEmail())
                        .amount(p.getAmount())
                        .percentage(p.getPercentage())
                        .currencyCode(currency)
                        .build())
                .collect(Collectors.toList());

        return SplitResultDto.builder()
                .tripId(d.getTripId())
                .totalAmount(d.getTotalAmount())
                .travelers(d.getTravelers())
                .perPersonAmount(d.getPerPersonAmount())
                .participants(pDtos)
                .currencyCode(currency)
                .splitMode(d.getSplitMode() != null ? d.getSplitMode() : "EQUAL")
                .build();
    }
}
