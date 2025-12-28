package com.backend.onebus.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "drivers")
public class Driver {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Driver ID is required")
    @Size(max = 50, message = "Driver ID cannot exceed 50 characters")
    @Column(name = "driver_id", unique = true, nullable = false, length = 50)
    private String driverId;

    @NotBlank(message = "Full name is required")
    @Size(max = 100, message = "Full name cannot exceed 100 characters")
    @Column(name = "full_name", nullable = false, length = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be valid")
    @Column(unique = true, nullable = false)
    private String email;

    @NotBlank(message = "Password is required")
    @Column(nullable = false)
    private String password;

    @Pattern(regexp = "^[0-9]{10,15}$", message = "Phone number must be 10-15 digits")
    @Column(name = "phone_number", length = 15)
    private String phoneNumber;

    @Size(max = 50, message = "License number cannot exceed 50 characters")
    @Column(name = "license_number", length = 50)
    private String licenseNumber;

    @Column(name = "license_expiry_date")
    private LocalDateTime licenseExpiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DriverStatus status = DriverStatus.ACTIVE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private BusCompany company;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "last_login")
    private LocalDateTime lastLogin;

    @Column(name = "is_registered_by_admin")
    private Boolean isRegisteredByAdmin = false;

    @Column(name = "on_duty")
    private Boolean onDuty = false;

    @Column(name = "currently_assigned_bus_id")
    private Long currentlyAssignedBusId;

    @Column(name = "currently_assigned_bus_number", length = 50)
    private String currentlyAssignedBusNumber;

    @Column(name = "last_assigned_bus_id")
    private Long lastAssignedBusId;

    @Column(name = "last_assigned_bus_number", length = 50)
    private String lastAssignedBusNumber;

    @Column(name = "assigned_route_id")
    private Long assignedRouteId;

    @Column(name = "assigned_route_name", length = 100)
    private String assignedRouteName;

    @Column(name = "shift_start_time")
    private LocalDateTime shiftStartTime;

    @Column(name = "shift_end_time")
    private LocalDateTime shiftEndTime;

    @Column(name = "total_hours_worked")
    private Double totalHoursWorked = 0.0;

    public enum DriverStatus {
        ACTIVE,
        INACTIVE,
        SUSPENDED,
        ON_LEAVE
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
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

    public DriverStatus getStatus() {
        return status;
    }

    public void setStatus(DriverStatus status) {
        this.status = status;
    }

    public BusCompany getCompany() {
        return company;
    }

    public void setCompany(BusCompany company) {
        this.company = company;
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
}
