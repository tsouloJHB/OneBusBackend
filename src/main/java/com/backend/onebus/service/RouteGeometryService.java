package com.backend.onebus.service;

import com.backend.onebus.controller.FullRouteController.Coordinate;
import com.backend.onebus.model.FullRoute;
import com.backend.onebus.repository.FullRouteRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for route geometry calculations using linear referencing.
 * Provides snap-to-path and distance-along-route calculations for accurate bus tracking.
 */
@Service
public class RouteGeometryService {
    
    private static final Logger logger = LoggerFactory.getLogger(RouteGeometryService.class);
    private static final double EARTH_RADIUS_METERS = 6371000.0; // Earth radius in meters
    
    @Autowired
    private FullRouteRepository fullRouteRepository;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * Calculate cumulative distances along a route from the start point.
     * Returns an array where each element is the total distance from the start to that coordinate.
     * Example: [0, 50.2, 125.8, 200.3, ...] means point 0 is at start, point 1 is 50.2m from start, etc.
     * 
     * @param coordinates List of coordinates representing the route polyline
     * @return List of cumulative distances in meters
     */
    public List<Double> calculateCumulativeDistances(List<Coordinate> coordinates) {
        List<Double> cumulativeDistances = new ArrayList<>();
        
        if (coordinates == null || coordinates.isEmpty()) {
            return cumulativeDistances;
        }
        
        // First point is always at distance 0
        cumulativeDistances.add(0.0);
        double totalDistance = 0.0;
        
        for (int i = 1; i < coordinates.size(); i++) {
            Coordinate prev = coordinates.get(i - 1);
            Coordinate curr = coordinates.get(i);
            
            double segmentDistance = haversineDistance(
                prev.getLat(), prev.getLon(),
                curr.getLat(), curr.getLon()
            );
            
            totalDistance += segmentDistance;
            cumulativeDistances.add(totalDistance);
        }
        
        logger.debug("Calculated cumulative distances for {} points, total route length: {} meters", 
                    coordinates.size(), totalDistance);
        
        return cumulativeDistances;
    }
    
    /**
     * Snap a GPS coordinate to the closest point on a route polyline.
     * Handles GPS noise by finding the nearest point on the route and projecting onto it.
     * 
     * @param gpsLat GPS latitude
     * @param gpsLon GPS longitude
     * @param routeCoordinates Route polyline coordinates
     * @param cumulativeDistances Pre-calculated cumulative distances for the route
     * @return SnapResult containing the snapped position and cumulative distance
     */
    public SnapResult snapToPath(double gpsLat, double gpsLon, 
                                  List<Coordinate> routeCoordinates,
                                  List<Double> cumulativeDistances) {
        
        if (routeCoordinates == null || routeCoordinates.isEmpty()) {
            logger.warn("Cannot snap to path: route coordinates are empty");
            return null;
        }
        
        if (cumulativeDistances == null || cumulativeDistances.size() != routeCoordinates.size()) {
            logger.warn("Cannot snap to path: cumulative distances mismatch");
            return null;
        }
        
        double minDistance = Double.MAX_VALUE;
        int closestSegmentIndex = 0;
        double closestSegmentT = 0.0; // Parameter t in range [0, 1] for interpolation
        double closestLat = routeCoordinates.get(0).getLat();
        double closestLon = routeCoordinates.get(0).getLon();
        
        // Check each segment of the route
        for (int i = 0; i < routeCoordinates.size() - 1; i++) {
            Coordinate p1 = routeCoordinates.get(i);
            Coordinate p2 = routeCoordinates.get(i + 1);
            
            // Find the closest point on this segment to the GPS position
            PointOnSegment pointOnSegment = closestPointOnSegment(
                gpsLat, gpsLon,
                p1.getLat(), p1.getLon(),
                p2.getLat(), p2.getLon()
            );
            
            if (pointOnSegment.distance < minDistance) {
                minDistance = pointOnSegment.distance;
                closestSegmentIndex = i;
                closestSegmentT = pointOnSegment.t;
                closestLat = pointOnSegment.lat;
                closestLon = pointOnSegment.lon;
            }
        }
        
        // Calculate the interpolated cumulative distance
        double distanceAtSegmentStart = cumulativeDistances.get(closestSegmentIndex);
        double distanceAtSegmentEnd = cumulativeDistances.get(closestSegmentIndex + 1);
        double interpolatedDistance = distanceAtSegmentStart + 
            (distanceAtSegmentEnd - distanceAtSegmentStart) * closestSegmentT;
        
        logger.debug("Snapped GPS ({}, {}) to segment {} at t={}, cumulative distance: {} meters, projection distance: {} meters",
                    gpsLat, gpsLon, closestSegmentIndex, closestSegmentT, interpolatedDistance, minDistance);
        
        return new SnapResult(
            closestSegmentIndex,
            interpolatedDistance,
            closestLat,
            closestLon,
            minDistance
        );
    }
    
