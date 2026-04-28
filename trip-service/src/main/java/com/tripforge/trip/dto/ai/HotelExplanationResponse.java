package com.tripforge.trip.dto.ai;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class HotelExplanationResponse {
    private String summary;
    private List<String> bullets;
    private boolean fallbackUsed;
    private String sourceProvider;
}
