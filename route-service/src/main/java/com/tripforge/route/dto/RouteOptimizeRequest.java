package com.tripforge.route.dto;

import lombok.Data;
import java.time.LocalDate;
import java.util.List;

@Data
public class RouteOptimizeRequest {
    private Long tripId;
    private String destination;
    private LocalDate startDate;
    private Integer durationDays;
    private List<String> interests;
}
