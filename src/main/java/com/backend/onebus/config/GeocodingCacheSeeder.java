package com.backend.onebus.config;

import com.backend.onebus.model.GeocodingCache;
import com.backend.onebus.repository.GeocodingCacheRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds the geocoding cache with popular South African locations on startup
 * This replaces the need for hardcoded fallback results and improves performance
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GeocodingCacheSeeder implements CommandLineRunner {
    
    private final GeocodingCacheRepository cacheRepository;
    
    @Override
    public void run(String... args) {
        // Only seed if cache is empty
        if (cacheRepository.count() > 0) {
            log.info("Geocoding cache already seeded ({} entries), skipping", cacheRepository.count());
            return;
        }
        
        log.info("Seeding geocoding cache with popular South African locations...");
        
        List<GeocodingCache> locations = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        
        // Johannesburg major areas
        locations.add(createCacheEntry("auckland park", "Auckland Park", -26.1853203, 27.9971831, 
            "Auckland Park, Johannesburg, Gauteng, South Africa", "suburb", 0.95, now));
        locations.add(createCacheEntry("sandton", "Sandton", -26.1032, 28.0552,
            "Sandton, Johannesburg, Gauteng, South Africa", "suburb", 0.95, now));
        locations.add(createCacheEntry("midrand", "Midrand", -25.9822, 28.1105,
            "Midrand, Johannesburg, Gauteng, South Africa", "suburb", 0.95, now));
        locations.add(createCacheEntry("randburg", "Randburg", -26.1447, 27.9558,
            "Randburg, Johannesburg, Gauteng, South Africa", "suburb", 0.95, now));
        locations.add(createCacheEntry("melville", "Melville", -26.1920, 27.8810,
            "Melville, Johannesburg, Gauteng, South Africa", "suburb", 0.95, now));
        locations.add(createCacheEntry("florida", "Florida", -26.2236, 27.9559,
            "Florida, Johannesburg, Gauteng, South Africa", "suburb", 0.95, now));
        locations.add(createCacheEntry("parktown", "Parktown", -26.1855, 28.0336,
            "Parktown, Johannesburg, Gauteng, South Africa", "suburb", 0.95, now));
        locations.add(createCacheEntry("braamfontein", "Braamfontein", -26.1900, 28.0341,
            "Braamfontein, Johannesburg, Gauteng, South Africa", "suburb", 0.95, now));
        locations.add(createCacheEntry("downtown johannesburg", "Downtown Johannesburg", -26.2041, 28.0473,
            "Downtown, Johannesburg, Gauteng, South Africa", "suburb", 0.90, now));
        
        // Pretoria
        locations.add(createCacheEntry("pretoria", "Pretoria", -25.7482, 28.2293,
            "Pretoria, Gauteng, South Africa", "city", 0.95, now));
        
        // Other major SA cities
        locations.add(createCacheEntry("cape town", "Cape Town", -33.9249, 18.4241,
            "Cape Town, Western Cape, South Africa", "city", 0.95, now));
        locations.add(createCacheEntry("durban", "Durban", -29.8683, 31.0292,
            "Durban, KwaZulu-Natal, South Africa", "city", 0.95, now));
        locations.add(createCacheEntry("bloemfontein", "Bloemfontein", -29.1199, 25.5276,
            "Bloemfontein, Free State, South Africa", "city", 0.95, now));
        
        cacheRepository.saveAll(locations);
        log.info("✓ Seeded {} popular South African locations into geocoding cache", locations.size());
    }
    
    private GeocodingCache createCacheEntry(String queryKey, String originalQuery, double lat, double lon,
                                            String displayName, String locationType, double confidence, 
                                            LocalDateTime now) {
        GeocodingCache entry = new GeocodingCache();
        entry.setQueryKey(queryKey);
        entry.setOriginalQuery(originalQuery);
        entry.setLatitude(lat);
        entry.setLongitude(lon);
        entry.setDisplayName(displayName);
        entry.setLocationType(locationType);
        entry.setConfidence(confidence);
        entry.setCreatedAt(now);
        entry.setLastAccessedAt(now);
        entry.setAccessCount(0);
        return entry;
    }
}
