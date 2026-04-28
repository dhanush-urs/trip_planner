package com.tripforge.trip.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
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
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ItineraryDayDto {
    private Integer dayNumber;
    private LocalDate date;
    private String theme;
    private List<TripPlaceDto> places;

    // Phase 9C additions
    private String sourceProvider;
    private boolean fallbackUsed;
}
