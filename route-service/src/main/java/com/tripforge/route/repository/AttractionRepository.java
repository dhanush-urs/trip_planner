package com.tripforge.route.repository;

import com.tripforge.route.entity.Attraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AttractionRepository extends JpaRepository<Attraction, Long> {

    List<Attraction> findByDestinationIgnoreCase(String destination);

    @Query("SELECT a FROM Attraction a WHERE LOWER(a.destination) = LOWER(:destination) ORDER BY a.priorityScore DESC")
    List<Attraction> findByDestinationOrderByPriority(String destination);
}
