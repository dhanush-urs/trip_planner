package com.tripforge.hotel.repository;

import com.tripforge.hotel.entity.Hotel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HotelRepository extends JpaRepository<Hotel, Long> {

    List<Hotel> findByDestinationIgnoreCase(String destination);

    @Query("SELECT h FROM Hotel h WHERE LOWER(h.destination) = LOWER(:destination) AND h.category = :category ORDER BY h.rating DESC")
    List<Hotel> findByDestinationAndCategory(String destination, String category);

    @Query("SELECT h FROM Hotel h WHERE LOWER(h.destination) = LOWER(:destination) AND h.pricePerNight <= :maxPrice ORDER BY h.rating DESC")
    List<Hotel> findByDestinationAndMaxPrice(String destination, Double maxPrice);

    @Query("SELECT h FROM Hotel h WHERE LOWER(h.destination) = LOWER(:destination) ORDER BY h.rating DESC")
    List<Hotel> findByDestinationOrderByRating(String destination);

    @Query("SELECT h FROM Hotel h WHERE LOWER(h.destination) = LOWER(:destination) ORDER BY h.distanceFromCenterKm ASC")
    List<Hotel> findByDestinationOrderByDistance(String destination);

    @Query("SELECT h FROM Hotel h WHERE LOWER(h.destination) = LOWER(:destination) ORDER BY h.pricePerNight ASC")
    List<Hotel> findByDestinationOrderByPrice(String destination);
}
