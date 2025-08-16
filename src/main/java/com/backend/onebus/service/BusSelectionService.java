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
     * Select the best bus for a client based on their location and bus stop index, following the updated rules:
     * 1. Prefer buses in the requested direction, ahead of or at the client's stop.
     * 2. If none, select the closest bus in the opposite direction.
     * 3. If only one bus exists and it's in the opposite direction, return it.
     * 4. If no buses at all, return null.
     * 5. Never suggest a bus with a negative index.
     */
    public String selectBestBusForClient(String busNumber, String direction, 
                                       double clientLat, double clientLon, int clientBusStopIndex) {
        logger.info("Selecting best bus for client at index {} on route {} {}", 
                   clientBusStopIndex, busNumber, direction);

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
            logger.info("Selected bus {} (index: {}) in requested direction {} for client at index {}", 
                best.getBusId(), best.getBusStopIndex(), direction, clientBusStopIndex);
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
                logger.info("Selected bus {} (index: {}) in opposite direction {} for client at index {}", 
                    bestOpp.getBusId(), bestOpp.getBusStopIndex(), oppositeDirection, clientBusStopIndex);
                return bestOpp.getBusId();
            }
        }

        // 6. No buses available in either direction
        logger.warn("No buses available for route {} in either direction.", busNumber);
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
                        busNumber.equalsIgnoreCase(location.getBusNumber()) && 
                        direction.equalsIgnoreCase(location.getTripDirection()) &&
                        location.getBusStopIndex() != null) {
                        activeBuses.add(location);
                    }
                }
            }
        } catch (Exception e) {
            logger.error("Error getting active buses for route {} {}: {}", busNumber, direction, e.getMessage());
        }
        
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
                    busInfo.put("busNumber", bus.getBusNumber());
                    busInfo.put("direction", bus.getTripDirection());
                    busInfo.put("trackerImei", bus.getTrackerImei());
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