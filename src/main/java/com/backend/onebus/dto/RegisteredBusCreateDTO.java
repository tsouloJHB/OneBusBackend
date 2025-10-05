package com.backend.onebus.dto;

import jakarta.validation.constraints.*;
import com.backend.onebus.model.RegisteredBus;
import java.time.LocalDateTime;

public class RegisteredBusCreateDTO {

    @NotNull(message = "Company ID is required")
    private Long companyId;

    @NotBlank(message = "Registration number is required")
    @Size(min = 2, max = 50, message = "Registration number must be between 2 and 50 characters")
    private String registrationNumber;

    @Size(max = 20, message = "Bus number cannot exceed 20 characters")
    private String busNumber;

    @Size(max = 50, message = "Bus ID cannot exceed 50 characters")
    private String busId;

    @NotBlank(message = "Tracker IMEI is required")
    @Size(min = 10, max = 20, message = "Tracker IMEI must be between 10 and 20 characters")
    private String trackerImei;

    @Size(max = 50, message = "Driver ID cannot exceed 50 characters")
    private String driverId;

    @Size(max = 100, message = "Driver name cannot exceed 100 characters")
    private String driverName;

    @NotBlank(message = "Model is required")
    @Size(min = 1, max = 50, message = "Model must be between 1 and 50 characters")
    private String model;

    @NotNull(message = "Year is required")
    @Min(value = 1900, message = "Year must be at least 1900")
    @Max(value = 2100, message = "Year cannot exceed 2100")
    private Integer year;

    @NotNull(message = "Capacity is required")
    @Min(value = 1, message = "Capacity must be at least 1")
    @Max(value = 200, message = "Capacity cannot exceed 200")
    private Integer capacity;

    @NotBlank(message = "Status is required")
    private String status;

    private Long routeId;
    private String routeName;

    // Default constructor
    public RegisteredBusCreateDTO() {}

    // Getters and Setters
    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
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
}