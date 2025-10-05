package com.backend.onebus.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;

public class RegisteredBusResponseDTO {

    private Long id;
    private Long companyId;
    private String companyName;
    private String registrationNumber;
    private String busNumber;
    private String busId;
    private String trackerImei;
    private String driverId;
    private String driverName;
    private String model;
    private Integer year;
    private Integer capacity;
    private String status;
    private Long routeId;
    private String routeName;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime routeAssignedAt;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime lastInspection;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime nextInspection;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;

    // Default constructor
    public RegisteredBusResponseDTO() {}

    // Constructor
    public RegisteredBusResponseDTO(Long id, String registrationNumber, String model, Integer year) {
        this.id = id;
        this.registrationNumber = registrationNumber;
        this.model = model;
        this.year = year;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
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