    /**
     * Calculate the distance along a route between two GPS positions.
     * Both positions are snapped to the route first, then the difference in cumulative distances is calculated.
     * 
     * @param busLat Bus GPS latitude
     * @param busLon Bus GPS longitude
     * @param userLat User GPS latitude
     * @param userLon User GPS longitude
     * @param routeId Route ID
     * @param direction Route direction (e.g., "Northbound", "Southbound")
     * @return RouteDistanceResult containing distance and other metadata
     */
    public RouteDistanceResult calculateRouteDistance(double busLat, double busLon,
                                                      double userLat, double userLon,
                                                      Long routeId, String direction) {
        try {
            // Find the full route
            List<FullRoute> routes = fullRouteRepository.findByRouteIdAndDirection(routeId, direction);
            if (routes.isEmpty()) {
                logger.warn("No full route found for routeId={}, direction={}", routeId, direction);
                return null;
            }
            
            FullRoute fullRoute = routes.get(0);
            
            // Parse coordinates and cumulative distances
            List<Coordinate> coordinates = parseCoordinates(fullRoute.getCoordinatesJson());
            List<Double> cumulativeDistances = parseCumulativeDistances(fullRoute.getCumulativeDistancesJson());
            
            // If cumulative distances are missing, calculate them on the fly
            if (cumulativeDistances == null || cumulativeDistances.isEmpty()) {
                logger.info("Cumulative distances not found for route {}, calculating on the fly", routeId);
                cumulativeDistances = calculateCumulativeDistances(coordinates);
            }
            
            // Snap bus and user to the route
            SnapResult busSnap = snapToPath(busLat, busLon, coordinates, cumulativeDistances);
            SnapResult userSnap = snapToPath(userLat, userLon, coordinates, cumulativeDistances);
            
            if (busSnap == null || userSnap == null) {
                logger.warn("Failed to snap positions to route");
                return null;
            }
            
            // Calculate distance along the route
            double distanceMeters = Math.abs(userSnap.cumulativeDistance - busSnap.cumulativeDistance);
            
            // Calculate ETA (assuming average speed of 30 km/h)
            double averageSpeedKmH = 30.0;
            double distanceKm = distanceMeters / 1000.0;
            double estimatedTimeMinutes = (distanceKm / averageSpeedKmH) * 60.0;
            
            logger.info("Route distance calculated: {} meters ({} km), ETA: {} minutes", 
                       distanceMeters, distanceKm, estimatedTimeMinutes);
            
            return new RouteDistanceResult(
                distanceMeters,
                distanceKm,
                estimatedTimeMinutes,
                busSnap.segmentIndex,
                userSnap.segmentIndex,
                busSnap.projectionDistance,
                userSnap.projectionDistance
            );
            
        } catch (Exception e) {
            logger.error("Error calculating route distance: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Haversine formula to calculate distance between two lat/lon points in meters.
     */
    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_METERS * c;
    }
    
    /**
     * Find the closest point on a line segment to a given point.
     * Uses vector projection to find the perpendicular point on the segment.
     */
    private PointOnSegment closestPointOnSegment(double px, double py,
                                                  double x1, double y1,
                                                  double x2, double y2) {
        // Vector from point 1 to point 2
        double dx = x2 - x1;
        double dy = y2 - y1;
        
        // If the segment is actually a point, return that point
        if (dx == 0 && dy == 0) {
            double distance = haversineDistance(px, py, x1, y1);
            return new PointOnSegment(x1, y1, 0.0, distance);
        }
        
        // Vector from point 1 to the GPS point
        double dpx = px - x1;
        double dpy = py - y1;
        
        // Calculate the projection parameter t
        // t = 0 means closest point is p1, t = 1 means closest point is p2
        double segmentLengthSquared = dx * dx + dy * dy;
        double t = (dpx * dx + dpy * dy) / segmentLengthSquared;
        
        // Clamp t to [0, 1] to stay on the segment
        t = Math.max(0.0, Math.min(1.0, t));
        
        // Calculate the closest point
        double closestLat = x1 + t * dx;
        double closestLon = y1 + t * dy;
        
        // Calculate distance from GPS point to closest point
        double distance = haversineDistance(px, py, closestLat, closestLon);
        
        return new PointOnSegment(closestLat, closestLon, t, distance);
    }
    
    /**
     * Parse coordinates JSON string to List of Coordinate objects
     */
    private List<Coordinate> parseCoordinates(String coordinatesJson) {
        try {
            if (coordinatesJson == null || coordinatesJson.isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(coordinatesJson, new TypeReference<List<Coordinate>>() {});
        } catch (Exception e) {
            logger.error("Error parsing coordinates JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Parse cumulative distances JSON string to List of Double
     */
    private List<Double> parseCumulativeDistances(String cumulativeDistancesJson) {
        try {
            if (cumulativeDistancesJson == null || cumulativeDistancesJson.isEmpty()) {
                return new ArrayList<>();
            }
            return objectMapper.readValue(cumulativeDistancesJson, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            logger.error("Error parsing cumulative distances JSON: {}", e.getMessage());
            return new ArrayList<>();
        }
    }
    
    // --- DTOs ---
    
    /**
     * Result of snapping a GPS position to a route
     */
    public static class SnapResult {
        public final int segmentIndex;          // Index of the closest segment
        public final double cumulativeDistance;  // Interpolated distance from route start
        public final double snappedLat;          // Latitude of snapped point on route
        public final double snappedLon;          // Longitude of snapped point on route
        public final double projectionDistance;  // Distance from GPS to snapped point (GPS noise)
        
        public SnapResult(int segmentIndex, double cumulativeDistance, 
                         double snappedLat, double snappedLon, double projectionDistance) {
            this.segmentIndex = segmentIndex;
            this.cumulativeDistance = cumulativeDistance;
            this.snappedLat = snappedLat;
            this.snappedLon = snappedLon;
            this.projectionDistance = projectionDistance;
        }
    }
    
    /**
     * Result of calculating distance along a route
     */
    public static class RouteDistanceResult {
        public final double distanceMeters;
        public final double distanceKm;
        public final double estimatedTimeMinutes;
        public final int busSnapIndex;
        public final int userSnapIndex;
        public final double busProjectionDistance;
        public final double userProjectionDistance;
        
        public RouteDistanceResult(double distanceMeters, double distanceKm, 
                                  double estimatedTimeMinutes, int busSnapIndex, int userSnapIndex,
                                  double busProjectionDistance, double userProjectionDistance) {
            this.distanceMeters = distanceMeters;
            this.distanceKm = distanceKm;
            this.estimatedTimeMinutes = estimatedTimeMinutes;
            this.busSnapIndex = busSnapIndex;
            this.userSnapIndex = userSnapIndex;
            this.busProjectionDistance = busProjectionDistance;
            this.userProjectionDistance = userProjectionDistance;
        }
    }
    
    /**
     * Helper class for closest point on segment calculation
     */
    private static class PointOnSegment {
        public final double lat;
        public final double lon;
        public final double t;         // Parameter in [0, 1]
        public final double distance;  // Distance from GPS point to this point
        
        public PointOnSegment(double lat, double lon, double t, double distance) {
            this.lat = lat;
            this.lon = lon;
            this.t = t;
            this.distance = distance;
        }
    }
}
