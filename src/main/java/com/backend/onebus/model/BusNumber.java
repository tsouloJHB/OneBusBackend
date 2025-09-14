package com.backend.onebus.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Entity
@Table(name = "bus_numbers")
public class BusNumber {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Bus number is required")
    @Size(min = 1, max = 20, message = "Bus number must be between 1 and 20 characters")
    @Column(name = "bus_number", nullable = false, length = 20)
    private String busNumber;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_company_id", nullable = false)
    private BusCompany busCompany;
    
    @Transient
    private String companyName;
    
    @Size(max = 100, message = "Route name cannot exceed 100 characters")
    @Column(name = "route_name", length = 100)
    private String routeName;
    
    @Size(max = 200, message = "Description cannot exceed 200 characters")
    @Column(name = "description", length = 200)
    private String description;
    
    @Size(max = 100, message = "Start destination cannot exceed 100 characters")
    @Column(name = "start_destination", length = 100)
    private String startDestination;
    
    @Size(max = 100, message = "End destination cannot exceed 100 characters")
    @Column(name = "end_destination", length = 100)
    private String endDestination;
    
    @Size(max = 50, message = "Direction cannot exceed 50 characters")
    @Column(name = "direction", length = 50)
    private String direction; // e.g., "Northbound", "Southbound", "Bidirectional"
    
    @Column(name = "distance_km")
    private Double distanceKm;
    
    @Column(name = "estimated_duration_minutes")
    private Integer estimatedDurationMinutes;
    
    @Column(name = "frequency_minutes")
    private Integer frequencyMinutes;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Default constructor
    public BusNumber() {}
    
    // Constructor with required fields
    public BusNumber(String busNumber, BusCompany busCompany) {
        this.busNumber = busNumber;
        this.busCompany = busCompany;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getBusNumber() {
        return busNumber;
    }
    
    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }
    
    public BusCompany getBusCompany() {
        return busCompany;
    }

    public void setBusCompany(BusCompany busCompany) {
        this.busCompany = busCompany;
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
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getCompanyName() {
        return busCompany != null ? busCompany.getName() : null;
    }
    
    public void setCompanyName(String companyName) {
        // This is a read-only field populated from the relationship
        // No-op setter to satisfy JSON serialization
    }
    
    @Override
    public String toString() {
        return "BusNumber{" +
                "id=" + id +
                ", busNumber='" + busNumber + '\'' +
                ", busCompanyId='" + (busCompany != null ? busCompany.getId() : null) + '\'' +
                ", routeName='" + routeName + '\'' +
                ", startDestination='" + startDestination + '\'' +
                ", endDestination='" + endDestination + '\'' +
                ", direction='" + direction + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
