package com.backend.onebus.controller;

import com.backend.onebus.dto.LocationSearchRequest;
import com.backend.onebus.dto.LocationSearchResponse;
import com.backend.onebus.service.LocationSearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * REST API for location-based bus route searching
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/search")
@Tag(name = "Location Search", description = "Search for bus routes by location")
@CrossOrigin(origins = "*")
public class LocationSearchController {
    
    private final LocationSearchService locationSearchService;
    
    public LocationSearchController(LocationSearchService locationSearchService) {
        this.locationSearchService = locationSearchService;
    }
    
    @PostMapping("/location")
    @Operation(summary = "Search routes by location name", 
               description = "Search for bus routes near a location specified by name (e.g., 'Soweto', '123 Main St')")
    public ResponseEntity<LocationSearchResponse> searchByLocation(
            @Valid @RequestBody LocationSearchRequest request) {
        
        log.info("Received location search request: {}", request);
        LocationSearchResponse response = locationSearchService.searchByLocation(request);
        return ResponseEntity.ok(response);
    }
    
    @GetMapping("/coordinates")
    @Operation(summary = "Search routes by coordinates", 
               description = "Search for bus routes near specific GPS coordinates")
    public ResponseEntity<LocationSearchResponse> searchByCoordinates(
            @Parameter(description = "Latitude", required = true) @RequestParam Double latitude,
            @Parameter(description = "Longitude", required = true) @RequestParam Double longitude,
            @Parameter(description = "Search radius in kilometers", required = false) @RequestParam(defaultValue = "5.0") Double radiusKm) {
        
        log.info("Received coordinate search request: lat={}, lon={}, radius={}km", latitude, longitude, radiusKm);
        LocationSearchResponse response = locationSearchService.searchByCoordinates(latitude, longitude, radiusKm);
        return ResponseEntity.ok(response);
    }
}
