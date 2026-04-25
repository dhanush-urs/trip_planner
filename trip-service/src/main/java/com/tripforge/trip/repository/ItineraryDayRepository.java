package com.tripforge.trip.repository;

import com.tripforge.trip.entity.ItineraryDay;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ItineraryDayRepository extends JpaRepository<ItineraryDay, Long> {

    List<ItineraryDay> findByTripIdOrderByDayNumberAsc(Long tripId);
}
