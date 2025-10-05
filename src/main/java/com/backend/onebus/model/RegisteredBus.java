package com.backend.onebus.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

@Entity
@Table(name = "registered_buses")
public class RegisteredBus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Company is required")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private BusCompany company;

    @NotBlank(message = "Registration number is required")
    @Size(min = 2, max = 50, message = "Registration number must be between 2 and 50 characters")
    @Column(name = "registration_number", nullable = false, length = 50)
    private String registrationNumber;

    @Size(max = 20, message = "Bus number cannot exceed 20 characters")
    @Column(name = "bus_number", length = 20)
    private String busNumber;

    @Size(max = 50, message = "Bus ID cannot exceed 50 characters")
    @Column(name = "bus_id", length = 50)
    private String busId;

    @NotBlank(message = "Tracker IMEI is required")
    @Size(min = 10, max = 20, message = "Tracker IMEI must be between 10 and 20 characters")
    @Column(name = "tracker_imei", nullable = false, length = 20)
    private String trackerImei;

    @Size(max = 50, message = "Driver ID cannot exceed 50 characters")
    @Column(name = "driver_id", length = 50)
    private String driverId;

    @Size(max = 100, message = "Driver name cannot exceed 100 characters")
    @Column(name = "driver_name", length = 100)
    private String driverName;

    @NotBlank(message = "Model is required")
    @Size(min = 1, max = 50, message = "Model must be between 1 and 50 characters")
    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @NotNull(message = "Year is required")
    @Column(name = "\"year\"", nullable = false)
    private Integer year;

    @NotNull(message = "Capacity is required")
    @Column(name = "capacity", nullable = false)
    private Integer capacity;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private BusStatus status;

    @Column(name = "route_id")
    private Long routeId;

    @Column(name = "route_name", length = 100)
    private String routeName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "route_assigned_at")
    private LocalDateTime routeAssignedAt;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "last_inspection")
    private LocalDateTime lastInspection;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "next_inspection")
    private LocalDateTime nextInspection;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum BusStatus {
        ACTIVE, INACTIVE, MAINTENANCE, RETIRED
    }

    // Default constructor
    public RegisteredBus() {}

    // Pre-persist callback
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    // Pre-update callback
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

    public BusCompany getCompany() {
        return company;
    }

    public void setCompany(BusCompany company) {
        this.company = company;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }

    public String getBusId() {
        return busId;
    }

    public void setBusId(String busId) {
        this.busId = busId;
    }

    public String getTrackerImei() {
        return trackerImei;
    }

    public void setTrackerImei(String trackerImei) {
        this.trackerImei = trackerImei;
    }

    public String getDriverId() {
        return driverId;
    }

    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }

    public String getDriverName() {
        return driverName;
    }

    public void setDriverName(String driverName) {
        this.driverName = driverName;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Integer getCapacity() {
        return capacity;
    }

    public void setCapacity(Integer capacity) {
        this.capacity = capacity;
    }

    public BusStatus getStatus() {
        return status;
    }

    public void setStatus(BusStatus status) {
        this.status = status;
    }

    public Long getRouteId() {
        return routeId;
    }

    public void setRouteId(Long routeId) {
        this.routeId = routeId;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public LocalDateTime getRouteAssignedAt() {
        return routeAssignedAt;
    }

    public void setRouteAssignedAt(LocalDateTime routeAssignedAt) {
        this.routeAssignedAt = routeAssignedAt;
    }

    public LocalDateTime getLastInspection() {
        return lastInspection;
    }

    public void setLastInspection(LocalDateTime lastInspection) {
        this.lastInspection = lastInspection;
    }

    public LocalDateTime getNextInspection() {
        return nextInspection;
    }

    public void setNextInspection(LocalDateTime nextInspection) {
        this.nextInspection = nextInspection;
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
}