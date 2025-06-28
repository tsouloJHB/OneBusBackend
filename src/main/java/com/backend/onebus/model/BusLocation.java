package com.backend.onebus.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "bus_locations")
@Data
public class BusLocation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String busId;
    private String trackerImei;
    private String timestamp;
    
    // Temporarily remove geometry column to avoid PostGIS issues
    // @Column(columnDefinition = "geometry(Point,4326)")
    // private Point location; // For PostgreSQL PostGIS

    private String location;
    
    private double lat;
    private double lon;
    private double speedKmh;
    private double headingDegrees;
    private String headingCardinal;
    private String tripDirection;
    private String busNumber;
    private String busDriverId;
    private String busDriver;
    private String busCompany;
    private long lastSavedTimestamp; // For tracking 30-min saves

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

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public double getSpeedKmh() {
        return speedKmh;
    }

    public void setSpeedKmh(double speedKmh) {
        this.speedKmh = speedKmh;
    }

    public double getHeadingDegrees() {
        return headingDegrees;
    }

    public void setHeadingDegrees(double headingDegrees) {
        this.headingDegrees = headingDegrees;
    }

    public String getHeadingCardinal() {
        return headingCardinal;
    }

    public void setHeadingCardinal(String headingCardinal) {
        this.headingCardinal = headingCardinal;
    }

    public String getTripDirection() {
        return tripDirection;
    }

    public void setTripDirection(String tripDirection) {
        this.tripDirection = tripDirection;
    }

    public String getBusNumber() {
        return busNumber;
    }

    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }

    public String getBusDriverId() {
        return busDriverId;
    }

    public void setBusDriverId(String busDriverId) {
        this.busDriverId = busDriverId;
    }

    public String getBusDriver() {
        return busDriver;
    }

    public void setBusDriver(String busDriver) {
        this.busDriver = busDriver;
    }

    public String getBusCompany() {
        return busCompany;
    }

    public void setBusCompany(String busCompany) {
        this.busCompany = busCompany;
    }

    public long getLastSavedTimestamp() {
        return lastSavedTimestamp;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public void setLastSavedTimestamp(long lastSavedTimestamp) {
        this.lastSavedTimestamp = lastSavedTimestamp;
    }
}