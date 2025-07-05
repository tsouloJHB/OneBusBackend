package com.backend.onebus.service;

import com.backend.onebus.model.Bus;
import com.backend.onebus.model.BusLocation;
import com.backend.onebus.repository.BusRepository;
import com.backend.onebus.repository.BusLocationRepository;
import org.locationtech.jts.geom.GeometryFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

import java.time.Instant;
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

    private static final String BUS_LOCATION_KEY = "bus:location:";
    private static final String BUS_GEO_KEY = "bus:geo";
    private static final long SAVE_INTERVAL_MS = 30 * 60 * 1000; // 30 minutes

    private static final Logger logger = LoggerFactory.getLogger(BusTrackingService.class);

    public void processTrackerPayload(BusLocation payload) {
        String redisKey = BUS_LOCATION_KEY + payload.getTrackerImei();
        BusLocation cachedLocation = (BusLocation) redisTemplate.opsForValue().get(redisKey);

        if (cachedLocation == null) {
            Bus bus = busRepository.findByTrackerImei(payload.getTrackerImei());
            if (bus == null) {
                logger.warn("No bus found for trackerImei: {}", payload.getTrackerImei());
                return;
            }
            // Set bus details from database
            payload.setBusId(bus.getBusId());
            payload.setBusNumber(bus.getBusNumber());
            payload.setBusDriverId(bus.getDriverId());
            payload.setBusDriver(bus.getDriverName());
            payload.setBusCompany(bus.getBusCompany());
        } else {
            payload.setBusId(cachedLocation.getBusId());
            payload.setBusNumber(cachedLocation.getBusNumber());
            payload.setBusDriverId(cachedLocation.getBusDriverId());
            payload.setBusDriver(cachedLocation.getBusDriver());
            payload.setBusCompany(cachedLocation.getBusCompany());
        }

        // Update Redis
        redisTemplate.opsForValue().set(redisKey, payload, 24, TimeUnit.HOURS);
        redisTemplate.opsForGeo().add(BUS_GEO_KEY, new RedisGeoCommands.GeoLocation<>(
                payload.getBusId(), new org.springframework.data.geo.Point(payload.getLon(), payload.getLat())));

        // Check if 30 minutes have passed since last save
        if (cachedLocation == null || (Instant.now().toEpochMilli() - cachedLocation.getLastSavedTimestamp() >= SAVE_INTERVAL_MS)) {
            // Temporarily remove geometry creation to avoid PostGIS issues
            // org.locationtech.jts.geom.Point point = geometryFactory.createPoint(new org.locationtech.jts.geom.Coordinate(payload.getLon(), payload.getLat()));
            // point.setSRID(4326); // Set SRID to WGS84
            // payload.setLocation(point);
            busLocationRepository.save(payload);
            logger.info("Bus info updated: {}", payload);
            payload.setLastSavedTimestamp(Instant.now().toEpochMilli());
            redisTemplate.opsForValue().set(redisKey, payload, 24, TimeUnit.HOURS);
        }

        // Stream to subscribed WebSocket clients
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