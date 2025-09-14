package com.backend.onebus.dto;

import jakarta.validation.constraints.*;

public class BusNumberCreateDTO {
    @NotNull(message = "Bus company ID is required")
    private Long busCompanyId;
    public Long getBusCompanyId() {
        return busCompanyId;
    }
    public void setBusCompanyId(Long busCompanyId) {
        this.busCompanyId = busCompanyId;
    }
    
    @NotBlank(message = "Bus number is required")
    @Size(min = 1, max = 20, message = "Bus number must be between 1 and 20 characters")
    private String busNumber;
    
    @Size(max = 100, message = "Route name cannot exceed 100 characters")
    private String routeName;
    
    @Size(max = 200, message = "Description cannot exceed 200 characters")
    private String description;
    
    @Size(max = 100, message = "Start destination cannot exceed 100 characters")
    private String startDestination;
    
    @Size(max = 100, message = "End destination cannot exceed 100 characters")
    private String endDestination;
    
    @Size(max = 50, message = "Direction cannot exceed 50 characters")
    private String direction;
    
    @DecimalMin(value = "0.0", message = "Distance must be positive")
    private Double distanceKm;
    
    @Min(value = 1, message = "Duration must be at least 1 minute")
    private Integer estimatedDurationMinutes;
    
    @Min(value = 1, message = "Frequency must be at least 1 minute")
    private Integer frequencyMinutes;
    
    private Boolean isActive;
    
    // Default constructor
    public BusNumberCreateDTO() {}
    
    // Getters and Setters
    public String getBusNumber() {
        return busNumber;
    }
    
    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }
    
    public String getRouteName() {
        return routeName;
    }
    
    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getStartDestination() {
        return startDestination;
    }
    
    public void setStartDestination(String startDestination) {
        this.startDestination = startDestination;
    }
    
    public String getEndDestination() {
        return endDestination;
    }
    
    public void setEndDestination(String endDestination) {
        this.endDestination = endDestination;
    }
    
    public String getDirection() {
        return direction;
    }
    
    public void setDirection(String direction) {
        this.direction = direction;
    }
    
    public Double getDistanceKm() {
        return distanceKm;
    }
    
    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }
    
    public Integer getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }
    
    public void setEstimatedDurationMinutes(Integer estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
    }
    
    public Integer getFrequencyMinutes() {
        return frequencyMinutes;
    }
    
    public void setFrequencyMinutes(Integer frequencyMinutes) {
        this.frequencyMinutes = frequencyMinutes;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
