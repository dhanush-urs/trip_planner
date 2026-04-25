package com.tripforge.budget.repository;

import com.tripforge.budget.entity.BudgetBreakdown;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BudgetBreakdownRepository extends JpaRepository<BudgetBreakdown, Long> {
    Optional<BudgetBreakdown> findByTripId(Long tripId);
}
