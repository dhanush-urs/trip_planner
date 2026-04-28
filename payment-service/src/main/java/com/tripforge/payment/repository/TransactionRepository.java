package com.tripforge.payment.repository;

import com.tripforge.payment.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByTripId(Long tripId);
    Optional<Transaction> findByGatewayOrderId(String gatewayOrderId);
    Optional<Transaction> findByGatewayPaymentId(String gatewayPaymentId);
    Optional<Transaction> findByIdempotencyKey(String idempotencyKey);
    List<Transaction> findByTripIdAndStatus(Long tripId, String status);
}
