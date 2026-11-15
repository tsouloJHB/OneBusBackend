package com.backend.onebus.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * Cache for geocoding results to reduce API calls to Nominatim
 * Stores successful geocoding lookups to avoid repeated API requests
 */
@Entity
@Table(name = "geocoding_cache", indexes = {
    @Index(name = "idx_query", columnList = "query_key"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GeocodingCache {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    /**
     * Normalized query key for cache lookup
     * Format: "query:latitude:longitude" to handle location-biased searches
     */
    @Column(name = "query_key", nullable = false, length = 500)
    private String queryKey;
    
    /**
     * Original search query
     */
    @Column(name = "original_query", nullable = false, length = 255)
    private String originalQuery;
    
    /**
     * Cached latitude
     */
    @Column(name = "latitude", nullable = false)
    private Double latitude;
    
    /**
     * Cached longitude
     */
    @Column(name = "longitude", nullable = false)
    private Double longitude;
    
    /**
     * Display name from geocoding provider
     */
    @Column(name = "display_name", length = 500)
    private String displayName;
    
    /**
     * Location type (suburb, city, village, etc.)
     */
    @Column(name = "location_type", length = 100)
    private String locationType;
    
    /**
     * Confidence score (0.0 - 1.0)
     */
    @Column(name = "confidence")
    private Double confidence;
    
    /**
     * Timestamp when this cache entry was created
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    /**
     * Timestamp when this cache entry was last accessed
     */
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    /**
     * Number of times this cache entry has been used
     */
    @Column(name = "access_count", columnDefinition = "INTEGER DEFAULT 0")
    private Integer accessCount = 0;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
        accessCount = 0;
    }
    
    /**
     * Update access tracking when cache entry is used
     */
    public void recordAccess() {
        this.lastAccessedAt = LocalDateTime.now();
        this.accessCount = (accessCount != null ? accessCount : 0) + 1;
    }
}
