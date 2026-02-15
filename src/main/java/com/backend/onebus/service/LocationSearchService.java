package com.backend.onebus.service;

import com.backend.onebus.dto.*;
import com.backend.onebus.service.geocoding.GeocodingProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Main service for location-based search.
 * Orchestrates geocoding and spatial search while keeping providers decoupled.
 */
@Slf4j
@Service
public class LocationSearchService {
    
    private final GeocodingProvider geocodingProvider;
    private final SpatialSearchService spatialSearchService;
    
    public LocationSearchService(
            @Qualifier("nominatimGeocodingProvider") GeocodingProvider geocodingProvider,
            SpatialSearchService spatialSearchService) {
        this.geocodingProvider = geocodingProvider;
        this.spatialSearchService = spatialSearchService;
        log.info("LocationSearchService initialized with provider: {}", geocodingProvider.getProviderName());
    }
    
    /**
     * Search for bus routes by location name/query
     */
    public LocationSearchResponse searchByLocation(LocationSearchRequest request) {
        log.info("Processing location search: query='{}', radius={}km", 
                request.getQuery(), request.getRadiusKm());
        
        // Step 1: Geocode the query to get coordinates
        List<GeocodingResult> geocodingResults = geocodingProvider.geocode(
            request.getQuery(), 
            request.getUserLatitude(), 
            request.getUserLongitude()
        );
        
        if (geocodingResults.isEmpty()) {
            log.warn("No geocoding results found for query: {}", request.getQuery());
            return LocationSearchResponse.builder()
                .geocodingResult(null)
                .nearbyRoutes(List.of())
                .totalRoutesFound(0)
                .searchRadiusKm(request.getRadiusKm())
                .build();
        }
        
        // Use the first (most relevant) result
        GeocodingResult primaryResult = geocodingResults.get(0);
        log.info("Geocoding successful: {} ({}, {})", 
                primaryResult.getDisplayName(), 
                primaryResult.getLatitude(), 
                primaryResult.getLongitude());
        
        // Step 2: Find routes near the geocoded location
        List<RouteProximityResult> nearbyRoutes = spatialSearchService.findRoutesNearLocation(
            primaryResult.getLatitude(),
            primaryResult.getLongitude(),
            request.getRadiusKm()
        );
        
        log.info("Found {} routes within {}km of {}", 
                nearbyRoutes.size(), request.getRadiusKm(), primaryResult.getDisplayName());
        
        return LocationSearchResponse.builder()
            .geocodingResult(primaryResult)
            .nearbyRoutes(nearbyRoutes)
            .totalRoutesFound(nearbyRoutes.size())
            .searchRadiusKm(request.getRadiusKm())
            .build();
    }
    
    /**
     * Search for routes by direct coordinates
     */
    public LocationSearchResponse searchByCoordinates(Double latitude, Double longitude, Double radiusKm) {
        log.info("Processing coordinate search: ({}, {}), radius={}km", latitude, longitude, radiusKm);
        
        // Reverse geocode to get location name
        GeocodingResult geocodingResult = geocodingProvider.reverseGeocode(latitude, longitude);
        
        // Find routes near the coordinates
        List<RouteProximityResult> nearbyRoutes = spatialSearchService.findRoutesNearLocation(
            latitude,
            longitude,
            radiusKm
        );
        
        return LocationSearchResponse.builder()
            .geocodingResult(geocodingResult)
            .nearbyRoutes(nearbyRoutes)
            .totalRoutesFound(nearbyRoutes.size())
            .searchRadiusKm(radiusKm)
            .build();
    }
}
