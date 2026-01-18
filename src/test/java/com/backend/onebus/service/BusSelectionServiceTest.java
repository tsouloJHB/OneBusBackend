package com.backend.onebus.service;

import com.backend.onebus.model.BusLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BusSelectionServiceTest {
    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    @InjectMocks
    private BusSelectionService busSelectionService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    private BusLocation createBus(String busId, String direction, int stopIndex) {
        BusLocation bus = new BusLocation();
        bus.setBusId(busId);
        bus.setBusNumber("C6");
        bus.setTripDirection(direction);
        bus.setBusStopIndex(stopIndex);
        return bus;
    }

    @Test
    void testNoBusesAvailable() {
        when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());
        String result = busSelectionService.selectBestBusForClient("C6", "Northbound", 0, 0, 1);
        assertNull(result, "Should return null when no buses are available");
    }

    @Test
    void testBusInRequestedDirectionApproachingClient() {
        // Bus at index 3 is approaching client at index 4 (BEHIND client)
        BusLocation bus1 = createBus("bus1", "Northbound", 3);
        BusLocation bus2 = createBus("bus2", "Northbound", 1);
        Set<String> keys = new HashSet<>(Arrays.asList("bus:location:bus1", "bus:location:bus2"));
        when(redisTemplate.keys(anyString())).thenReturn(keys);
        when(valueOperations.get("bus:location:bus1")).thenReturn(bus1);
        when(valueOperations.get("bus:location:bus2")).thenReturn(bus2);
        
        // Client at index 4
        String result = busSelectionService.selectBestBusForClient("C6", "Northbound", 0, 0, 4);
        assertEquals("bus1", result, "Should assign the closest approaching bus (index 3 closest to 4)");
    }

    @Test
    void testBusInRequestedDirectionAtClient() {
        BusLocation bus1 = createBus("bus1", "Northbound", 3);
        Set<String> keys = new HashSet<>(Collections.singletonList("bus:location:bus1"));
        when(redisTemplate.keys(anyString())).thenReturn(keys);
        when(valueOperations.get("bus:location:bus1")).thenReturn(bus1);
        String result = busSelectionService.selectBestBusForClient("C6", "Northbound", 0, 0, 3);
        assertEquals("bus1", result, "Should assign bus at client's stop");
    }

    @Test
    void testFallbackToOppositeDirection() {
        BusLocation bus1 = createBus("bus1", "Southbound", 10);
        Set<String> keys = new HashSet<>(Collections.singletonList("bus:location:bus1"));
        when(redisTemplate.keys(anyString())).thenReturn(keys);
        when(valueOperations.get("bus:location:bus1")).thenReturn(bus1);
        String result = busSelectionService.selectBestBusForClient("C6", "Northbound", 0, 0, 1);
        assertEquals("bus1", result, "Should assign closest bus in opposite direction");
    }

    @Test
    void testNoNegativeIndexBusSuggested() {
        BusLocation bus1 = createBus("bus1", "Southbound", -1);
        Set<String> keys = new HashSet<>(Collections.singletonList("bus:location:bus1"));
        when(redisTemplate.keys(anyString())).thenReturn(keys);
        when(valueOperations.get("bus:location:bus1")).thenReturn(bus1);
        String result = busSelectionService.selectBestBusForClient("C6", "Northbound", 0, 0, 1);
        assertNull(result, "Should not suggest bus with negative index");
    }

    @Test
    void testMultipleBusesOppositeDirectionClosestSelected() {
        BusLocation bus1 = createBus("bus1", "Southbound", 2);
        BusLocation bus2 = createBus("bus2", "Southbound", 8);
        Set<String> keys = new HashSet<>(Arrays.asList("bus:location:bus1", "bus:location:bus2"));
        when(redisTemplate.keys(anyString())).thenReturn(keys);
        when(valueOperations.get("bus:location:bus1")).thenReturn(bus1);
        when(valueOperations.get("bus:location:bus2")).thenReturn(bus2);
        String result = busSelectionService.selectBestBusForClient("C6", "Northbound", 0, 0, 5);
        assertEquals("bus1", result, "Should select closest bus in opposite direction");
    }

    @Test
    void testNoFallbackIfBusInRequestedDirectionPassed() {
        // Bus in requested direction exists but has passed (index 7 > client index 6)
        BusLocation busRequested = createBus("busRequested", "Northbound", 7);
        // Bus in opposite direction exists (would normally be selected as fallback)
        BusLocation busOpposite = createBus("busOpposite", "Southbound", 5);
        
        Set<String> keys = new HashSet<>(Arrays.asList("bus:location:busRequested", "bus:location:busOpposite"));
        when(redisTemplate.keys(anyString())).thenReturn(keys);
        when(valueOperations.get("bus:location:busRequested")).thenReturn(busRequested);
        when(valueOperations.get("bus:location:busOpposite")).thenReturn(busOpposite);
        
        // Client is at index 6 in Northbound
        String result = busSelectionService.selectBestBusForClient("C6", "Northbound", 0, 0, 6);
        
        assertNull(result, "Should return null (not fall back) because a bus exists in the requested direction, even though it passed");
    }
} 