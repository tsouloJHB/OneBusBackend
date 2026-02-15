package com.backend.onebus.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LocationSearchResponse {
    private GeocodingResult geocodingResult;
    private List<RouteProximityResult> nearbyRoutes;
    private Integer totalRoutesFound;
    private Double searchRadiusKm;
}
