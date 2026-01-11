package com.backend.onebus.service.routing;

import com.backend.onebus.repository.RouteStopRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
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
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    private final java.util.Map<String, BusCompanyRoutingStrategy> strategyCache = new java.util.concurrent.ConcurrentHashMap<>();
    
    /**
     * Get the routing strategy for a bus company.
     * 
     * @param companyName The name of the bus company
     * @return The routing strategy for this company
     */
    public BusCompanyRoutingStrategy getStrategy(String companyName) {
        String key = companyName != null ? companyName.toLowerCase() : "default";
        
        return strategyCache.computeIfAbsent(key, k -> {
            BusCompanyRoutingStrategy strategy = null;
            
            if (companyName != null) {
                if (companyName.equalsIgnoreCase("Rea Vaya")) {
                    strategy = new ReaVayaRoutingStrategy();
                    logger.debug("Created new ReaVayaRoutingStrategy");
                } else if (companyName.equalsIgnoreCase("Metro Bus")) {
                    strategy = new MetroBusRoutingStrategy();
                    logger.debug("Created new MetroBusRoutingStrategy");
                }
            }
            
            if (strategy == null) {
                logger.debug("No specific strategy for company '{}', creating default", companyName);
                strategy = new DefaultRoutingStrategy();
            }
            
            // Set the repository and Redis template on the strategy
            strategy.setRouteStopRepository(routeStopRepository);
            strategy.setRedisTemplate(redisTemplate);
            
            return strategy;
        });
    }
}
