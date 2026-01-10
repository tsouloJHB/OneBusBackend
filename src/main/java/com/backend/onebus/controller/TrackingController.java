package com.backend.onebus.controller;

import com.backend.onebus.service.RouteGeometryService;
import com.backend.onebus.repository.RouteRepository;
import com.backend.onebus.model.Route;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;

/**
 * Controller for bus tracking operations including route-based distance calculations
 */
@RestController
@RequestMapping("/api/tracking")
@Tag(name = "Bus Tracking", description = "Endpoints for bus tracking and distance calculations")
public class TrackingController {
    
    private static final Logger logger = LoggerFactory.getLogger(TrackingController.class);
    
    @Autowired
    private RouteGeometryService routeGeometryService;
    
    @Autowired
    private RouteRepository routeRepository;
    
    /**
     * Calculate accurate distance between bus and user along the actual route path.
     * Uses linear referencing to snap both GPS positions to the route and calculate
     * distance along the polyline instead of straight-line distance.
     */
    @PostMapping("/distance")
    @Operation(summary = "Calculate route distance", 
               description = "Calculate distance between bus and user along the actual bus route using linear referencing")
    public ResponseEntity<?> calculateDistance(@Valid @RequestBody DistanceRequest request) {
        try {
            logger.info("Calculating route distance for bus {} ({}) from ({}, {}) to user ({}, {})",
                       request.getBusNumber(), request.getDirection(),
                       request.getBusLat(), request.getBusLon(),
                       request.getUserLat(), request.getUserLon());
            
            // Find the route by bus number
            List<Route> routes = routeRepository.findByBusNumber(request.getBusNumber());
            if (routes.isEmpty()) {
                logger.warn("No route found for bus number: {}", request.getBusNumber());
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", "Route not found for bus number: " + request.getBusNumber()));
            }
            
            // Find the route matching the direction
            Route route = routes.stream()
                .filter(r -> request.getDirection().equalsIgnoreCase(r.getDirection()))
                .findFirst()
                .orElse(routes.get(0)); // Fallback to first route if direction doesn't match
            
            // Calculate distance along the route
            RouteGeometryService.RouteDistanceResult result = routeGeometryService.calculateRouteDistance(
                request.getBusLat(),
                request.getBusLon(),
                request.getUserLat(),
                request.getUserLon(),
                route.getId(),
                request.getDirection()
            );
            
            if (result == null) {
                logger.error("Failed to calculate route distance - likely no full route geometry available");
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", "Route geometry not found. Please ensure the full route path is configured."));
            }
            
            DistanceResponse response = new DistanceResponse(
                result.distanceMeters,
                result.distanceKm,
                result.estimatedTimeMinutes,
                result.busSnapIndex,
                result.userSnapIndex,
                result.busProjectionDistance,
                result.userProjectionDistance
            );
            
            logger.info("Distance calculation successful: {} meters, ETA: {} minutes", 
                       result.distanceMeters, result.estimatedTimeMinutes);
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            logger.error("Error calculating route distance: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Collections.singletonMap("error", "Failed to calculate distance: " + e.getMessage()));
        }
    }
    
    // --- DTOs ---
    
    /**
     * Request for calculating distance along a route
     */
    public static class DistanceRequest {
        @NotBlank(message = "Bus number is required")
        private String busNumber;
        
        @NotBlank(message = "Direction is required")
        private String direction;
        
        @NotNull(message = "Bus latitude is required")
        private Double busLat;
        
        @NotNull(message = "Bus longitude is required")
        private Double busLon;
        
        @NotNull(message = "User latitude is required")
        private Double userLat;
        
        @NotNull(message = "User longitude is required")
        private Double userLon;
        
        // Getters and setters
        public String getBusNumber() { return busNumber; }
        public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
        
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        
        public Double getBusLat() { return busLat; }
        public void setBusLat(Double busLat) { this.busLat = busLat; }
        
        public Double getBusLon() { return busLon; }
        public void setBusLon(Double busLon) { this.busLon = busLon; }
        
        public Double getUserLat() { return userLat; }
        public void setUserLat(Double userLat) { this.userLat = userLat; }
        
        public Double getUserLon() { return userLon; }
        public void setUserLon(Double userLon) { this.userLon = userLon; }
    }
    
    /**
     * Response containing calculated distance and ETA
     */
    public static class DistanceResponse {
        private double distanceMeters;
        private double distanceKm;
        private double estimatedTimeMinutes;
        private int busSnapIndex;
        private int userSnapIndex;
        private double busProjectionDistance;
        private double userProjectionDistance;
        
        public DistanceResponse(double distanceMeters, double distanceKm, double estimatedTimeMinutes,
                               int busSnapIndex, int userSnapIndex, 
                               double busProjectionDistance, double userProjectionDistance) {
            this.distanceMeters = distanceMeters;
            this.distanceKm = distanceKm;
            this.estimatedTimeMinutes = estimatedTimeMinutes;
            this.busSnapIndex = busSnapIndex;
            this.userSnapIndex = userSnapIndex;
            this.busProjectionDistance = busProjectionDistance;
            this.userProjectionDistance = userProjectionDistance;
        }
        
        // Getters and setters
        public double getDistanceMeters() { return distanceMeters; }
        public void setDistanceMeters(double distanceMeters) { this.distanceMeters = distanceMeters; }
        
        public double getDistanceKm() { return distanceKm; }
        public void setDistanceKm(double distanceKm) { this.distanceKm = distanceKm; }
        
        public double getEstimatedTimeMinutes() { return estimatedTimeMinutes; }
        public void setEstimatedTimeMinutes(double estimatedTimeMinutes) { this.estimatedTimeMinutes = estimatedTimeMinutes; }
        
        public int getBusSnapIndex() { return busSnapIndex; }
        public void setBusSnapIndex(int busSnapIndex) { this.busSnapIndex = busSnapIndex; }
        
        public int getUserSnapIndex() { return userSnapIndex; }
        public void setUserSnapIndex(int userSnapIndex) { this.userSnapIndex = userSnapIndex; }
        
        public double getBusProjectionDistance() { return busProjectionDistance; }
        public void setBusProjectionDistance(double busProjectionDistance) { this.busProjectionDistance = busProjectionDistance; }
        
        public double getUserProjectionDistance() { return userProjectionDistance; }
        public void setUserProjectionDistance(double userProjectionDistance) { this.userProjectionDistance = userProjectionDistance; }
    }
}
