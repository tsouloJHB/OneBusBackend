package com.backend.onebus.service;

import com.backend.onebus.dto.RouteProximityResult;
import com.backend.onebus.model.FullRoute;
import com.backend.onebus.model.RouteStop;
import com.backend.onebus.model.Route;
import com.backend.onebus.repository.FullRouteRepository;
import com.backend.onebus.repository.RouteStopRepository;
import com.backend.onebus.repository.RouteRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service for performing spatial searches on bus routes and stops.
 * Uses the Haversine formula for distance calculations.
 * In production, this should be replaced with PostGIS spatial queries for better performance.
 */
@Slf4j
@Service
public class SpatialSearchService {
    
    private final FullRouteRepository fullRouteRepository;
    private final RouteStopRepository routeStopRepository;
    private final RouteRepository routeRepository;
    private final ObjectMapper objectMapper;
    
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    public SpatialSearchService(
            FullRouteRepository fullRouteRepository,
            RouteStopRepository routeStopRepository,
            RouteRepository routeRepository,
            ObjectMapper objectMapper) {
        this.fullRouteRepository = fullRouteRepository;
        this.routeStopRepository = routeStopRepository;
        this.routeRepository = routeRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Find all routes within a specified radius of a point
     */
    public List<RouteProximityResult> findRoutesNearLocation(
            Double targetLat, 
            Double targetLon, 
            Double radiusKm) {
        
        log.info("Searching for routes within {}km of ({}, {})", radiusKm, targetLat, targetLon);
        
        List<RouteProximityResult> results = new ArrayList<>();
        List<FullRoute> allRoutes = fullRouteRepository.findAll();
        
        for (FullRoute fullRoute : allRoutes) {
            try {
                // Parse route coordinates
                List<Map<String, Double>> coordinates = objectMapper.readValue(
                    fullRoute.getCoordinatesJson(), 
                    new TypeReference<List<Map<String, Double>>>() {}
                );
                
                // Find minimum distance from target to any point on route
                Double minDistance = findMinimumDistanceToRoute(targetLat, targetLon, coordinates);
                
                if (minDistance != null && minDistance <= radiusKm) {
                    // Find nearby stops
                    List<RouteProximityResult.NearbyStop> nearbyStops = 
                        findNearbyStops(fullRoute.getRouteId(), targetLat, targetLon, radiusKm);
                    
                    // Get route info
                    Route route = routeRepository.findById(fullRoute.getRouteId()).orElse(null);
                    
                    RouteProximityResult result = RouteProximityResult.builder()
                        .routeId(fullRoute.getRouteId())
                        .routeName(fullRoute.getName())
                        .direction(fullRoute.getDirection())
                        .distanceKm(minDistance)
                        .nearbyStops(nearbyStops)
                        .companyId(fullRoute.getCompanyId())
                        .companyName(route != null && route.getCompany() != null ? 
                            route.getCompany() : "Unknown")
                        .build();
                    
                    results.add(result);
                }
                
            } catch (Exception e) {
                log.error("Error processing route {}: {}", fullRoute.getId(), e.getMessage());
            }
        }
        
        // Sort by distance
        results.sort(Comparator.comparing(RouteProximityResult::getDistanceKm));
        
        log.info("Found {} routes within {}km", results.size(), radiusKm);
        return results;
    }
    
    /**
     * Find nearby stops for a specific route
     */
    private List<RouteProximityResult.NearbyStop> findNearbyStops(
            Long routeId, 
            Double targetLat, 
            Double targetLon, 
            Double radiusKm) {
        
        List<RouteStop> allStops = routeStopRepository.findByRouteIdOrderByBusStopIndex(routeId);
        List<RouteProximityResult.NearbyStop> nearbyStops = new ArrayList<>();
        
        for (RouteStop stop : allStops) {
            Double distance = calculateDistance(targetLat, targetLon, stop.getLatitude(), stop.getLongitude());
            
            if (distance <= radiusKm) {
                RouteProximityResult.NearbyStop nearbyStop = RouteProximityResult.NearbyStop.builder()
                    .stopId(stop.getId())
                    .address(stop.getAddress())
                    .latitude(stop.getLatitude())
                    .longitude(stop.getLongitude())
                    .distanceKm(distance)
                    .stopIndex(stop.getBusStopIndex())
                    .build();
                nearbyStops.add(nearbyStop);
            }
        }
        
        // Sort by distance
        nearbyStops.sort(Comparator.comparing(RouteProximityResult.NearbyStop::getDistanceKm));
        
        return nearbyStops;
    }
    
    /**
     * Find minimum distance from a point to a route (LineString)
     */
    private Double findMinimumDistanceToRoute(
            Double targetLat, 
            Double targetLon, 
            List<Map<String, Double>> routeCoordinates) {
        
        if (routeCoordinates == null || routeCoordinates.isEmpty()) {
            return null;
        }
        
        Double minDistance = Double.MAX_VALUE;
        
        // Check distance to each point on the route
        for (Map<String, Double> coord : routeCoordinates) {
            Double lat = coord.get("lat");
            Double lon = coord.get("lon");
            
            if (lat != null && lon != null) {
                Double distance = calculateDistance(targetLat, targetLon, lat, lon);
                minDistance = Math.min(minDistance, distance);
            }
        }
        
        // Also check distance to line segments (more accurate)
        for (int i = 0; i < routeCoordinates.size() - 1; i++) {
            Map<String, Double> p1 = routeCoordinates.get(i);
            Map<String, Double> p2 = routeCoordinates.get(i + 1);
            
            Double lat1 = p1.get("lat");
            Double lon1 = p1.get("lon");
            Double lat2 = p2.get("lat");
            Double lon2 = p2.get("lon");
            
            if (lat1 != null && lon1 != null && lat2 != null && lon2 != null) {
                Double distance = distanceToLineSegment(targetLat, targetLon, lat1, lon1, lat2, lon2);
                minDistance = Math.min(minDistance, distance);
            }
        }
        
        return minDistance == Double.MAX_VALUE ? null : minDistance;
    }
    
    /**
     * Calculate distance from point to line segment
     */
    private Double distanceToLineSegment(
            Double px, Double py, 
            Double x1, Double y1, 
            Double x2, Double y2) {
        
        // Vector from point 1 to point 2
        double dx = x2 - x1;
        double dy = y2 - y1;
        
        // If the line segment is actually a point
        if (dx == 0 && dy == 0) {
            return calculateDistance(px, py, x1, y1);
        }
        
        // Calculate the parameter t
        double t = ((px - x1) * dx + (py - y1) * dy) / (dx * dx + dy * dy);
        
        // Clamp t to the segment
        t = Math.max(0, Math.min(1, t));
        
        // Find the nearest point on the segment
        double nearestX = x1 + t * dx;
        double nearestY = y1 + t * dy;
        
        return calculateDistance(px, py, nearestX, nearestY);
    }
    
    /**
     * Calculate distance between two points using Haversine formula
     */
    public Double calculateDistance(Double lat1, Double lon1, Double lat2, Double lon2) {
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                   Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return EARTH_RADIUS_KM * c;
    }
}
