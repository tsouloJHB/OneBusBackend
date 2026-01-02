package com.backend.onebus.controller;

import com.backend.onebus.model.BusCompany;
import com.backend.onebus.model.BusNumber;
import com.backend.onebus.repository.BusNumberRepository;
import com.backend.onebus.service.routing.BusCompanyStrategyFactory;
import com.backend.onebus.service.routing.BusCompanyRoutingStrategy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Test that BusStreamingController correctly identifies company strategies
 * for smart bus selection.
 */
class BusStreamingControllerStrategyTest {

    @Mock
    private BusNumberRepository busNumberRepository;

    private BusCompanyStrategyFactory strategyFactory;
    private BusStreamingController controller;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        strategyFactory = new BusCompanyStrategyFactory();
        controller = new BusStreamingController();
        
        // Use reflection to set the private field for testing
        try {
            var field = BusStreamingController.class.getDeclaredField("busNumberRepository");
            field.setAccessible(true);
            field.set(controller, busNumberRepository);
        } catch (Exception e) {
            fail("Could not set busNumberRepository field: " + e.getMessage());
        }
    }

    @Test
    void testGetCompanyNameForReaVayaBus() {
        // Setup mock data
        BusCompany reaVayaCompany = new BusCompany();
        reaVayaCompany.setName("Rea Vaya");

        BusNumber busNumber = new BusNumber();
        busNumber.setBusNumber("C6");
        busNumber.setBusCompany(reaVayaCompany);
        busNumber.setIsActive(true);

        when(busNumberRepository.findByIsActiveTrue()).thenReturn(List.of(busNumber));

        // Test the private method using reflection
        try {
            var method = BusStreamingController.class.getDeclaredMethod("getCompanyNameForBusNumber", String.class);
            method.setAccessible(true);
            String result = (String) method.invoke(controller, "C6");
            
            assertEquals("Rea Vaya", result, "Should return Rea Vaya for C6 bus");
        } catch (Exception e) {
            fail("Could not invoke getCompanyNameForBusNumber method: " + e.getMessage());
        }
    }

    @Test
    void testGetCompanyNameForMetroBus() {
        // Setup mock data
        BusCompany metroBusCompany = new BusCompany();
        metroBusCompany.setName("Metro Bus");

        BusNumber busNumber = new BusNumber();
        busNumber.setBusNumber("M1");
        busNumber.setBusCompany(metroBusCompany);
        busNumber.setIsActive(true);

        when(busNumberRepository.findByIsActiveTrue()).thenReturn(List.of(busNumber));

        // Test the private method using reflection
        try {
            var method = BusStreamingController.class.getDeclaredMethod("getCompanyNameForBusNumber", String.class);
            method.setAccessible(true);
            String result = (String) method.invoke(controller, "M1");
            
            assertEquals("Metro Bus", result, "Should return Metro Bus for M1 bus");
        } catch (Exception e) {
            fail("Could not invoke getCompanyNameForBusNumber method: " + e.getMessage());
        }
    }

    @Test
    void testStrategyFactoryReturnsCorrectStrategies() {
        BusCompanyRoutingStrategy reaVayaStrategy = strategyFactory.getStrategy("Rea Vaya");
        BusCompanyRoutingStrategy metroBusStrategy = strategyFactory.getStrategy("Metro Bus");
        BusCompanyRoutingStrategy defaultStrategy = strategyFactory.getStrategy("Unknown Company");

        assertTrue(reaVayaStrategy.supportsSmartBusSelection(), 
                  "Rea Vaya strategy should support smart selection");
        assertTrue(metroBusStrategy.supportsSmartBusSelection(), 
                  "Metro Bus strategy should support smart selection");
        assertFalse(defaultStrategy.supportsSmartBusSelection(), 
                   "Default strategy should not support smart selection");

        assertEquals("Rea Vaya", reaVayaStrategy.getCompanyName());
        assertEquals("Metro Bus", metroBusStrategy.getCompanyName());
        assertEquals("Default", defaultStrategy.getCompanyName());
    }
}