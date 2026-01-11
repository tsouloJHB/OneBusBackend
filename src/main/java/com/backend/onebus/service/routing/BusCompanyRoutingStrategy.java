package com.backend.onebus.service.routing;

import com.backend.onebus.model.BusLocation;
import com.backend.onebus.model.Route;
import com.backend.onebus.model.RouteStop;
import com.backend.onebus.repository.RouteStopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;

import java.util.List;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

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
    private static final String BUS_LOCATION_KEY = "bus:location:";
    
    protected RouteStopRepository routeStopRepository;
    protected RedisTemplate<String, Object> redisTemplate;
    
    // Intensively cache route stops locally within the strategy to ensure sub-5ms performance
    private static final java.util.Map<String, List<RouteStop>> localRouteStopsCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    public void setRouteStopRepository(RouteStopRepository routeStopRepository) {
        this.routeStopRepository = routeStopRepository;
    }
    
    public void setRedisTemplate(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
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
        if (route == null) return 0;
        List<RouteStop> stops = getRouteStopsCached(route.getId(), direction);
        return stops.size();
    }
    
    private List<RouteStop> getRouteStopsCached(Long routeId, String direction) {
        String cacheKey = routeId + "_" + direction;
        return localRouteStopsCache.computeIfAbsent(cacheKey, k -> {
            if (routeStopRepository == null) {
                logger.warn("RouteStopRepository is null in strategy - cannot load stops");
                return new java.util.ArrayList<>();
            }
            try {
                long start = System.currentTimeMillis();
                List<RouteStop> stops = routeStopRepository.findByRouteIdAndDirectionOrderByBusStopIndex(routeId, direction);
                logger.info("[STRATEGY-CACHE] Loaded {} stops for route {} {} from DB in {}ms", 
                    stops.size(), routeId, direction, (System.currentTimeMillis() - start));
                return stops;
            } catch (Exception e) {
                logger.error("Error loading stops for strategy cache: {}", e.getMessage());
                return new java.util.ArrayList<>();
            }
        });
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
        if (route == null) return null;
        List<RouteStop> stops = getRouteStopsCached(route.getId(), direction);
        return stops.isEmpty() ? null : stops.get(0);
    }
    
    /**
     * Find the last stop of a route in a given direction
     */
    protected RouteStop getLastStop(Route route, String direction) {
        if (route == null) return null;
        List<RouteStop> stops = getRouteStopsCached(route.getId(), direction);
        return stops.isEmpty() ? null : stops.get(stops.size() - 1);
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
    
    // ============ Smart Bus Selection Methods (Company-specific) ============
    
    /**
     * Select the best bus for a client based on company-specific logic.
     * Default implementation provides basic fallback logic.
     * Companies can override this for their specific selection strategies.
     * 
     * @param busNumber The bus route number
     * @param direction The requested direction
     * @param clientLat Client latitude
     * @param clientLon Client longitude
     * @param clientBusStopIndex Client's bus stop index
     * @return The selected bus ID, or null if no suitable bus found
     */
    public String selectBestBusForClient(String busNumber, String direction, 
                                       double clientLat, double clientLon, int clientBusStopIndex) {
        // Default implementation - companies can override for specific logic
        logger.info("[{}] Using default bus selection for client at index {} on route {} {}", 
                   getCompanyName(), clientBusStopIndex, busNumber, direction);
        
        return selectBestBusDefault(busNumber, direction, clientLat, clientLon, clientBusStopIndex);
    }
    
    /**
     * Check if this company supports smart bus selection (Shadow Bus strategy).
     * Companies that don't support it will use traditional subscription only.
     * 
     * @return true if company supports smart bus selection, false otherwise
     */
    public boolean supportsSmartBusSelection() {
        return false; // Default: no smart selection
    }
    
    /**
     * Default bus selection logic that can be used by any company.
     * This implements the basic Shadow Bus strategy.
     */
    protected String selectBestBusDefault(String busNumber, String direction, 
                                        double clientLat, double clientLon, int clientBusStopIndex) {
        // 1. Get all active buses for this route in the requested direction
        List<BusLocation> busesRequestedDir = getActiveBusesForRoute(busNumber, direction);
        // 2. Get all active buses for this route in the opposite direction
        String oppositeDirection = direction.equalsIgnoreCase("Northbound") ? "Southbound" : "Northbound";
        List<BusLocation> busesOppositeDir = getActiveBusesForRoute(busNumber, oppositeDirection);

        // 3. Filter buses in requested direction: at or ahead of client
        List<BusLocation> suitableRequested = busesRequestedDir.stream()
            .filter(bus -> bus.getBusStopIndex() != null && bus.getBusStopIndex() >= clientBusStopIndex)
            .collect(Collectors.toList());

        if (!suitableRequested.isEmpty()) {
            // 4. Return the closest bus in requested direction
            BusLocation best = findClosestIndexBus(suitableRequested, clientBusStopIndex);
            logger.info("[{}] Selected bus {} (index: {}) in requested direction {} for client at index {}", 
                getCompanyName(), best.getBusId(), best.getBusStopIndex(), direction, clientBusStopIndex);
            return best.getBusId();
        }

        // 5. If no suitable bus in requested direction, try opposite direction
        if (!busesOppositeDir.isEmpty()) {
            // Never suggest a bus with a negative index
            List<BusLocation> suitableOpposite = busesOppositeDir.stream()
                .filter(bus -> bus.getBusStopIndex() != null && bus.getBusStopIndex() >= 0)
                .collect(Collectors.toList());
            if (!suitableOpposite.isEmpty()) {
                BusLocation bestOpp = findClosestIndexBus(suitableOpposite, clientBusStopIndex);
                logger.info("[{}] Selected bus {} (index: {}) in opposite direction {} for client at index {}", 
                    getCompanyName(), bestOpp.getBusId(), bestOpp.getBusStopIndex(), oppositeDirection, clientBusStopIndex);
                return bestOpp.getBusId();
            }
        }

        // 6. No buses available in either direction
        logger.warn("[{}] No buses available for route {} in either direction.", getCompanyName(), busNumber);
        return null;
    }
    
    /**
     * Get all active buses for a specific route and direction
     */
    protected List<BusLocation> getActiveBusesForRoute(String busNumber, String direction) {
        List<BusLocation> activeBuses = new ArrayList<>();
        
        if (redisTemplate == null) {
            logger.warn("[{}] RedisTemplate not available for bus selection", getCompanyName());
            return activeBuses;
        }
        
        try {
            Set<String> keys = redisTemplate.keys(BUS_LOCATION_KEY + "*");
            
            if (keys != null) {
                for (String key : keys) {
                    BusLocation location = (BusLocation) redisTemplate.opsForValue().get(key);
                    
                    if (location != null && 
                        busNumber.equalsIgnoreCase(location.getBusNumber()) && 
                        direction.equalsIgnoreCase(location.getTripDirection()) &&
                        location.getBusStopIndex() != null) {
                        activeBuses.add(location);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("[{}] Error getting active buses for route {} {}: {}", 
                        getCompanyName(), busNumber, direction, e.getMessage());
        }
        
        return activeBuses;
    }
    
    /**
     * Find the bus with the closest index to the client's index
     */
    protected BusLocation findClosestIndexBus(List<BusLocation> suitableBuses, int clientBusStopIndex) {
        if (suitableBuses.isEmpty()) return null;
        
        // Sort by index difference (closest first)
        suitableBuses.sort((bus1, bus2) -> {
            int diff1 = Math.abs(bus1.getBusStopIndex() - clientBusStopIndex);
            int diff2 = Math.abs(bus2.getBusStopIndex() - clientBusStopIndex);
            return Integer.compare(diff1, diff2);
        });
        
        BusLocation bestBus = suitableBuses.get(0);
        logger.debug("[{}] Closest bus: {} (index: {}, difference: {})", 
                    getCompanyName(), bestBus.getBusId(), bestBus.getBusStopIndex(), 
                    Math.abs(bestBus.getBusStopIndex() - clientBusStopIndex));
        
        return bestBus;
    }
}
