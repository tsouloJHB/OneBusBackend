package com.backend.onebus.service.routing;

import com.backend.onebus.model.BusLocation;
import com.backend.onebus.model.Route;

/**
 * Metro Bus routing strategy.
 * 
 * Metro Bus currently uses the Rea Vaya routing logic as a fallback,
 * since both are BRT systems with similar terminal-point-based route structures.
 * 
 * As Metro Bus develops its own unique route patterns, this strategy can be
 * enhanced with Metro-specific rules without affecting other bus companies.
 */
public class MetroBusRoutingStrategy extends BusCompanyRoutingStrategy {
    
    private ReaVayaRoutingStrategy reaVayaFallback = new ReaVayaRoutingStrategy();
    
    @Override
    public String getCompanyName() {
        return "Metro Bus";
    }
    
    @Override
    public void setRouteStopRepository(com.backend.onebus.repository.RouteStopRepository routeStopRepository) {
        super.setRouteStopRepository(routeStopRepository);
        // Pass repository to fallback strategy as well
        reaVayaFallback.setRouteStopRepository(routeStopRepository);
    }
    
    /**
     * Infer direction for Metro Bus using Rea Vaya logic for now.
     * 
     * When Metro Bus develops its own routing patterns, add Metro-specific logic here
     * and fall back to Rea Vaya only when Metro logic doesn't apply.
     */
    @Override
    public String inferDirection(BusLocation current, BusLocation previous, Route route) {
        // For now, delegate to Rea Vaya logic
        // In the future, add Metro-specific rules here before falling back
        
        logger.debug("[Metro Bus] Using Rea Vaya fallback for direction inference");
        return reaVayaFallback.inferDirection(current, previous, route);
    }
    
    /**
     * Handle end-of-route for Metro Bus using Rea Vaya logic.
     */
    @Override
    public String handleEndOfRoute(BusLocation current, Route route) {
        logger.debug("[Metro Bus] Using Rea Vaya fallback for end-of-route handling");
        return reaVayaFallback.handleEndOfRoute(current, route);
    }
    
    // ============ Smart Bus Selection (Shadow Bus Strategy) ============

    /**
     * Metro Bus supports smart bus selection with Shadow Bus strategy.
     * This enables fallback bus selection when no buses are available in the requested direction.
     */
    @Override
    public boolean supportsSmartBusSelection() {
        return true;
    }

    /**
     * Metro Bus smart bus selection with Shadow Bus strategy.
     * Uses the same logic as Rea Vaya for now, but can be customized later.
     */
    @Override
    public String selectBestBusForClient(String busNumber, String direction, 
                                       double clientLat, double clientLon, int clientBusStopIndex) {
        logger.info("[Metro Bus] Smart bus selection for client at index {} on route {} {}", 
                   clientBusStopIndex, busNumber, direction);
        
        // Use the default Shadow Bus strategy implementation
        return selectBestBusDefault(busNumber, direction, clientLat, clientLon, clientBusStopIndex);
    }
}
