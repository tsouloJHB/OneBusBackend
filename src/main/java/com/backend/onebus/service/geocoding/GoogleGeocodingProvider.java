package com.backend.onebus.service.geocoding;

import com.backend.onebus.dto.GeocodingResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Google Places API implementation (placeholder for future use)
 * Activated when geocoding.provider=google in application.properties
 */
@Slf4j
@Service("googleGeocodingProvider")
@ConditionalOnProperty(name = "geocoding.provider", havingValue = "google")
public class GoogleGeocodingProvider implements GeocodingProvider {
    
    // Add Google API key configuration
    // @Value("${geocoding.google.api-key}")
    // private String apiKey;
    
    @Override
    public List<GeocodingResult> geocode(String query, Double userLat, Double userLon) {
        // TODO: Implement Google Places API integration
        log.warn("Google geocoding provider not yet implemented");
        return new ArrayList<>();
    }
    
    @Override
    public GeocodingResult reverseGeocode(Double latitude, Double longitude) {
        // TODO: Implement Google Places API reverse geocoding
        log.warn("Google reverse geocoding not yet implemented");
        return null;
    }
    
    @Override
    public String getProviderName() {
        return "Google Places";
    }
}
