package com.backend.onebus.service;

import com.backend.onebus.model.Bus;
import com.backend.onebus.model.BusLocation;
import com.backend.onebus.model.BusStop;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.backend.onebus.repository.BusRepository;
import com.backend.onebus.repository.BusLocationRepository;
import com.backend.onebus.repository.RouteRepository;
import com.backend.onebus.repository.RouteStopRepository;
import com.backend.onebus.model.Route;
import com.backend.onebus.model.RouteStop;
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

    private static final String BUS_LOCATION_KEY = "bus:location:";
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
        String redisKey = BUS_LOCATION_KEY + payload.getTrackerImei();
        BusLocation cachedLocation = (BusLocation) redisTemplate.opsForValue().get(redisKey);

        if (cachedLocation == null) {
            Bus bus = busRepository.findByTrackerImei(payload.getTrackerImei());
            if (bus == null) {
                logger.warn("No bus found for trackerImei: {}", payload.getTrackerImei());
                return;
            }
            payload.setBusId(bus.getBusId());
            payload.setBusNumber(bus.getBusNumber());
            payload.setBusDriverId(bus.getDriverId());
            payload.setBusDriver(bus.getDriverName());
            // Use the company name or the legacy busCompanyName field
            String companyName = (bus.getBusCompany() != null) ? 
                bus.getBusCompany().getName() : bus.getBusCompanyName();
            payload.setBusCompany(companyName);
        } else {
            payload.setBusId(cachedLocation.getBusId());
            payload.setBusNumber(cachedLocation.getBusNumber());
            payload.setBusDriverId(cachedLocation.getBusDriverId());
            payload.setBusDriver(cachedLocation.getBusDriver());
            payload.setBusCompany(cachedLocation.getBusCompany());
        }

        // --- New logic: update direction and busStopIndex based on proximity to stops ---
        String company = payload.getBusCompany();
        String busNumber = payload.getBusNumber();
        if (company != null && busNumber != null) {
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
        }
        // --- End new logic ---

        redisTemplate.opsForValue().set(redisKey, payload, 24, TimeUnit.HOURS);
        redisTemplate.opsForGeo().add(BUS_GEO_KEY, new RedisGeoCommands.GeoLocation<>(
                payload.getBusId(), new org.springframework.data.geo.Point(payload.getLon(), payload.getLat())));

        if (cachedLocation == null || (Instant.now().toEpochMilli() - cachedLocation.getLastSavedTimestamp() >= SAVE_INTERVAL_MS)) {
            busLocationRepository.save(payload);
            logger.info("Bus info updated: {}", payload);
            payload.setLastSavedTimestamp(Instant.now().toEpochMilli());
            redisTemplate.opsForValue().set(redisKey, payload, 24, TimeUnit.HOURS);
        }

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
                if (location != null && tripDirection.equals(location.getTripDirection())) {
                    return location;
                }
            }
        }
        return null;
    }

    public void updateBusDetails(Bus bus) {
        busRepository.save(bus);
        redisTemplate.delete(BUS_LOCATION_KEY + bus.getTrackerImei());
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
                    busNumber.equals(location.getBusNumber()) && 
                    direction.equals(location.getTripDirection())) {
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
}