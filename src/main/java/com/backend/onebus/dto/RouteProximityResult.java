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
public class RouteProximityResult {
    private Long routeId;
    private String routeName;
    private String direction;
    private Double distanceKm; // Distance from search point to nearest point on route
    private List<NearbyStop> nearbyStops;
    private String companyName;
    private Long companyId;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NearbyStop {
        private Long stopId;
        private String address;
        private Double latitude;
        private Double longitude;
        private Double distanceKm;
        private Integer stopIndex;
    }
}
