package com.tripforge.split.repository;

import com.tripforge.split.entity.SplitDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SplitDetailRepository extends JpaRepository<SplitDetail, Long> {
    Optional<SplitDetail> findByTripId(Long tripId);
}
