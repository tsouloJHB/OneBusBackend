package com.backend.onebus.service.routing;

import com.backend.onebus.model.BusLocation;
import com.backend.onebus.model.Route;

/**
 * Default routing strategy for unknown bus companies.
 * 
 * Uses Rea Vaya logic as the default fallback, since it's a proven
 * routing pattern that works well for most BRT systems.
 * 
 * When a new bus company joins the OneBus network, it will use this
 * default strategy until a company-specific strategy is implemented.
 */
public class DefaultRoutingStrategy extends BusCompanyRoutingStrategy {
    
    private ReaVayaRoutingStrategy reaVayaFallback = new ReaVayaRoutingStrategy();
    
    @Override
    public String getCompanyName() {
        return "Default";
    }
    
    @Override
    public void setRouteStopRepository(com.backend.onebus.repository.RouteStopRepository routeStopRepository) {
        super.setRouteStopRepository(routeStopRepository);
        // Pass repository to fallback strategy as well
        reaVayaFallback.setRouteStopRepository(routeStopRepository);
    }
    
    /**
     * Use Rea Vaya logic for unknown companies.
     * This provides reasonable direction inference for most transit systems.
     */
    @Override
    public String inferDirection(BusLocation current, BusLocation previous, Route route) {
        logger.debug("[Default] Unknown company, using Rea Vaya fallback for bus {}", 
            current.getBusNumber());
        return reaVayaFallback.inferDirection(current, previous, route);
    }
    
    /**
     * Use Rea Vaya logic for end-of-route handling.
     */
    @Override
    public String handleEndOfRoute(BusLocation current, Route route) {
        logger.debug("[Default] Unknown company, using Rea Vaya fallback for end-of-route");
        return reaVayaFallback.handleEndOfRoute(current, route);
    }
}
