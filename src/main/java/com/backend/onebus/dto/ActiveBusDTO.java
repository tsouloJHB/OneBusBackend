package com.backend.onebus.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ActiveBusDTO {
    private String id;
    
    // Bus information
    private BusInfo bus;
    
    // Route information
    private RouteInfo route;
    
    // Current location
    private LocationInfo currentLocation;
    
    // Next stop info
    private StopInfo nextStop;
    
    // Last stop info
    private StopInfo lastStop;
    
    // Bus status
    private String status; // 'on_route', 'at_stop', 'delayed'
    
    // Estimated arrival time
    private Long estimatedArrival;
    
    // Passenger count
    private Integer passengerCount;
    
    // Last updated timestamp
    private Long lastUpdated;
    
    // Additional telemetry
    private Double speedKmh;
    private Double headingDegrees;
    private String headingCardinal;
    
    // Bus company
    private String busCompany;
    
    // Trip direction
    private String tripDirection;
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BusInfo {
        private String id;
        private String busNumber;
        private String trackerImei;
        private String driverId;
        private String driverName;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RouteInfo {
        private Long id;
        private String routeName;
        private String company;
        private String busNumber;
        private String description;
        private String direction;
        private String startPoint;
        private String endPoint;
        private Boolean active;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LocationInfo {
        private Double lat;
        private Double lng;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StopInfo {
        private String id;
        private String name;
        private Double lat;
        private Double lng;
        private Integer order;
    }
}
