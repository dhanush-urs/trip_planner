package com.tripforge.payment.repository;

import com.tripforge.payment.entity.ParticipantPaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ParticipantPaymentStatusRepository extends JpaRepository<ParticipantPaymentStatus, Long> {
    List<ParticipantPaymentStatus> findByTripId(Long tripId);
    Optional<ParticipantPaymentStatus> findByTripIdAndParticipantId(Long tripId, Long participantId);
}
