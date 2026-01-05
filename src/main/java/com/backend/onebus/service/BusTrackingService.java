package com.backend.onebus.service;

import com.backend.onebus.model.Bus;
import com.backend.onebus.model.BusLocation;
import com.backend.onebus.model.BusStop;
import com.backend.onebus.model.Route;
import com.backend.onebus.model.RouteStop;
import com.backend.onebus.repository.BusLocationRepository;
import com.backend.onebus.repository.BusRepository;
import com.backend.onebus.repository.RouteRepository;
import com.backend.onebus.repository.RouteStopRepository;
import com.backend.onebus.service.routing.BusCompanyRoutingStrategy;
import com.backend.onebus.service.RuleEngineService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.geo.Circle;
import org.springframework.data.geo.GeoResult;
import org.springframework.data.geo.GeoResults;
import org.springframework.data.geo.Distance;
import org.springframework.data.geo.Metrics;
import org.springframework.data.redis.connection.RedisGeoCommands;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class BusTrackingService {
    @Autowired
    private BusRepository busRepository;
    @Autowired
    private BusLocationRepository busLocationRepository;
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    @Autowired
    private GeometryFactory geometryFactory;
    @Autowired
    private BusStreamingService streamingService;
    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private RouteStopRepository routeStopRepository;

    @Autowired
    private com.backend.onebus.service.routing.BusCompanyStrategyFactory strategyFactory;

    @Autowired
    private BusSelectionService busSelectionService;
    @Autowired
    private RuleEngineService ruleEngineService;
    private static final String BUS_LOCATION_KEY = "bus:location:";
    private static final String ACTIVE_BUS_KEY_PREFIX = "active:bus:";
    private static final String BUS_GEO_KEY = "bus:geo";
    private static final long SAVE_INTERVAL_MS = 30 * 60 * 1000; // 30 minutes

    // In-memory cache: company_busNumber -> list of stops
    private static final Map<String, List<BusStop>> routeStopsCache = new HashMap<>();
    private static final double STOP_PROXIMITY_METERS = 30.0;
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private static final Logger logger = LoggerFactory.getLogger(BusTrackingService.class);

    private void loadRouteStopsIfNeeded(String company, String busNumber) {
        String cacheKey = company + "_" + busNumber;
        if (routeStopsCache.containsKey(cacheKey)) return;
        
        try {
            // Find route in database
            Route route = routeRepository.findByCompanyAndBusNumber(company, busNumber)
                    .orElse(null);
            
            if (route == null) {
                logger.warn("No route found for company: {} and busNumber: {}", company, busNumber);
                return;
            }
            
            // Load stops for this route
            List<RouteStop> routeStops = routeStopRepository.findByRouteIdOrderByBusStopIndex(route.getId());
            List<BusStop> stops = new ArrayList<>();
            
            for (RouteStop routeStop : routeStops) {
                BusStop stop = new BusStop();
                stop.setLatitude(routeStop.getLatitude());
                stop.setLongitude(routeStop.getLongitude());
                stop.setAddress(routeStop.getAddress());
                stop.setBusStopIndex(routeStop.getBusStopIndex());
                stop.setDirection(routeStop.getDirection());
                stop.setType(routeStop.getType());
                stop.setNorthboundIndex(routeStop.getNorthboundIndex());
                stop.setSouthboundIndex(routeStop.getSouthboundIndex());
                stops.add(stop);
            }
            
            routeStopsCache.put(cacheKey, stops);
            logger.info("Loaded {} stops for route {} (company: {}, busNumber: {})", 
                       stops.size(), route.getRouteName(), company, busNumber);
            
        } catch (Exception e) {
            logger.error("Failed to load route stops for company: {} and busNumber: {}: {}", 
                        company, busNumber, e.getMessage());
        }
    }

    private BusStop findNearbyStop(String company, String busNumber, double lat, double lon) {
        loadRouteStopsIfNeeded(company, busNumber);
        String cacheKey = company + "_" + busNumber;
        List<BusStop> stops = routeStopsCache.get(cacheKey);
        if (stops == null) return null;
        for (BusStop stop : stops) {
            if (distanceMeters(lat, lon, stop.getLatitude(), stop.getLongitude()) <= STOP_PROXIMITY_METERS) {
                return stop;
            }
        }
        return null;
    }

    // Haversine formula for distance in meters
    private double distanceMeters(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371000; // meters
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat/2) * Math.sin(dLat/2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                Math.sin(dLon/2) * Math.sin(dLon/2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1-a));
        return R * c;
    }

    public void processTrackerPayload(BusLocation payload) {
        // Rule 1: Validate IMEI is registered
        String redisKey = BUS_LOCATION_KEY + payload.getTrackerImei();
        BusLocation cachedLocation = (BusLocation) redisTemplate.opsForValue().get(redisKey);

        Bus bus = null;
        Long companyId = null;
        if (cachedLocation == null) {
            bus = busRepository.findByTrackerImei(payload.getTrackerImei());
            if (bus == null) {
                logger.warn("[REJECTED] Unregistered trackerImei: {} - Ignoring foreign device", payload.getTrackerImei());
                return;
            }
            
            // Rule 2 & 3: Check operational status before processing
            if (!"active".equalsIgnoreCase(bus.getOperationalStatus())) {
                logger.info("[IGNORED] Bus {} (IMEI: {}) has status '{}' - Not saving coordinates", 
                    bus.getBusNumber(), payload.getTrackerImei(), bus.getOperationalStatus());
                return;
            }
            payload.setBusId(bus.getBusId());
            payload.setBusNumber(bus.getBusNumber());
            payload.setBusDriverId(bus.getDriverId());
            payload.setBusDriver(bus.getDriverName());
            // Use the company name or the legacy busCompanyName field
            String companyName = (bus.getBusCompany() != null) ? 
                bus.getBusCompany().getName() : bus.getBusCompanyName();
            
            // CRITICAL: Initialize tripDirection from route default for first GPS
            // Fetch route immediately to get direction, even if company is unknown
            Route initialRoute = null;
            if (payload.getBusNumber() != null) {
                initialRoute = routeRepository.findByBusNumber(payload.getBusNumber()).stream().findFirst().orElse(null);
                if (initialRoute != null) {
                    // Set direction from route regardless of whether we found company
                    if (initialRoute.getDirection() != null) {
                        payload.setTripDirection(initialRoute.getDirection());
                        logger.info("[INIT] Set tripDirection='{}' for bus {} from route", 
                            initialRoute.getDirection(), bus.getBusId());
                    }
                    // Also try to resolve company from route if not found
                    if (companyName == null && initialRoute.getCompany() != null) {
                        companyName = initialRoute.getCompany();
                        logger.info("[INIT] Resolved company='{}' for bus {} from route", companyName, bus.getBusId());
                    }
                } else {
                    logger.warn("[INIT] No route found for bus {}", bus.getBusNumber());
                }
            }
            
            if (bus.getBusCompany() != null) {
                companyId = bus.getBusCompany().getId();
            }
            payload.setBusCompany(companyName);
        } else {
            // If using cached location, we still need to verify the bus is active
            // Re-fetch bus to check current operational status
            bus = busRepository.findByTrackerImei(payload.getTrackerImei());
            if (bus != null && !"active".equalsIgnoreCase(bus.getOperationalStatus())) {
                logger.info("[IGNORED] Cached bus {} has status '{}' - Not saving coordinates", 
                    cachedLocation.getBusNumber(), bus.getOperationalStatus());
                return;
            }
            
            payload.setBusId(cachedLocation.getBusId());
            payload.setBusNumber(cachedLocation.getBusNumber());
            payload.setBusDriverId(cachedLocation.getBusDriverId());
            payload.setBusDriver(cachedLocation.getBusDriver());
            payload.setBusCompany(cachedLocation.getBusCompany());
            payload.setTripDirection(cachedLocation.getTripDirection());
            
            // CRITICAL FIX: If cached tripDirection is null, initialize from route
            if (payload.getTripDirection() == null && payload.getBusNumber() != null) {
                Route route = routeRepository.findByBusNumber(payload.getBusNumber()).stream().findFirst().orElse(null);
                if (route != null && route.getDirection() != null) {
                    payload.setTripDirection(route.getDirection());
                    logger.info("[CACHED-INIT] Set tripDirection='{}' for cached bus {} from route", 
                        route.getDirection(), payload.getBusId());
                }
            }
            
            if (bus != null && bus.getBusCompany() != null) {
                companyId = bus.getBusCompany().getId();
            }
        }

        // --- New logic: update direction and busStopIndex based on proximity to stops ---
        String company = payload.getBusCompany();
        String busNumber = payload.getBusNumber();
        if (company != null && busNumber != null) {
            try {
                if (companyId == null) {
                    companyId = ruleEngineService.resolveCompanyIdByName(company).orElse(null);
                }
                // Apply company-specific routing strategy
                com.backend.onebus.service.routing.BusCompanyRoutingStrategy strategy = 
                    strategyFactory.getStrategy(company);
                
                // Get the route for this bus
                Route route = routeRepository.findByCompanyAndBusNumber(company, busNumber).orElse(null);
                
                // Retrieve previous location from Redis to use as historical context
                BusLocation previousLocation = cachedLocation;
                
                // Step 1: Apply global rules (first GPS, end-of-route)
                String globalDirection = strategy.applyGlobalRules(payload, previousLocation, route, companyId, ruleEngineService);
                if (globalDirection != null) {
                    payload.setTripDirection(globalDirection);
                    logger.info("[Strategy] Global rule applied: Bus {} direction set to {}", 
                        busNumber, globalDirection);
                }
                
                // Step 2: Apply company-specific direction inference
                String inferredDirection = strategy.inferDirection(payload, previousLocation, route);
                if (inferredDirection != null) {
                    payload.setTripDirection(inferredDirection);
                    logger.info("[Strategy] Company inference applied: Bus {} direction set to {}", 
                        busNumber, inferredDirection);
                }
                
                // Step 3: Update busStopIndex based on proximity to stops
                BusStop nearbyStop = findNearbyStop(company, busNumber, payload.getLat(), payload.getLon());
                if (nearbyStop != null) {
                    // Only update direction if not bidirectional
                    if (!"bidirectional".equalsIgnoreCase(nearbyStop.getDirection())) {
                        payload.setTripDirection(nearbyStop.getDirection());
                    }
                    // For bidirectional, use the correct index if available
                    if ("bidirectional".equalsIgnoreCase(nearbyStop.getDirection())) {
                        if (payload.getTripDirection() != null && payload.getTripDirection().equalsIgnoreCase("Northbound") && nearbyStop.getNorthboundIndex() != null) {
                            payload.setBusStopIndex(nearbyStop.getNorthboundIndex());
                        } else if (payload.getTripDirection() != null && payload.getTripDirection().equalsIgnoreCase("Southbound") && nearbyStop.getSouthboundIndex() != null) {
                            payload.setBusStopIndex(nearbyStop.getSouthboundIndex());
                        }
                    } else {
                        payload.setBusStopIndex(nearbyStop.getBusStopIndex());
                    }
                }
                
                // Step 4: Final fallback - if still no direction (first GPS, no nearby stop), use route default
                if (payload.getTripDirection() == null && cachedLocation == null && route != null) {
                    payload.setTripDirection(route.getDirection());
                    logger.info("[Fallback] Bus {} first GPS with no direction determined - using route default: {}", 
                        busNumber, route.getDirection());
                }
                
            } catch (Exception e) {
                logger.error("[Strategy] Error applying routing strategy for bus {}: {}", 
                    busNumber, e.getMessage(), e);
                // Continue with existing logic if strategy fails
                BusStop nearbyStop = findNearbyStop(company, busNumber, payload.getLat(), payload.getLon());
                if (nearbyStop != null) {
                    if (!"bidirectional".equalsIgnoreCase(nearbyStop.getDirection())) {
                        payload.setTripDirection(nearbyStop.getDirection());
                    }
                    if ("bidirectional".equalsIgnoreCase(nearbyStop.getDirection())) {
                        if (payload.getTripDirection() != null && payload.getTripDirection().equalsIgnoreCase("Northbound") && nearbyStop.getNorthboundIndex() != null) {
                            payload.setBusStopIndex(nearbyStop.getNorthboundIndex());
                        } else if (payload.getTripDirection() != null && payload.getTripDirection().equalsIgnoreCase("Southbound") && nearbyStop.getSouthboundIndex() != null) {
                            payload.setBusStopIndex(nearbyStop.getSouthboundIndex());
                        }
                    } else {
                        payload.setBusStopIndex(nearbyStop.getBusStopIndex());
                    }
                }
            }
        }
        // --- End new logic ---

        redisTemplate.opsForValue().set(redisKey, payload, 24, TimeUnit.HOURS);
        redisTemplate.opsForGeo().add(BUS_GEO_KEY, new RedisGeoCommands.GeoLocation<>(
            payload.getBusId(), new org.springframework.data.geo.Point(payload.getLon(), payload.getLat())));

        // Always stamp the payload before saving so DB rows have a fresh lastSavedTimestamp
        payload.setLastSavedTimestamp(Instant.now().toEpochMilli());

        // Persist every payload so /buses/active can read recent positions from the database
        busLocationRepository.save(payload);
        logger.info("Bus info updated: {}", payload);
        redisTemplate.opsForValue().set(redisKey, payload, 24, TimeUnit.HOURS);

        streamingService.broadcastBusUpdate(payload);
    }

    public BusLocation findNearestBus(double lat, double lon, String tripDirection) {
        GeoResults<RedisGeoCommands.GeoLocation<Object>> results = redisTemplate.opsForGeo()
                .radius(BUS_GEO_KEY, new Circle(new org.springframework.data.geo.Point(lon, lat),
                        new Distance(10, Metrics.KILOMETERS)));
        if (results != null) {
            for (GeoResult<RedisGeoCommands.GeoLocation<Object>> result : results) {
                String busId = (String) result.getContent().getName();
                BusLocation location = (BusLocation) redisTemplate.opsForValue().get(BUS_LOCATION_KEY + busId);
                if (location != null && tripDirection.equalsIgnoreCase(location.getTripDirection())) {
                    return location;
                }
            }
        }
        return null;
    }

    /**
     * Find a replacement bus using existing selection rules.
     * Treat the offline bus's last known position/index as the "client" position.
     */
    private BusLocation findReplacementBus(BusLocation offline) {
        if (offline == null || offline.getBusNumber() == null || offline.getTripDirection() == null) return null;

        int clientIndex = offline.getBusStopIndex() != null ? offline.getBusStopIndex() : 0;
        String replacementBusId = busSelectionService.selectBestBusForClient(
                offline.getBusNumber(),
                offline.getTripDirection(),
                offline.getLat(),
                offline.getLon(),
                clientIndex
        );

        if (replacementBusId == null || replacementBusId.equals(offline.getBusId())) {
            return null;
        }

        return getBusLocationById(replacementBusId);
    }

    /**
     * Locate a bus in Redis by its busId (keys are stored by tracker IMEI).
     */
    private BusLocation getBusLocationById(String busId) {
        try {
            Set<String> keys = redisTemplate.keys(BUS_LOCATION_KEY + "*");
            if (keys == null) return null;
            for (String key : keys) {
                BusLocation loc = (BusLocation) redisTemplate.opsForValue().get(key);
                if (loc != null && busId.equals(loc.getBusId())) {
                    return loc;
                }
            }
        } catch (Exception e) {
            logger.error("Error finding bus location by id {}: {}", busId, e.getMessage());
        }
        return null;
    }

    public void updateBusDetails(Bus bus) {
        busRepository.save(bus);
        redisTemplate.delete(BUS_LOCATION_KEY + bus.getTrackerImei());
    }
    
    /**
     * Update the operational status of a bus.
     * This controls whether the bus's GPS data will be processed and broadcast.
     * 
     * @param busId The ID of the bus
     * @param newStatus The new operational status (active, inactive, maintenance, retired)
     * @return true if bus was found and updated, false if bus not found
     */
    public boolean updateBusStatus(String busId, String newStatus) {
        Bus bus = busRepository.findById(busId).orElse(null);
        if (bus == null) {
            logger.warn("Cannot update status - bus not found: {}", busId);
            return false;
        }
        
        String oldStatus = bus.getOperationalStatus();
        bus.setOperationalStatus(newStatus);
        busRepository.save(bus);
        
        logger.info("Bus {} status changed: {} → {}", busId, oldStatus, newStatus);
        
        // If changing to inactive, clear cache, broadcast offline, and optionally swap in a replacement bus
        if ("inactive".equalsIgnoreCase(newStatus) || 
            "maintenance".equalsIgnoreCase(newStatus) || 
            "retired".equalsIgnoreCase(newStatus)) {
            String redisKey = BUS_LOCATION_KEY + bus.getTrackerImei();
            BusLocation offlineLocation = (BusLocation) redisTemplate.opsForValue().get(redisKey);

            redisTemplate.delete(redisKey);
            logger.info("Cleared Redis cache for inactive bus: {}", bus.getTrackerImei());

            if (offlineLocation != null) {
                // Let clients drop the inactive bus immediately
                streamingService.broadcastBusOffline(offlineLocation);

                // Find a replacement bus using existing selection rules
                BusLocation replacement = findReplacementBus(offlineLocation);
                if (replacement != null) {
                    logger.info("Broadcasting replacement bus {} for offline bus {}", replacement.getBusId(), offlineLocation.getBusId());
                    streamingService.broadcastBusUpdate(replacement);
                } else {
                    logger.info("No replacement bus available for offline bus {} on route {} {}", 
                            offlineLocation.getBusId(), offlineLocation.getBusNumber(), offlineLocation.getTripDirection());
                }
            }
        }
        
        return true;
    }

    public Bus saveBus(Bus bus) {
        return busRepository.save(bus);
    }

    @Scheduled(cron = "0 0 0 * * ?", zone = "Africa/Johannesburg")
    public void clearRedisData() {
        Set<String> keys = redisTemplate.keys(BUS_LOCATION_KEY + "*");
        if (keys != null) {
            redisTemplate.delete(keys);
        }
        redisTemplate.delete(BUS_GEO_KEY);
    }

    public void clearTrackingData() {
        clearRedisData();
    }

    /**
     * Get current location for a specific bus and direction
     */
    public BusLocation getBusLocation(String busNumber, String direction) {
        Set<String> keys = redisTemplate.keys(BUS_LOCATION_KEY + "*");
        if (keys != null) {
            for (String key : keys) {
                BusLocation location = (BusLocation) redisTemplate.opsForValue().get(key);
                if (location != null && 
                    busNumber.equalsIgnoreCase(location.getBusNumber()) && 
                    direction.equalsIgnoreCase(location.getTripDirection())) {
                    return location;
                }
            }
        }
        return null;
    }

    /**
     * Get all active bus numbers
     */
    public Set<String> getActiveBuses() {
        Set<String> activeBuses = new java.util.HashSet<>();
        Set<String> keys = redisTemplate.keys(BUS_LOCATION_KEY + "*");
        if (keys != null) {
            for (String key : keys) {
                BusLocation location = (BusLocation) redisTemplate.opsForValue().get(key);
                if (location != null && location.getBusNumber() != null) {
                    activeBuses.add(location.getBusNumber());
                }
            }
        }
        return activeBuses;
    }

    /**
     * Clean up orphaned buses that have registration numbers but no registered_buses entry
     */
    public int cleanupOrphanedBuses() {
        List<Bus> allBuses = busRepository.findAll();
        int deleted = 0;
        
        for (Bus bus : allBuses) {
            // Only delete buses that have a registration number (meaning they were synced from registered_buses)
            // but keep simulator buses (those without registration numbers)
            if (bus.getRegistrationNumber() != null && !bus.getRegistrationNumber().isEmpty()) {
                busRepository.delete(bus);
                deleted++;
                logger.info("Deleted orphaned bus: {} (regNum: {})", bus.getBusId(), bus.getRegistrationNumber());
            }
        }
        
        logger.info("Cleanup complete: deleted {} orphaned buses", deleted);
        return deleted;
    }

    /**
     * Get count of active buses from Redis (in-memory cache)
     * Active buses are those that have sent location updates recently
     */
    public long getActiveBusesCount() {
        Set<String> activeBusIds = redisTemplate.keys(ACTIVE_BUS_KEY_PREFIX + "*");
        return activeBusIds != null ? activeBusIds.size() : 0;
    }
}