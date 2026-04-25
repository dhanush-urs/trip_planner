package com.tripforge.trip.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ItineraryDayDto {
    private Integer dayNumber;
    private LocalDate date;
    private String theme;
    private List<TripPlaceDto> places;
}
