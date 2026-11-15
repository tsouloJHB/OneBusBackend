package com.backend.onebus.repository;

import com.backend.onebus.model.GeocodingCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository for geocoding cache operations
 */
@Repository
public interface GeocodingCacheRepository extends JpaRepository<GeocodingCache, Long> {
    
    /**
     * Find a cached geocoding result by query key
     */
    Optional<GeocodingCache> findByQueryKey(String queryKey);
    
    /**
     * Find all cached entries for a query (handles multiple results)
     */
    List<GeocodingCache> findByOriginalQueryIgnoreCase(String query);
    
    /**
     * Find cache entries that are older than the specified date (for cleanup)
     */
    @Query("SELECT gc FROM GeocodingCache gc WHERE gc.createdAt < :cutoffDate")
    List<GeocodingCache> findOlderEntries(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    /**
     * Find the most frequently accessed cache entries
     */
    @Query("SELECT gc FROM GeocodingCache gc ORDER BY gc.accessCount DESC LIMIT :limit")
    List<GeocodingCache> findMostUsedEntries(@Param("limit") int limit);
    
    /**
     * Delete all cache entries older than the specified date
     */
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}
