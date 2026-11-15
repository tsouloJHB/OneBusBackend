package com.backend.onebus.service.geocoding;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Scheduled tasks for geocoding cache maintenance
 * Automatically cleans up old and unused cache entries
 */
@Slf4j
@Service
public class GeocodingCacheMaintenanceService {
    
    private final GeocodingCacheService cacheService;
    
    public GeocodingCacheMaintenanceService(GeocodingCacheService cacheService) {
        this.cacheService = cacheService;
    }
    
    /**
     * Run cleanup task daily at 2 AM
     * Removes cache entries older than 30 days
     */
    @Scheduled(cron = "0 0 2 * * *")  // Daily at 2:00 AM
    public void cleanupExpiredCacheEntries() {
        try {
            long sizeBefore = cacheService.getCacheSize();
            cacheService.cleanupExpiredEntries();
            long sizeAfter = cacheService.getCacheSize();
            long removed = sizeBefore - sizeAfter;
            
            log.info("Geocoding cache cleanup completed. Removed {} entries (Before: {}, After: {})", 
                removed, sizeBefore, sizeAfter);
        } catch (Exception e) {
            log.error("Error during geocoding cache cleanup: {}", e.getMessage(), e);
        }
    }
}
