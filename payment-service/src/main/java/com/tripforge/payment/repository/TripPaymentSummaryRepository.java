package com.tripforge.payment.repository;

import com.tripforge.payment.entity.TripPaymentSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TripPaymentSummaryRepository extends JpaRepository<TripPaymentSummary, Long> {
    Optional<TripPaymentSummary> findByTripId(Long tripId);
}
