package com.backend.onebus.service.geocoding;

import com.backend.onebus.dto.GeocodingResult;
import com.backend.onebus.model.GeocodingCache;
import com.backend.onebus.repository.GeocodingCacheRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Service for managing geocoding cache operations
 * Handles caching, retrieval, and cleanup of geocoding results
 */
@Slf4j
@Service
public class GeocodingCacheService {
    
    private final GeocodingCacheRepository cacheRepository;
    private static final int CACHE_TTL_DAYS = 30; // Cache entries valid for 30 days
    
    public GeocodingCacheService(GeocodingCacheRepository cacheRepository) {
        this.cacheRepository = cacheRepository;
    }
    
    /**
     * Generate a cache key for the given query and optional location
     */
    public String generateCacheKey(String query, Double userLat, Double userLon) {
        String normalized = query == null ? "" : query.trim().toLowerCase();
        
        if (userLat != null && userLon != null) {
            return String.format("%s:%.4f:%.4f", normalized, userLat, userLon);
        }
        return normalized;
    }
    
    /**
     * Try to retrieve a cached geocoding result
     */
    public Optional<GeocodingResult> getCachedResult(String queryKey) {
        try {
            Optional<GeocodingCache> cached = cacheRepository.findByQueryKey(queryKey);
            
            if (cached.isPresent()) {
                GeocodingCache cache = cached.get();
                
                // Check if cache entry is still valid (not expired)
                LocalDateTime expirationDate = cache.getCreatedAt().plusDays(CACHE_TTL_DAYS);
                if (LocalDateTime.now().isAfter(expirationDate)) {
                    log.debug("Cache entry expired for key: {}", queryKey);
                    cacheRepository.delete(cache);
                    return Optional.empty();
                }
                
                // Update access tracking
                cache.recordAccess();
                cacheRepository.save(cache);
                
                log.debug("Cache hit for query: {} (accessed {} times)", queryKey, cache.getAccessCount());
                
                return Optional.of(GeocodingResult.builder()
                    .latitude(cache.getLatitude())
                    .longitude(cache.getLongitude())
                    .displayName(cache.getDisplayName())
                    .type(cache.getLocationType())
                    .confidence(cache.getConfidence())
                    .build());
            }
            
            return Optional.empty();
        } catch (Exception e) {
            log.error("Error retrieving geocoding cache for key {}: {}", queryKey, e.getMessage());
            return Optional.empty();
        }
    }
    
    /**
     * Store a geocoding result in the cache
     */
    public void cacheResult(String queryKey, String originalQuery, GeocodingResult result) {
        try {
            // Check if already cached (avoid duplicates)
            if (cacheRepository.findByQueryKey(queryKey).isPresent()) {
                log.debug("Cache entry already exists for key: {}", queryKey);
                return;
            }
            
            GeocodingCache cacheEntry = GeocodingCache.builder()
                .queryKey(queryKey)
                .originalQuery(originalQuery)
                .latitude(result.getLatitude())
                .longitude(result.getLongitude())
                .displayName(result.getDisplayName())
                .locationType(result.getType())
                .confidence(result.getConfidence())
                .build();
            
            cacheRepository.save(cacheEntry);
            log.debug("Cached geocoding result for key: {}", queryKey);
        } catch (Exception e) {
            log.error("Error caching geocoding result for key {}: {}", queryKey, e.getMessage());
        }
    }
    
    /**
     * Clean up expired cache entries
     * Should be called periodically (e.g., via scheduled task)
     */
    public void cleanupExpiredEntries() {
        try {
            LocalDateTime cutoffDate = LocalDateTime.now().minusDays(CACHE_TTL_DAYS);
            cacheRepository.deleteByCreatedAtBefore(cutoffDate);
            log.info("Cleaned up geocoding cache entries older than {}", cutoffDate);
        } catch (Exception e) {
            log.error("Error cleaning up expired cache entries: {}", e.getMessage());
        }
    }
    
    /**
     * Get cache statistics
     */
    public long getCacheSize() {
        return cacheRepository.count();
    }
}
