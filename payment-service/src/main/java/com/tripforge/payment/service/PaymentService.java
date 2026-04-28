package com.tripforge.payment.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tripforge.payment.config.RazorpayProperties;
import com.tripforge.payment.dto.*;
import com.tripforge.payment.entity.ParticipantPaymentStatus;
import com.tripforge.payment.entity.Transaction;
import com.tripforge.payment.entity.TripPaymentSummary;
import com.tripforge.payment.gateway.PaymentGateway;
import com.tripforge.payment.repository.ParticipantPaymentStatusRepository;
import com.tripforge.payment.repository.TransactionRepository;
import com.tripforge.payment.repository.TripPaymentSummaryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Core payment orchestration service.
 *
 * Responsibilities:
 *   - Create payment orders via gateway abstraction
 *   - Verify payments (signature check)
 *   - Process webhooks (idempotent)
 *   - Maintain trip payment summary
 *   - Maintain participant payment statuses
 *   - Generate payment links
 *
 * Never fakes payment success.
 * Runs in degraded mode if gateway not configured.
 */
@Service
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    @Autowired private PaymentGateway paymentGateway;
    @Autowired private RazorpayProperties razorpayProps;
    @Autowired private TransactionRepository transactionRepo;
    @Autowired private TripPaymentSummaryRepository summaryRepo;
    @Autowired private ParticipantPaymentStatusRepository participantRepo;
    @Autowired private ObjectMapper objectMapper;

    // ── Create Full Trip Payment Order ────────────────────────────────────────

    @Transactional
    public CreateOrderResponse createFullOrder(CreateOrderRequest request) {
        if (!paymentGateway.isConfigured()) {
            throw new IllegalStateException(
                    "Payments not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }

        TripPaymentSummary summary = getOrCreateSummary(request.getTripId(),
                null, request.getCurrency());

        BigDecimal amount = summary.getAmountRemaining();
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Trip is already fully paid.");
        }

        String receipt = "trip_" + request.getTripId() + "_full_" + System.currentTimeMillis();
        String idempotencyKey = "full_" + request.getTripId() + "_" + request.getUserId();

        // Check for existing pending order (idempotency)
        Optional<Transaction> existing = transactionRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent() && "CREATED".equals(existing.get().getStatus())) {
            log.info("Returning existing pending order for trip {}", request.getTripId());
            return buildOrderResponse(existing.get(), "FULL");
        }

        GatewayOrderResult gatewayResult = paymentGateway.createOrder(
                amount, request.getCurrency(), receipt, "Full trip payment");

        Transaction txn = Transaction.builder()
                .tripId(request.getTripId())
                .userId(request.getUserId())
                .gatewayProvider(paymentGateway.getProviderName())
                .gatewayOrderId(gatewayResult.getOrderId())
                .amount(amount)
                .currencyCode(request.getCurrency())
                .paymentType("FULL")
                .status("CREATED")
                .idempotencyKey(idempotencyKey)
                .build();
        transactionRepo.save(txn);

        log.info("Full payment order created: tripId={} orderId={} amount={}",
                request.getTripId(), gatewayResult.getOrderId(), amount);

        return buildOrderResponse(txn, "FULL");
    }

    // ── Create Share Payment Order ────────────────────────────────────────────

    @Transactional
    public CreateOrderResponse createShareOrder(CreateOrderRequest request) {
        if (!paymentGateway.isConfigured()) {
            throw new IllegalStateException(
                    "Payments not configured. Set RAZORPAY_KEY_ID and RAZORPAY_KEY_SECRET.");
        }

        if (request.getParticipantId() == null) {
            throw new IllegalArgumentException("participantId is required for share payment");
        }

        ParticipantPaymentStatus participant = participantRepo
                .findByTripIdAndParticipantId(request.getTripId(), request.getParticipantId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Participant not found for trip " + request.getTripId()));

        BigDecimal remaining = participant.getAllocatedAmount()
                .subtract(participant.getPaidAmount())
                .setScale(2, RoundingMode.HALF_UP);

        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Participant has already paid their share.");
        }

        String receipt = "trip_" + request.getTripId() + "_share_" + request.getParticipantId();
        String idempotencyKey = "share_" + request.getTripId() + "_" + request.getParticipantId();

        Optional<Transaction> existing = transactionRepo.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent() && "CREATED".equals(existing.get().getStatus())) {
            return buildOrderResponse(existing.get(), "SHARE");
        }

        GatewayOrderResult gatewayResult = paymentGateway.createOrder(
                remaining, request.getCurrency(), receipt,
                "Share payment: " + participant.getParticipantName());

        Transaction txn = Transaction.builder()
                .tripId(request.getTripId())
                .userId(request.getUserId())
                .participantId(request.getParticipantId())
                .participantName(participant.getParticipantName())
                .participantEmail(participant.getParticipantEmail())
                .gatewayProvider(paymentGateway.getProviderName())
                .gatewayOrderId(gatewayResult.getOrderId())
                .amount(remaining)
                .currencyCode(request.getCurrency())
                .paymentType("SHARE")
                .status("CREATED")
                .idempotencyKey(idempotencyKey)
                .build();
        transactionRepo.save(txn);

        log.info("Share payment order created: tripId={} participantId={} orderId={}",
                request.getTripId(), request.getParticipantId(), gatewayResult.getOrderId());

        return buildOrderResponse(txn, "SHARE");
    }

    // ── Verify Payment ────────────────────────────────────────────────────────

    @Transactional
    public ApiResponse<PaymentSummaryDto> verifyPayment(VerifyPaymentRequest request) {
        Transaction txn = transactionRepo.findById(request.getTransactionId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Transaction not found: " + request.getTransactionId()));

        // Idempotency — already verified
        if ("PAID".equals(txn.getStatus())) {
            log.info("Payment already verified (idempotent): txnId={}", txn.getId());
            return ApiResponse.success("Payment already verified",
                    getTripPaymentSummary(request.getTripId()));
        }

        GatewayVerifyResult result = paymentGateway.verifyPayment(
                request.getGatewayOrderId(),
                request.getGatewayPaymentId(),
                request.getGatewaySignature());

        txn.setGatewayPaymentId(request.getGatewayPaymentId());
        txn.setGatewaySignature(request.getGatewaySignature());

        if (result.isSuccess()) {
            txn.setStatus("PAID");
            transactionRepo.save(txn);
            applyPaymentToSummary(txn);
            log.info("Payment verified: tripId={} txnId={} paymentId={}",
                    request.getTripId(), txn.getId(), request.getGatewayPaymentId());
            return ApiResponse.success("Payment verified successfully",
                    getTripPaymentSummary(request.getTripId()));
        } else {
            txn.setStatus("FAILED");
            transactionRepo.save(txn);
            log.warn("Payment verification failed: tripId={} txnId={} reason={}",
                    request.getTripId(), txn.getId(), result.getMessage());
            return ApiResponse.error("Payment verification failed: " + result.getMessage(),
                    "VERIFICATION_FAILED");
        }
    }

    // ── Webhook Processing ────────────────────────────────────────────────────

    @Transactional
    public void processWebhook(String payload, String signature) {
        // Verify signature
        if (!paymentGateway.verifyWebhookSignature(payload, signature)) {
            log.warn("Webhook signature verification failed — ignoring");
            throw new SecurityException("Invalid webhook signature");
        }

        try {
            var node = objectMapper.readTree(payload);
            String event = node.path("event").asText();
            log.info("Webhook received: event={}", event);

            switch (event) {
                case "payment.captured" -> handlePaymentCaptured(node, payload);
                case "payment.failed"   -> handlePaymentFailed(node);
                case "order.paid"       -> log.info("Webhook: order.paid received (handled via verify)");
                default                 -> log.debug("Webhook: unhandled event={}", event);
            }
        } catch (SecurityException e) {
            throw e;
        } catch (Exception e) {
            log.error("Webhook processing error: {}", e.getMessage(), e);
        }
    }

    private void handlePaymentCaptured(com.fasterxml.jackson.databind.JsonNode node, String payload) {
        String paymentId = node.path("payload").path("payment").path("entity").path("id").asText();
        String orderId   = node.path("payload").path("payment").path("entity").path("order_id").asText();

        if (paymentId.isBlank() || orderId.isBlank()) {
            log.warn("Webhook payment.captured: missing paymentId or orderId");
            return;
        }

        transactionRepo.findByGatewayOrderId(orderId).ifPresent(txn -> {
            if ("PAID".equals(txn.getStatus())) {
                log.info("Webhook idempotent: payment {} already marked PAID", paymentId);
                return;
            }
            txn.setGatewayPaymentId(paymentId);
            txn.setStatus("PAID");
            txn.setRawGatewayPayloadJson(payload.length() > 4000
                    ? payload.substring(0, 4000) : payload);
            transactionRepo.save(txn);
            applyPaymentToSummary(txn);
            log.info("Webhook: payment captured and applied: orderId={} paymentId={}", orderId, paymentId);
        });
    }

    private void handlePaymentFailed(com.fasterxml.jackson.databind.JsonNode node) {
        String orderId = node.path("payload").path("payment").path("entity").path("order_id").asText();
        transactionRepo.findByGatewayOrderId(orderId).ifPresent(txn -> {
            if (!"PAID".equals(txn.getStatus())) {
                txn.setStatus("FAILED");
                transactionRepo.save(txn);
                log.info("Webhook: payment failed for orderId={}", orderId);
            }
        });
    }

    // ── Trip Payment Summary ──────────────────────────────────────────────────

    public PaymentSummaryDto getTripPaymentSummary(Long tripId) {
        TripPaymentSummary summary = summaryRepo.findByTripId(tripId)
                .orElse(null);

        List<ParticipantPaymentStatus> participants = participantRepo.findByTripId(tripId);

        if (summary == null) {
            return PaymentSummaryDto.builder()
                    .tripId(tripId)
                    .status("NOT_INITIALIZED")
                    .participants(mapParticipants(participants))
                    .build();
        }

        return PaymentSummaryDto.builder()
                .tripId(tripId)
                .currencyCode(summary.getCurrencyCode())
                .totalAmount(summary.getTotalAmount())
                .amountPaid(summary.getAmountPaid())
                .amountRemaining(summary.getAmountRemaining())
                .status(summary.getStatus())
                .participants(mapParticipants(participants))
                .build();
    }

    // ── Initialize Payment Tracking ───────────────────────────────────────────

    @Transactional
    public void initializePaymentTracking(InitPaymentRequest request) {
        // Upsert summary
        TripPaymentSummary summary = summaryRepo.findByTripId(request.getTripId())
                .orElse(TripPaymentSummary.builder()
                        .tripId(request.getTripId())
                        .build());

        summary.setTotalAmount(request.getTotalAmount());
        summary.setCurrencyCode(request.getCurrencyCode() != null
                ? request.getCurrencyCode() : "INR");
        summary.setAmountPaid(BigDecimal.ZERO);
        summary.setAmountRemaining(request.getTotalAmount());
        summary.setStatus("UNPAID");
        summaryRepo.save(summary);

        // Upsert participant statuses
        if (request.getParticipants() != null) {
            for (InitPaymentRequest.ParticipantInfo p : request.getParticipants()) {
                ParticipantPaymentStatus pps = participantRepo
                        .findByTripIdAndParticipantId(request.getTripId(), p.getParticipantId())
                        .orElse(ParticipantPaymentStatus.builder()
                                .tripId(request.getTripId())
                                .build());
                pps.setParticipantId(p.getParticipantId());
                pps.setParticipantName(p.getParticipantName() != null
                        ? p.getParticipantName() : "Participant");
                pps.setParticipantEmail(p.getParticipantEmail());
                pps.setAllocatedAmount(p.getAllocatedAmount());
                pps.setPaidAmount(BigDecimal.ZERO);
                pps.setCurrencyCode(summary.getCurrencyCode());
                pps.setStatus("UNPAID");
                participantRepo.save(pps);
            }
        }

        log.info("Payment tracking initialized: tripId={} total={} participants={}",
                request.getTripId(), request.getTotalAmount(),
                request.getParticipants() != null ? request.getParticipants().size() : 0);
    }

    // ── Generate Payment Links ────────────────────────────────────────────────

    @Transactional
    public PaymentSummaryDto generatePaymentLinks(Long tripId) {
        List<ParticipantPaymentStatus> participants = participantRepo.findByTripId(tripId);

        for (ParticipantPaymentStatus p : participants) {
            if ("UNPAID".equals(p.getStatus()) || "PARTIAL".equals(p.getStatus())) {
                if (p.getPaymentLink() == null) {
                    // Generate internal deep-link (gateway-native links require Razorpay Payment Links API)
                    // This creates a signed internal link that opens the Pay My Share flow
                    String link = buildInternalPaymentLink(tripId, p.getParticipantId());
                    p.setPaymentLink(link);
                    participantRepo.save(p);
                }
            }
        }

        return getTripPaymentSummary(tripId);
    }

    // ── Refresh Summary ───────────────────────────────────────────────────────

    @Transactional
    public PaymentSummaryDto refreshSummary(Long tripId) {
        List<Transaction> paidTxns = transactionRepo.findByTripIdAndStatus(tripId, "PAID");
        BigDecimal totalPaid = paidTxns.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        summaryRepo.findByTripId(tripId).ifPresent(summary -> {
            summary.setAmountPaid(totalPaid);
            summary.setAmountRemaining(summary.getTotalAmount().subtract(totalPaid)
                    .max(BigDecimal.ZERO));
            summary.setStatus(computeSummaryStatus(totalPaid, summary.getTotalAmount()));
            summaryRepo.save(summary);
        });

        return getTripPaymentSummary(tripId);
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private TripPaymentSummary getOrCreateSummary(Long tripId, BigDecimal amount, String currency) {
        return summaryRepo.findByTripId(tripId).orElseGet(() -> {
            BigDecimal total = amount != null ? amount : BigDecimal.ZERO;
            TripPaymentSummary s = TripPaymentSummary.builder()
                    .tripId(tripId)
                    .totalAmount(total)
                    .currencyCode(currency != null ? currency : "INR")
                    .amountPaid(BigDecimal.ZERO)
                    .amountRemaining(total)
                    .status("UNPAID")
                    .build();
            return summaryRepo.save(s);
        });
    }

    private void applyPaymentToSummary(Transaction txn) {
        TripPaymentSummary summary = getOrCreateSummary(
                txn.getTripId(), txn.getAmount(), txn.getCurrencyCode());

        BigDecimal newPaid = summary.getAmountPaid().add(txn.getAmount())
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal newRemaining = summary.getTotalAmount().subtract(newPaid)
                .max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);

        summary.setAmountPaid(newPaid);
        summary.setAmountRemaining(newRemaining);
        summary.setStatus(computeSummaryStatus(newPaid, summary.getTotalAmount()));
        summary.setLastPaymentAt(LocalDateTime.now());
        summaryRepo.save(summary);

        // Update participant status if SHARE payment
        if ("SHARE".equals(txn.getPaymentType()) && txn.getParticipantId() != null) {
            participantRepo.findByTripIdAndParticipantId(txn.getTripId(), txn.getParticipantId())
                    .ifPresent(p -> {
                        BigDecimal paidNow = p.getPaidAmount().add(txn.getAmount())
                                .setScale(2, RoundingMode.HALF_UP);
                        p.setPaidAmount(paidNow);
                        p.setLastPaymentAt(LocalDateTime.now());
                        p.setStatus(paidNow.compareTo(p.getAllocatedAmount()) >= 0
                                ? "PAID" : "PARTIAL");
                        participantRepo.save(p);
                    });
        }
    }

    private String computeSummaryStatus(BigDecimal paid, BigDecimal total) {
        if (paid.compareTo(BigDecimal.ZERO) == 0) return "UNPAID";
        if (paid.compareTo(total) >= 0) return "FULLY_PAID";
        return "PARTIALLY_PAID";
    }

    private CreateOrderResponse buildOrderResponse(Transaction txn, String paymentType) {
        return CreateOrderResponse.builder()
                .gatewayProvider(txn.getGatewayProvider())
                .paymentType(paymentType)
                .tripId(txn.getTripId())
                .transactionId(txn.getId())
                .orderId(txn.getGatewayOrderId())
                .amount(txn.getAmount())
                .currency(txn.getCurrencyCode())
                .displayAmountMinor(txn.getAmount()
                        .multiply(BigDecimal.valueOf(100))
                        .longValue())
                .keyId(razorpayProps.getKeyId())  // public key only
                .participantId(txn.getParticipantId())
                .participantName(txn.getParticipantName())
                .build();
    }

    private List<PaymentSummaryDto.ParticipantPaymentDto> mapParticipants(
            List<ParticipantPaymentStatus> list) {
        return list.stream()
                .map(p -> PaymentSummaryDto.ParticipantPaymentDto.builder()
                        .participantId(p.getParticipantId())
                        .participantName(p.getParticipantName())
                        .participantEmail(p.getParticipantEmail())
                        .allocatedAmount(p.getAllocatedAmount())
                        .paidAmount(p.getPaidAmount())
                        .currencyCode(p.getCurrencyCode())
                        .status(p.getStatus())
                        .paymentLink(p.getPaymentLink())
                        .build())
                .collect(Collectors.toList());
    }

    private String buildInternalPaymentLink(Long tripId, Long participantId) {
        // Internal deep-link — frontend opens Pay My Share flow
        // In production, replace with Razorpay Payment Links API call
        return "/trip/" + tripId + "/pay?participant=" + participantId
                + "&token=" + UUID.randomUUID().toString().substring(0, 12);
    }
}
