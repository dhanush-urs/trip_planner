package com.tripforge.trip.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class TripSummaryResponse {
    private String headline;
    private String shortSummary;
    private boolean fallbackUsed;
    private String sourceProvider;
}
