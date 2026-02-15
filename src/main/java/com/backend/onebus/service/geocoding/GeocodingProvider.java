package com.backend.onebus.service.geocoding;

import com.backend.onebus.dto.GeocodingResult;

import java.util.List;

/**
 * Interface for geocoding providers.
 * Allows switching between different geocoding services (OSM Nominatim, Google Places, etc.)
 * without changing business logic.
 */
public interface GeocodingProvider {
    
    /**
     * Geocode a location query to coordinates
     * @param query The location search query (e.g., "Soweto", "123 Main St")
     * @param userLat Optional user latitude for biasing results
     * @param userLon Optional user longitude for biasing results
     * @return List of possible geocoding results, ordered by relevance
     */
    List<GeocodingResult> geocode(String query, Double userLat, Double userLon);
    
    /**
     * Reverse geocode coordinates to an address
     * @param latitude The latitude
     * @param longitude The longitude
     * @return The geocoding result with address information
     */
    GeocodingResult reverseGeocode(Double latitude, Double longitude);
    
    /**
     * Get the provider name
     * @return The name of the geocoding provider
     */
    String getProviderName();
}
