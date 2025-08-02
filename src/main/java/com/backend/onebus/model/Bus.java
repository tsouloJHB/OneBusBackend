package com.backend.onebus.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "buses")
@Data
public class Bus {
    @Id
    private String busId;
    private String trackerImei;
    private String busNumber;
    private String route;
    
    // Change from String to BusCompany entity relationship
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bus_company_id", nullable = true)
    @JsonIgnore
    private BusCompany busCompany;
    
    // Keep the old busCompany field for backward compatibility (can be removed later)
    @Column(name = "bus_company_name")
    private String busCompanyName;
    
    private String driverId;
    private String driverName;

    // Explicit getters and setters to ensure compatibility
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

    public String getBusNumber() {
        return busNumber;
    }

    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public BusCompany getBusCompany() {
        return busCompany;
    }

    public void setBusCompany(BusCompany busCompany) {
        this.busCompany = busCompany;
    }
    
    public String getBusCompanyName() {
        return busCompanyName;
    }

    public void setBusCompanyName(String busCompanyName) {
        this.busCompanyName = busCompanyName;
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
}