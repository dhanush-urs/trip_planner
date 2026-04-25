package com.tripforge.trip.repository;

import com.tripforge.trip.entity.Trip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TripRepository extends JpaRepository<Trip, Long> {

    List<Trip> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT t FROM Trip t WHERE t.userId = :userId AND t.status = :status ORDER BY t.createdAt DESC")
    List<Trip> findByUserIdAndStatus(Long userId, Trip.TripStatus status);
}
