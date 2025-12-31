package com.backend.onebus.service.routing;

import com.backend.onebus.model.BusLocation;
import com.backend.onebus.model.Route;
import com.backend.onebus.model.RouteStop;
import com.backend.onebus.repository.RouteStopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Base abstract class for bus company routing strategies.
 * 
 * Global Rules (apply to ALL bus companies):
 * 1. First GPS point with NO direction + busStopIndex is 0/null → Direction = "Southbound"
 * 2. Bus reaches end of route (busStopIndex = total stops in direction) → Switch to opposite direction
 * 
 * Company-specific rules can override or extend these defaults.
 */
public abstract class BusCompanyRoutingStrategy {
    
    protected static final Logger logger = LoggerFactory.getLogger(BusCompanyRoutingStrategy.class);
    protected static final double STOP_PROXIMITY_METERS = 30.0;
    
    protected RouteStopRepository routeStopRepository;
    
    public void setRouteStopRepository(RouteStopRepository routeStopRepository) {
        this.routeStopRepository = routeStopRepository;
    }
    
    /**
     * Apply global routing rules that apply to all bus companies.
     * This is called before company-specific inference.
     * 
     * Global Rule 1: First GPS point with no direction and busStopIndex = 0/null → Southbound
     * Global Rule 2: Bus at end of route (busStopIndex = total stops) → Switch direction
     * 
     * @param current Current bus location
     * @param previous Previous bus location (can be null for first GPS point)
     * @param route The route the bus is operating on
     * @return The direction after applying global rules, or null if no rule applies
     */
    public String applyGlobalRules(BusLocation current, BusLocation previous, Route route, Long companyId, com.backend.onebus.service.RuleEngineService ruleEngineService) {
        // Global Rule 1: First GPS point - no direction + busStopIndex is 0/null
        boolean allowAutoFlip = ruleEngineService == null || ruleEngineService.isEnabled(companyId, com.backend.onebus.constants.RuleConstants.AUTO_FLIP_AT_TERMINAL, true);
        boolean allowFirstPointDefault = ruleEngineService == null || ruleEngineService.isEnabled(companyId, com.backend.onebus.constants.RuleConstants.FIRST_GPS_SOUTHBOUND, true);

        if (previous == null && current.getTripDirection() == null && allowFirstPointDefault) {
            if (current.getBusStopIndex() == null || current.getBusStopIndex() == 0) {
                logger.info("[Global Rule 1] Bus {} first GPS point with no direction and index 0/null - assigning Southbound", 
                    current.getBusNumber());
                return "Southbound";
            }
        }
        
        // Global Rule 2: Bus reached end of route - switch direction
        if (allowAutoFlip && current.getTripDirection() != null && current.getBusStopIndex() != null && route != null) {
            int totalStops = getTotalStopsInDirection(route, current.getTripDirection());
            
            if (totalStops > 0 && current.getBusStopIndex() == totalStops) {
                String newDirection = switchDirection(current.getTripDirection());
                logger.info("[Global Rule 2] Bus {} reached end of route (stop {}/{}), switching {} → {}", 
                    current.getBusNumber(), current.getBusStopIndex(), totalStops, 
                    current.getTripDirection(), newDirection);
                return newDirection;
            }
        }
        
        // No global rule applies
        return null;
    }
    
    /**
     * Infer the direction a bus is traveling based on current and previous locations.
     * This is the main method that uses company-specific logic.
     * 
     * @param current Current bus location
     * @param previous Previous bus location (can be null for first GPS point)
     * @param route The route the bus is operating on
     * @return The inferred direction (e.g., "Northbound", "Southbound"), or null if unable to determine
     */
    public abstract String inferDirection(BusLocation current, BusLocation previous, Route route);
    
    /**
     * Determine what to do when a bus reaches the end of its route.
     * Different companies may have different behaviors (turn around, go to depot, etc.)
     * 
     * @param current Current bus location
     * @param route The route
     * @return The new direction, or null to keep current direction
     */
    public abstract String handleEndOfRoute(BusLocation current, Route route);
    
    // ============ Common Utility Methods (Shared by all strategies) ============
    
    /**
     * Get total number of stops in a given direction for a route
     */
    protected int getTotalStopsInDirection(Route route, String direction) {
        if (route == null || routeStopRepository == null) return 0;
        try {
            List<RouteStop> stops = routeStopRepository
                .findByRouteIdAndDirectionOrderByBusStopIndex(route.getId(), direction);
            return stops.size();
        } catch (Exception e) {
            logger.error("Error getting total stops for route {} direction {}: {}", 
                route.getId(), direction, e.getMessage());
            return 0;
        }
    }
    
    /**
     * Check if a bus location is near a specific stop
     */
    protected boolean isNearStop(BusLocation location, RouteStop stop, double proximityMeters) {
        if (stop == null || location == null) return false;
        double distance = distanceMeters(
            location.getLat(), location.getLon(),
            stop.getLatitude(), stop.getLongitude()
        );
        return distance <= proximityMeters;
    }
    
    /**
     * Find the first stop of a route in a given direction
     */
    protected RouteStop getFirstStop(Route route, String direction) {
        if (route == null || routeStopRepository == null) return null;
        try {
            List<RouteStop> stops = routeStopRepository
                .findByRouteIdAndDirectionOrderByBusStopIndex(route.getId(), direction);
            return stops.isEmpty() ? null : stops.get(0);
        } catch (Exception e) {
            logger.error("Error getting first stop: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Find the last stop of a route in a given direction
     */
    protected RouteStop getLastStop(Route route, String direction) {
        if (route == null || routeStopRepository == null) return null;
        try {
            List<RouteStop> stops = routeStopRepository
                .findByRouteIdAndDirectionOrderByBusStopIndex(route.getId(), direction);
            return stops.isEmpty() ? null : stops.get(stops.size() - 1);
        } catch (Exception e) {
            logger.error("Error getting last stop: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Check if bus is at the start of a route in a given direction
     */
    protected boolean isAtStartStop(BusLocation location, Route route, String direction) {
        RouteStop startStop = getFirstStop(route, direction);
        return isNearStop(location, startStop, STOP_PROXIMITY_METERS);
    }
    
    /**
     * Check if bus is at the end of a route in a given direction
     */
    protected boolean isAtEndStop(BusLocation location, Route route, String direction) {
        RouteStop endStop = getLastStop(route, direction);
        return isNearStop(location, endStop, STOP_PROXIMITY_METERS);
    }
    
    /**
     * Switch between Northbound and Southbound
     */
    protected String switchDirection(String currentDirection) {
        if (currentDirection == null) return null;
        return "Southbound".equalsIgnoreCase(currentDirection) ? "Northbound" : "Southbound";
    }
    
    /**
     * Calculate distance between two coordinates using Haversine formula
     */
    protected double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }
    
    /**
     * Get the company name this strategy is designed for
     */
    public abstract String getCompanyName();
}
