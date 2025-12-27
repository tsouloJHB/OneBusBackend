package com.backend.onebus.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Entity
@Table(name = "buses")
public class Bus {
    @Id
    private String busId;
    
    // === Tracker Fields ===
    @NotBlank
    @Column(nullable = false, length = 20)
    private String trackerImei;
    
    @NotBlank
    @Column(length = 20)
    private String busNumber;
    
    @Column(length = 100)
    private String route;
    
    // === Company Relationship ===
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_company_id", nullable = true)
    @JsonIgnore
    private BusCompany busCompany;
    
    @Column(name = "bus_company_name", length = 100)
    private String busCompanyName;
    
    // === Driver Information ===
    @Column(length = 50)
    private String driverId;
    
    @Column(length = 100)
    private String driverName;
    
    // === Physical Bus Details ===
    @Column(length = 50)
    private String model;
    
    @Column(name = "\"year\"")
    private Integer year;
    
    @Column
    private Integer capacity;
    
    // === Registration & Status ===
    @Column(name = "registration_number", length = 50)
    private String registrationNumber;
    
    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private BusStatus status = BusStatus.ACTIVE;
    
    @Column(name = "operational_status", nullable = false, length = 20)
    private String operationalStatus = "active";
    
    // === Route Assignment ===
    @Column(name = "route_id")
    private Long routeId;
    
    @Column(name = "route_name", length = 100)
    private String routeName;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "route_assigned_at")
    private LocalDateTime routeAssignedAt;
    
    // === Maintenance ===
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "last_inspection")
    private LocalDateTime lastInspection;
    
    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "next_inspection")
    private LocalDateTime nextInspection;
    
    // === Timestamps ===
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum BusStatus {
        ACTIVE, INACTIVE, MAINTENANCE, RETIRED
    }
    
    @PrePersist
    protected void onCreate() {
        if (createdAt == null) createdAt = LocalDateTime.now();
        if (updatedAt == null) updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // === Getters and Setters ===
    public String getBusId() { return busId; }
    public void setBusId(String busId) { this.busId = busId; }
    
    public String getTrackerImei() { return trackerImei; }
    public void setTrackerImei(String trackerImei) { this.trackerImei = trackerImei; }
    
    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
    
    public String getRoute() { return route; }
    public void setRoute(String route) { this.route = route; }
    
    public BusCompany getBusCompany() { return busCompany; }
    public void setBusCompany(BusCompany busCompany) { this.busCompany = busCompany; }
    
    public String getBusCompanyName() { return busCompanyName; }
    public void setBusCompanyName(String busCompanyName) { this.busCompanyName = busCompanyName; }
    
    public String getDriverId() { return driverId; }
    public void setDriverId(String driverId) { this.driverId = driverId; }
    
    public String getDriverName() { return driverName; }
    public void setDriverName(String driverName) { this.driverName = driverName; }
    
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    
    public Integer getYear() { return year; }
    public void setYear(Integer year) { this.year = year; }
    
    public Integer getCapacity() { return capacity; }
    public void setCapacity(Integer capacity) { this.capacity = capacity; }
    
    public String getRegistrationNumber() { return registrationNumber; }
    public void setRegistrationNumber(String registrationNumber) { this.registrationNumber = registrationNumber; }
    
    public BusStatus getStatus() { return status; }
    public void setStatus(BusStatus status) { this.status = status; }
    
    public String getOperationalStatus() { return operationalStatus; }
    public void setOperationalStatus(String operationalStatus) { this.operationalStatus = operationalStatus; }
    
    public Long getRouteId() { return routeId; }
    public void setRouteId(Long routeId) { this.routeId = routeId; }
    
    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }
    
    public LocalDateTime getRouteAssignedAt() { return routeAssignedAt; }
    public void setRouteAssignedAt(LocalDateTime routeAssignedAt) { this.routeAssignedAt = routeAssignedAt; }
    
    public LocalDateTime getLastInspection() { return lastInspection; }
    public void setLastInspection(LocalDateTime lastInspection) { this.lastInspection = lastInspection; }
    
    public LocalDateTime getNextInspection() { return nextInspection; }
    public void setNextInspection(LocalDateTime nextInspection) { this.nextInspection = nextInspection; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}