package com.tripforge.hotel.controller;

import com.tripforge.hotel.dto.ApiResponse;
import com.tripforge.hotel.dto.HotelChangeRequest;
import com.tripforge.hotel.dto.HotelDto;
import com.tripforge.hotel.service.HotelService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/hotels")
public class HotelController {

    @Autowired
    private HotelService hotelService;

    /** GET /api/hotels/recommend */
    @GetMapping("/recommend")
    public ResponseEntity<ApiResponse<List<HotelDto>>> recommend(
            @RequestParam String destination,
            @RequestParam BigDecimal budget,
            @RequestParam Integer durationDays,
            @RequestParam Integer travelers,
            @RequestParam(defaultValue = "STANDARD") String hotelPreference) {
        List<HotelDto> hotels = hotelService.recommendHotels(
                destination, budget, durationDays, travelers, hotelPreference);
        return ResponseEntity.ok(ApiResponse.success(hotels));
    }

    /** POST /api/hotels/change */
    @PostMapping("/change")
    public ResponseEntity<ApiResponse<List<HotelDto>>> changeHotel(
            @Valid @RequestBody HotelChangeRequest request) {
        List<HotelDto> alternatives = hotelService.changeHotel(request);
        return ResponseEntity.ok(ApiResponse.success("Alternative hotels found", alternatives));
    }

    /** GET /api/hotels/alternatives/{tripId} */
    @GetMapping("/alternatives/{tripId}")
    public ResponseEntity<ApiResponse<List<HotelDto>>> getAlternatives(
            @PathVariable Long tripId,
            @RequestParam String destination,
            @RequestParam(required = false) Long excludeHotelId) {
        List<HotelDto> alternatives = hotelService.getAlternatives(tripId, destination, excludeHotelId);
        return ResponseEntity.ok(ApiResponse.success(alternatives));
    }

    /** GET /api/hotels/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<HotelDto>> getById(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(hotelService.getHotelById(id)));
    }
}
