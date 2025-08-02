package com.backend.onebus.service;

import com.backend.onebus.model.BusLocation;
import com.backend.onebus.model.Bus;
import com.backend.onebus.model.BusStop;
import com.backend.onebus.repository.BusRepository;
import com.backend.onebus.repository.BusLocationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.GeoOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BusTrackingServiceTest {
    @Mock
    private BusRepository busRepository;
    @Mock
    private BusLocationRepository busLocationRepository;
    @Mock
    private RedisTemplate<String, Object> redisTemplate;
    @Mock
    private SimpMessagingTemplate messagingTemplate;
    @Mock
    private BusStreamingService streamingService;

    @InjectMocks
    private BusTrackingService busTrackingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessTrackerPayloadUpdatesDirectionAndStopIndex() throws Exception {
        // Arrange: mock bus and location
        Bus bus = new Bus();
        bus.setBusId("bus-1");
        bus.setBusNumber("C5");
        bus.setTrackerImei("imei-123");
        bus.setDriverId("driver-1");
        bus.setDriverName("John Doe");
        bus.setBusCompanyName("CompanyX");
        when(busRepository.findByTrackerImei("imei-123")).thenReturn(bus);

        // Mock ValueOperations for Redis
        ValueOperations<String, Object> mockValueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockValueOps);
        when(mockValueOps.get(anyString())).thenReturn(null);

        // Mock GeoOperations for Redis
        GeoOperations<String, Object> mockGeoOps = mock(GeoOperations.class);
        when(redisTemplate.opsForGeo()).thenReturn(mockGeoOps);

        // Pre-populate the routeStopsCache with a C5 stop
        BusStop stop = new BusStop();
        stop.setLatitude(-26.20282);
        stop.setLongitude(28.04011);
        stop.setBusStopIndex(2);
        stop.setDirection("Southbound");
        Field cacheField = BusTrackingService.class.getDeclaredField("routeStopsCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, java.util.List<BusStop>> cache = (java.util.Map<String, java.util.List<BusStop>>) cacheField.get(null);
        cache.put("C5", java.util.Collections.singletonList(stop));

        // Location near a known C5 Southbound stop (e.g., Harrison Street Bus Station)
        BusLocation payload = new BusLocation();
        payload.setTrackerImei("imei-123");
        payload.setLat(-26.20282);
        payload.setLon(28.04011);
        payload.setBusNumber("C5");

        // Act
        busTrackingService.processTrackerPayload(payload);

        // Assert: direction and stop index should be set
        assertEquals("Southbound", payload.getTripDirection());
        assertEquals(2, payload.getBusStopIndex());
    }

    @Test
    void testProcessTrackerPayloadUpdatesDirectionAndStopIndex_Northbound() throws Exception {
        Bus bus = new Bus();
        bus.setBusId("bus-2");
        bus.setBusNumber("C5");
        bus.setTrackerImei("imei-456");
        bus.setDriverId("driver-2");
        bus.setDriverName("Jane Doe");
        bus.setBusCompanyName("CompanyY");
        when(busRepository.findByTrackerImei("imei-456")).thenReturn(bus);

        ValueOperations<String, Object> mockValueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockValueOps);
        when(mockValueOps.get(anyString())).thenReturn(null);
        GeoOperations<String, Object> mockGeoOps = mock(GeoOperations.class);
        when(redisTemplate.opsForGeo()).thenReturn(mockGeoOps);

        BusStop stop = new BusStop();
        stop.setLatitude(-26.173968);
        stop.setLongitude(27.957238);
        stop.setBusStopIndex(1);
        stop.setDirection("Northbound");
        Field cacheField = BusTrackingService.class.getDeclaredField("routeStopsCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, java.util.List<BusStop>> cache = (java.util.Map<String, java.util.List<BusStop>>) cacheField.get(null);
        cache.put("C5", java.util.Collections.singletonList(stop));

        BusLocation payload = new BusLocation();
        payload.setTrackerImei("imei-456");
        payload.setLat(-26.173968);
        payload.setLon(27.957238);
        payload.setBusNumber("C5");

        busTrackingService.processTrackerPayload(payload);
        assertEquals("Northbound", payload.getTripDirection());
        assertEquals(1, payload.getBusStopIndex());
    }

    @Test
    void testProcessTrackerPayloadUpdatesDirectionAndStopIndex_Bidirectional_Northbound() throws Exception {
        Bus bus = new Bus();
        bus.setBusId("bus-3");
        bus.setBusNumber("C5");
        bus.setTrackerImei("imei-789");
        bus.setDriverId("driver-3");
        bus.setDriverName("Alex Smith");
        bus.setBusCompanyName("CompanyZ");
        when(busRepository.findByTrackerImei("imei-789")).thenReturn(bus);

        ValueOperations<String, Object> mockValueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockValueOps);
        when(mockValueOps.get(anyString())).thenReturn(null);
        GeoOperations<String, Object> mockGeoOps = mock(GeoOperations.class);
        when(redisTemplate.opsForGeo()).thenReturn(mockGeoOps);

        BusStop stop = new BusStop();
        stop.setLatitude(-26.183160);
        stop.setLongitude(28.020200);
        stop.setDirection("bidirectional");
        stop.setNorthboundIndex(11);
        stop.setSouthboundIndex(8);
        Field cacheField = BusTrackingService.class.getDeclaredField("routeStopsCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, java.util.List<BusStop>> cache = (java.util.Map<String, java.util.List<BusStop>>) cacheField.get(null);
        cache.put("C5", java.util.Collections.singletonList(stop));

        BusLocation payload = new BusLocation();
        payload.setTrackerImei("imei-789");
        payload.setLat(-26.183160);
        payload.setLon(28.020200);
        payload.setBusNumber("C5");
        payload.setTripDirection("Northbound");

        busTrackingService.processTrackerPayload(payload);
        assertEquals("Northbound", payload.getTripDirection());
        assertEquals(11, payload.getBusStopIndex());
    }

    @Test
    void testProcessTrackerPayloadUpdatesDirectionAndStopIndex_Bidirectional_Southbound() throws Exception {
        Bus bus = new Bus();
        bus.setBusId("bus-4");
        bus.setBusNumber("C5");
        bus.setTrackerImei("imei-101");
        bus.setDriverId("driver-4");
        bus.setDriverName("Sam Lee");
        bus.setBusCompanyName("CompanyA");
        when(busRepository.findByTrackerImei("imei-101")).thenReturn(bus);

        ValueOperations<String, Object> mockValueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockValueOps);
        when(mockValueOps.get(anyString())).thenReturn(null);
        GeoOperations<String, Object> mockGeoOps = mock(GeoOperations.class);
        when(redisTemplate.opsForGeo()).thenReturn(mockGeoOps);

        BusStop stop = new BusStop();
        stop.setLatitude(-26.183160);
        stop.setLongitude(28.020200);
        stop.setDirection("bidirectional");
        stop.setNorthboundIndex(11);
        stop.setSouthboundIndex(8);
        Field cacheField = BusTrackingService.class.getDeclaredField("routeStopsCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, java.util.List<BusStop>> cache = (java.util.Map<String, java.util.List<BusStop>>) cacheField.get(null);
        cache.put("C5", java.util.Collections.singletonList(stop));

        BusLocation payload = new BusLocation();
        payload.setTrackerImei("imei-101");
        payload.setLat(-26.183160);
        payload.setLon(28.020200);
        payload.setBusNumber("C5");
        payload.setTripDirection("Southbound");

        busTrackingService.processTrackerPayload(payload);
        assertEquals("Southbound", payload.getTripDirection());
        assertEquals(8, payload.getBusStopIndex());
    }

    @Test
    void testProcessTrackerPayloadUpdatesDirectionAndStopIndex_NoNearbyStop() throws Exception {
        Bus bus = new Bus();
        bus.setBusId("bus-5");
        bus.setBusNumber("C5");
        bus.setTrackerImei("imei-202");
        bus.setDriverId("driver-5");
        bus.setDriverName("Chris Kim");
        bus.setBusCompanyName("CompanyB");
        when(busRepository.findByTrackerImei("imei-202")).thenReturn(bus);

        ValueOperations<String, Object> mockValueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockValueOps);
        when(mockValueOps.get(anyString())).thenReturn(null);
        GeoOperations<String, Object> mockGeoOps = mock(GeoOperations.class);
        when(redisTemplate.opsForGeo()).thenReturn(mockGeoOps);

        // No stop within proximity
        Field cacheField = BusTrackingService.class.getDeclaredField("routeStopsCache");
        cacheField.setAccessible(true);
        @SuppressWarnings("unchecked")
        java.util.Map<String, java.util.List<BusStop>> cache = (java.util.Map<String, java.util.List<BusStop>>) cacheField.get(null);
        cache.put("C5", java.util.Collections.emptyList());

        BusLocation payload = new BusLocation();
        payload.setTrackerImei("imei-202");
        payload.setLat(-26.00000);
        payload.setLon(28.00000);
        payload.setBusNumber("C5");
        payload.setTripDirection("Northbound");
        payload.setBusStopIndex(99);

        busTrackingService.processTrackerPayload(payload);
        // Should not update direction or index
        assertEquals("Northbound", payload.getTripDirection());
        assertEquals(99, payload.getBusStopIndex());
    }
} 