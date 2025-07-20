package com.backend.onebus.service;

import com.backend.onebus.model.BusLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BusSelectionService {
    
    private static final Logger logger = LoggerFactory.getLogger(BusSelectionService.class);
    private static final String BUS_LOCATION_KEY = "bus:location:";
    private static final int MAX_INDEX_DIFFERENCE = 3; // Disregard buses with index > clientIndex + 3
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Select the best bus for a client based on their location and bus stop index
     * 
     * @param busNumber Route number (e.g., "C5")
     * @param direction Direction (e.g., "Northbound")
     * @param clientLat Client's latitude
     * @param clientLon Client's longitude
     * @param clientBusStopIndex Client's current bus stop index
     * @return The best bus ID for the client, or null if no suitable bus found
     */
    public String selectBestBusForClient(String busNumber, String direction, 
                                       double clientLat, double clientLon, int clientBusStopIndex) {
        
        logger.info("Selecting best bus for client at index {} on route {} {}", 
                   clientBusStopIndex, busNumber, direction);
        
        // Get all active buses for this route and direction
        List<BusLocation> activeBuses = getActiveBusesForRoute(busNumber, direction);
        
        if (activeBuses.isEmpty()) {
            logger.warn("No active buses found for route {} {}", busNumber, direction);
            return null;
        }
        
        // Filter buses based on index criteria
        List<BusLocation> suitableBuses = filterSuitableBuses(activeBuses, clientBusStopIndex);
        
        if (suitableBuses.isEmpty()) {
            logger.warn("No suitable buses found for client at index {} (all buses have index > {})", 
                       clientBusStopIndex, clientBusStopIndex + MAX_INDEX_DIFFERENCE);
            return null;
        }
        
        // Find the bus with the closest index to the client
        BusLocation bestBus = findClosestIndexBus(suitableBuses, clientBusStopIndex);
        
        if (bestBus != null) {
            logger.info("Selected bus {} (index: {}) for client at index {}", 
                       bestBus.getBusId(), bestBus.getBusStopIndex(), clientBusStopIndex);
            return bestBus.getBusId();
        }
        
        return null;
    }
    
    /**
     * Get all active buses for a specific route and direction
     */
    private List<BusLocation> getActiveBusesForRoute(String busNumber, String direction) {
        List<BusLocation> activeBuses = new ArrayList<>();
        
        try {
            Set<String> keys = redisTemplate.keys(BUS_LOCATION_KEY + "*");
            if (keys != null) {
                for (String key : keys) {
                    BusLocation location = (BusLocation) redisTemplate.opsForValue().get(key);
                    if (location != null && 
                        busNumber.equals(location.getBusNumber()) && 
                        direction.equals(location.getTripDirection()) &&
                        location.getBusStopIndex() != null) {
                        activeBuses.add(location);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting active buses for route {} {}: {}", busNumber, direction, e.getMessage());
        }
        
        logger.debug("Found {} active buses for route {} {}", activeBuses.size(), busNumber, direction);
        return activeBuses;
    }
    
    /**
     * Filter buses to only include those suitable for the client
     * - Disregard buses with index > clientIndex + MAX_INDEX_DIFFERENCE
     * - Only include buses that are ahead of or at the client's position
     */
    private List<BusLocation> filterSuitableBuses(List<BusLocation> activeBuses, int clientBusStopIndex) {
        return activeBuses.stream()
                .filter(bus -> {
                    Integer busIndex = bus.getBusStopIndex();
                    if (busIndex == null) return false;
                    
                    // Only consider buses that are at or ahead of the client
                    if (busIndex < clientBusStopIndex) return false;
                    
                    // Disregard buses with index > clientIndex + MAX_INDEX_DIFFERENCE
                    if (busIndex > clientBusStopIndex + MAX_INDEX_DIFFERENCE) return false;
                    
                    return true;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Find the bus with the closest index to the client's index
     * Prefer buses that are closest to the client's position
     */
    private BusLocation findClosestIndexBus(List<BusLocation> suitableBuses, int clientBusStopIndex) {
        if (suitableBuses.isEmpty()) return null;
        
        // Sort by index difference (closest first)
        suitableBuses.sort((bus1, bus2) -> {
            int diff1 = Math.abs(bus1.getBusStopIndex() - clientBusStopIndex);
            int diff2 = Math.abs(bus2.getBusStopIndex() - clientBusStopIndex);
            return Integer.compare(diff1, diff2);
        });
        
        BusLocation bestBus = suitableBuses.get(0);
        logger.debug("Closest bus: {} (index: {}, difference: {})", 
                    bestBus.getBusId(), bestBus.getBusStopIndex(), 
                    Math.abs(bestBus.getBusStopIndex() - clientBusStopIndex));
        
        return bestBus;
    }
    
    /**
     * Get all available buses for a route and direction with their indices
     * Useful for debugging and monitoring
     */
    public List<Map<String, Object>> getAvailableBusesForRoute(String busNumber, String direction) {
        List<BusLocation> activeBuses = getActiveBusesForRoute(busNumber, direction);
        
        return activeBuses.stream()
                .map(bus -> {
                    Map<String, Object> busInfo = new HashMap<>();
                    busInfo.put("busId", bus.getBusId());
                    busInfo.put("busStopIndex", bus.getBusStopIndex());
                    busInfo.put("latitude", bus.getLat());
                    busInfo.put("longitude", bus.getLon());
                    busInfo.put("lastUpdate", bus.getTimestamp());
                    return busInfo;
                })
                .collect(Collectors.toList());
    }
    
    /**
     * Check if a bus is still suitable for a client
     * Used for dynamic re-evaluation of bus selection
     */
    public boolean isBusStillSuitable(String busId, int clientBusStopIndex) {
        try {
            BusLocation busLocation = (BusLocation) redisTemplate.opsForValue().get(BUS_LOCATION_KEY + busId);
            if (busLocation == null || busLocation.getBusStopIndex() == null) {
                return false;
            }
            
            int busIndex = busLocation.getBusStopIndex();
            
            // Check if bus is still within acceptable range
            return busIndex >= clientBusStopIndex && 
                   busIndex <= clientBusStopIndex + MAX_INDEX_DIFFERENCE;
            
        } catch (Exception e) {
            logger.error("Error checking if bus {} is still suitable: {}", busId, e.getMessage());
            return false;
        }
    }
    
    /**
     * Find a better bus for a client if their current bus is no longer suitable
     */
    public String findBetterBusForClient(String busNumber, String direction, 
                                       double clientLat, double clientLon, int clientBusStopIndex, 
                                       String currentBusId) {
        
        // Check if current bus is still suitable
        if (isBusStillSuitable(currentBusId, clientBusStopIndex)) {
            return currentBusId; // Current bus is still good
        }
        
        logger.info("Current bus {} is no longer suitable for client at index {}, finding better bus", 
                   currentBusId, clientBusStopIndex);
        
        // Find a new bus
        return selectBestBusForClient(busNumber, direction, clientLat, clientLon, clientBusStopIndex);
    }
} 