package com.backend.onebus.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "trackers")
public class Tracker {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "IMEI number is required")
    @Size(min = 10, max = 20, message = "IMEI must be between 10 and 20 characters")
    @Column(name = "imei", nullable = false, unique = true, length = 20)
    private String imei;

    @NotBlank(message = "Brand is required")
    @Size(max = 50, message = "Brand cannot exceed 50 characters")
    @Column(name = "brand", nullable = false, length = 50)
    private String brand;

    @NotBlank(message = "Model is required")
    @Size(max = 50, message = "Model cannot exceed 50 characters")
    @Column(name = "model", nullable = false, length = 50)
    private String model;

    @JsonFormat(pattern = "yyyy-MM-dd")
    @Column(name = "purchase_date")
    private LocalDate purchaseDate;

    @NotNull(message = "Status is required")
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TrackerStatus status = TrackerStatus.AVAILABLE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private BusCompany company;

    @OneToOne(mappedBy = "tracker", fetch = FetchType.LAZY)
    private RegisteredBus assignedBus;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum TrackerStatus {
        AVAILABLE,      // Not assigned to any bus
        IN_USE,         // Currently assigned to a bus
        MAINTENANCE,    // Under maintenance
        DAMAGED,        // Damaged/non-functional
        RETIRED         // No longer in use
    }

    // Default constructor
    public Tracker() {}

    // Constructor with essential fields
    public Tracker(String imei, String brand, String model, BusCompany company) {
        this.imei = imei;
        this.brand = brand;
        this.model = model;
        this.company = company;
        this.status = TrackerStatus.AVAILABLE;
    }

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

    public TrackerStatus getStatus() {
        return status;
    }

    public void setStatus(TrackerStatus status) {
        this.status = status;
    }

    public BusCompany getCompany() {
        return company;
    }

    public void setCompany(BusCompany company) {
        this.company = company;
    }

    public RegisteredBus getAssignedBus() {
        return assignedBus;
    }

    public void setAssignedBus(RegisteredBus assignedBus) {
        this.assignedBus = assignedBus;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
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

    @Override
    public String toString() {
        return "Tracker{" +
                "id=" + id +
                ", imei='" + imei + '\'' +
                ", brand='" + brand + '\'' +
                ", model='" + model + '\'' +
                ", status=" + status +
                '}';
    }
}
