package com.backend.onebus.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class LocationSearchRequest {
    
    @NotBlank(message = "Search query is required")
    private String query;
    
    @Positive(message = "Radius must be positive")
    private Double radiusKm = 5.0; // Default 5km radius
    
    // Optional: User's current location for better geocoding results
    private Double userLatitude;
    private Double userLongitude;
}
