package com.backend.onebus.service.routing;

import com.backend.onebus.repository.RouteStopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Factory for creating bus company routing strategies.
 * 
 * This factory selects the appropriate routing strategy based on the bus company name.
 * If a company-specific strategy exists, it's used; otherwise, the default strategy is used.
 */
@Component
public class BusCompanyStrategyFactory {
    
    private static final Logger logger = LoggerFactory.getLogger(BusCompanyStrategyFactory.class);
    
    @Autowired
    private RouteStopRepository routeStopRepository;
    
    /**
     * Get the routing strategy for a bus company.
     * 
     * @param companyName The name of the bus company
     * @return The routing strategy for this company
     */
    public BusCompanyRoutingStrategy getStrategy(String companyName) {
        BusCompanyRoutingStrategy strategy = null;
        
        if (companyName != null) {
            if (companyName.equalsIgnoreCase("Rea Vaya")) {
                strategy = new ReaVayaRoutingStrategy();
            } else if (companyName.equalsIgnoreCase("Metro Bus")) {
                strategy = new MetroBusRoutingStrategy();
            }
        }
        
        // Default to DefaultRoutingStrategy if no company-specific strategy found
        if (strategy == null) {
            logger.debug("No specific strategy for company '{}', using default", companyName);
            strategy = new DefaultRoutingStrategy();
        }
        
        // Set the repository on the strategy
        strategy.setRouteStopRepository(routeStopRepository);
        
        logger.debug("Using {} strategy for company: {}", strategy.getClass().getSimpleName(), companyName);
        return strategy;
    }
}
