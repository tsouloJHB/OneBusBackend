package com.backend.onebus.dto;

import com.backend.onebus.model.Tracker;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

public class TrackerDTO {

    private Long id;

    @NotBlank(message = "IMEI number is required")
    @Size(min = 10, max = 20, message = "IMEI must be between 10 and 20 characters")
    private String imei;

    @NotBlank(message = "Brand is required")
    @Size(max = 50, message = "Brand cannot exceed 50 characters")
    private String brand;

    @NotBlank(message = "Model is required")
    @Size(max = 50, message = "Model cannot exceed 50 characters")
    private String model;

    private LocalDate purchaseDate;

    @NotNull(message = "Status is required")
    private Tracker.TrackerStatus status;

    private Long companyId;
    private String companyName;

    private Long assignedBusId;
    private String assignedBusNumber;

    private String notes;

    // Default constructor
    public TrackerDTO() {}

    // Constructor from entity
    public TrackerDTO(Tracker tracker) {
        this.id = tracker.getId();
        this.imei = tracker.getImei();
        this.brand = tracker.getBrand();
        this.model = tracker.getModel();
        this.purchaseDate = tracker.getPurchaseDate();
        this.status = tracker.getStatus();
        this.notes = tracker.getNotes();
        
        if (tracker.getCompany() != null) {
            this.companyId = tracker.getCompany().getId();
            this.companyName = tracker.getCompany().getName();
        }
        
        if (tracker.getAssignedBus() != null) {
            this.assignedBusId = tracker.getAssignedBus().getId();
            this.assignedBusNumber = tracker.getAssignedBus().getBusNumber();
        }
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getImei() {
        return imei;
    }

    public void setImei(String imei) {
        this.imei = imei;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public Tracker.TrackerStatus getStatus() {
        return status;
    }

    public void setStatus(Tracker.TrackerStatus status) {
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

    public Long getAssignedBusId() {
        return assignedBusId;
    }

    public void setAssignedBusId(Long assignedBusId) {
        this.assignedBusId = assignedBusId;
    }

    public String getAssignedBusNumber() {
        return assignedBusNumber;
    }

    public void setAssignedBusNumber(String assignedBusNumber) {
        this.assignedBusNumber = assignedBusNumber;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
