package com.backend.onebus.dto;

import com.backend.onebus.model.Driver;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class DriverDTO {
    
    private Long id;
    
    @NotBlank(message = "Driver ID is required")
    @Size(max = 50, message = "Driver ID cannot exceed 50 characters")
    private String driverId;
    
    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    private String fullName;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    private String email;
    
    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    private String phoneNumber;
    
    @Size(max = 50, message = "License number cannot exceed 50 characters")
    private String licenseNumber;
    
    private LocalDateTime licenseExpiryDate;
    
    private Driver.DriverStatus status;
    
    private Long companyId;
    
    private String companyName;

    private Boolean onDuty;

    private Long currentlyAssignedBusId;

    private String currentlyAssignedBusNumber;

    private Long lastAssignedBusId;

    private String lastAssignedBusNumber;

    private Long assignedRouteId;

    private String assignedRouteName;

    private LocalDateTime shiftStartTime;

    private LocalDateTime shiftEndTime;

    private Double totalHoursWorked;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
    
    private LocalDateTime lastLogin;
    
    private Boolean isRegisteredByAdmin;
    
    // Constructors
    public DriverDTO() {}
    
    public DriverDTO(Driver driver) {
        this.id = driver.getId();
        this.driverId = driver.getDriverId();
        this.fullName = driver.getFullName();
        this.email = driver.getEmail();
        this.phoneNumber = driver.getPhoneNumber();
        this.licenseNumber = driver.getLicenseNumber();
        this.licenseExpiryDate = driver.getLicenseExpiryDate();
        this.status = driver.getStatus();
        this.companyId = driver.getCompany() != null ? driver.getCompany().getId() : null;
        this.companyName = driver.getCompany() != null ? driver.getCompany().getName() : null;
        this.onDuty = driver.getOnDuty() != null ? driver.getOnDuty() : false;
        this.currentlyAssignedBusId = driver.getCurrentlyAssignedBusId();
        this.currentlyAssignedBusNumber = driver.getCurrentlyAssignedBusNumber();
        this.lastAssignedBusId = driver.getLastAssignedBusId();
        this.lastAssignedBusNumber = driver.getLastAssignedBusNumber();
        this.assignedRouteId = driver.getAssignedRouteId();
        this.assignedRouteName = driver.getAssignedRouteName();
        this.shiftStartTime = driver.getShiftStartTime();
        this.shiftEndTime = driver.getShiftEndTime();
        this.totalHoursWorked = driver.getTotalHoursWorked();
        this.createdAt = driver.getCreatedAt();
        this.updatedAt = driver.getUpdatedAt();
        this.lastLogin = driver.getLastLogin();
        this.isRegisteredByAdmin = driver.getIsRegisteredByAdmin();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getDriverId() {
        return driverId;
    }
    
    public void setDriverId(String driverId) {
        this.driverId = driverId;
    }
    
    public String getFullName() {
        return fullName;
    }
    
    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhoneNumber() {
        return phoneNumber;
    }
    
    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }
    
    public String getLicenseNumber() {
        return licenseNumber;
    }
    
    public void setLicenseNumber(String licenseNumber) {
        this.licenseNumber = licenseNumber;
    }
    
    public LocalDateTime getLicenseExpiryDate() {
        return licenseExpiryDate;
    }
    
    public void setLicenseExpiryDate(LocalDateTime licenseExpiryDate) {
        this.licenseExpiryDate = licenseExpiryDate;
    }
    
    public Driver.DriverStatus getStatus() {
        return status;
    }
    
    public void setStatus(Driver.DriverStatus status) {
        this.status = status;
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

    public Boolean getOnDuty() {
        return onDuty;
    }

    public void setOnDuty(Boolean onDuty) {
        this.onDuty = onDuty;
    }

    public Long getCurrentlyAssignedBusId() {
        return currentlyAssignedBusId;
    }

    public void setCurrentlyAssignedBusId(Long currentlyAssignedBusId) {
        this.currentlyAssignedBusId = currentlyAssignedBusId;
    }

    public String getCurrentlyAssignedBusNumber() {
        return currentlyAssignedBusNumber;
    }

    public void setCurrentlyAssignedBusNumber(String currentlyAssignedBusNumber) {
        this.currentlyAssignedBusNumber = currentlyAssignedBusNumber;
    }

    public Long getLastAssignedBusId() {
        return lastAssignedBusId;
    }

    public void setLastAssignedBusId(Long lastAssignedBusId) {
        this.lastAssignedBusId = lastAssignedBusId;
    }

    public String getLastAssignedBusNumber() {
        return lastAssignedBusNumber;
    }

    public void setLastAssignedBusNumber(String lastAssignedBusNumber) {
        this.lastAssignedBusNumber = lastAssignedBusNumber;
    }

    public Long getAssignedRouteId() {
        return assignedRouteId;
    }

    public void setAssignedRouteId(Long assignedRouteId) {
        this.assignedRouteId = assignedRouteId;
    }

    public String getAssignedRouteName() {
        return assignedRouteName;
    }

    public void setAssignedRouteName(String assignedRouteName) {
        this.assignedRouteName = assignedRouteName;
    }

    public LocalDateTime getShiftStartTime() {
        return shiftStartTime;
    }

    public void setShiftStartTime(LocalDateTime shiftStartTime) {
        this.shiftStartTime = shiftStartTime;
    }

    public LocalDateTime getShiftEndTime() {
        return shiftEndTime;
    }

    public void setShiftEndTime(LocalDateTime shiftEndTime) {
        this.shiftEndTime = shiftEndTime;
    }

    public Double getTotalHoursWorked() {
        return totalHoursWorked;
    }

    public void setTotalHoursWorked(Double totalHoursWorked) {
        this.totalHoursWorked = totalHoursWorked;
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
    
    public LocalDateTime getLastLogin() {
        return lastLogin;
    }
    
    public void setLastLogin(LocalDateTime lastLogin) {
        this.lastLogin = lastLogin;
    }
    
    public Boolean getIsRegisteredByAdmin() {
        return isRegisteredByAdmin;
    }
    
    public void setIsRegisteredByAdmin(Boolean isRegisteredByAdmin) {
        this.isRegisteredByAdmin = isRegisteredByAdmin;
    }
}
