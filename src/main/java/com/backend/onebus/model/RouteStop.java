package com.backend.onebus.model;

import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "route_stops")
@Data
public class RouteStop implements java.io.Serializable {
    private static final long serialVersionUID = 1L;
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id")
    @JsonIgnore
    private Route route;
    
    private double latitude;
    private double longitude;
    private String address;
    private Integer busStopIndex;
    private String direction; // "Northbound", "Southbound", "bidirectional"
    private String type; // "Bus stop", "Bus station"
    private Integer northboundIndex; // For bidirectional stops
    private Integer southboundIndex; // For bidirectional stops
    
    // Explicit getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    
    public Route getRoute() { return route; }
    public void setRoute(Route route) { this.route = route; }
    
    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }
    
    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
    
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    
    public Integer getBusStopIndex() { return busStopIndex; }
    public void setBusStopIndex(Integer busStopIndex) { this.busStopIndex = busStopIndex; }
    
    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public Integer getNorthboundIndex() { return northboundIndex; }
    public void setNorthboundIndex(Integer northboundIndex) { this.northboundIndex = northboundIndex; }
    
    public Integer getSouthboundIndex() { return southboundIndex; }
    public void setSouthboundIndex(Integer southboundIndex) { this.southboundIndex = southboundIndex; }
} 