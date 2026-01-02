package com.backend.onebus.service.routing;

import com.backend.onebus.model.BusLocation;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Test smart bus selection functionality in routing strategies.
 * Verifies that Rea Vaya and Metro Bus support smart selection while Default does not.
 */
class RoutingStrategySmartSelectionTest {

    @Mock
    private RedisTemplate<String, Object> redisTemplate;

    @Mock
    private ValueOperations<String, Object> valueOperations;

    private ReaVayaRoutingStrategy reaVayaStrategy;
    private MetroBusRoutingStrategy metroBusStrategy;
    private DefaultRoutingStrategy defaultStrategy;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        reaVayaStrategy = new ReaVayaRoutingStrategy();
        reaVayaStrategy.setRedisTemplate(redisTemplate);

        metroBusStrategy = new MetroBusRoutingStrategy();
        metroBusStrategy.setRedisTemplate(redisTemplate);

        defaultStrategy = new DefaultRoutingStrategy();
        defaultStrategy.setRedisTemplate(redisTemplate);
    }

    @Test
    void testReaVayaSupportsSmartSelection() {
        assertTrue(reaVayaStrategy.supportsSmartBusSelection(), 
                  "Rea Vaya should support smart bus selection");
    }

    @Test
    void testMetroBusSupportsSmartSelection() {
        assertTrue(metroBusStrategy.supportsSmartBusSelection(), 
                  "Metro Bus should support smart bus selection");
    }

    @Test
    void testDefaultDoesNotSupportSmartSelection() {
        assertFalse(defaultStrategy.supportsSmartBusSelection(), 
                   "Default strategy should not support smart bus selection");
    }

    @Test
    void testReaVayaSmartSelectionWithNoBuses() {
        when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());
        
        String result = reaVayaStrategy.selectBestBusForClient("C6", "Northbound", 0, 0, 1);
        
        assertNull(result, "Should return null when no buses are available");
    }

    @Test
    void testMetroBusSmartSelectionWithNoBuses() {
        when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());
        
        String result = metroBusStrategy.selectBestBusForClient("C6", "Northbound", 0, 0, 1);
        
        assertNull(result, "Should return null when no buses are available");
    }

    @Test
    void testReaVayaSmartSelectionWithAvailableBus() {
        // Setup mock data
        BusLocation bus1 = new BusLocation();
        bus1.setBusId("bus1");
        bus1.setBusNumber("C6");
        bus1.setTripDirection("Northbound");
        bus1.setBusStopIndex(5);

        Set<String> keys = Set.of("bus:location:bus1");
        when(redisTemplate.keys(anyString())).thenReturn(keys);
        when(valueOperations.get("bus:location:bus1")).thenReturn(bus1);

        String result = reaVayaStrategy.selectBestBusForClient("C6", "Northbound", 0, 0, 4);

        assertEquals("bus1", result, "Should select the available bus ahead of client");
    }

    @Test
    void testCompanyNames() {
        assertEquals("Rea Vaya", reaVayaStrategy.getCompanyName());
        assertEquals("Metro Bus", metroBusStrategy.getCompanyName());
        assertEquals("Default", defaultStrategy.getCompanyName());
    }
}