package com.backend.onebus.model;

public class BusStop {
    private double latitude;
    private double longitude;
    private String address;
    private Integer busStopIndex;
    private String direction; // e.g., "Northbound", "Southbound", "bidirectional"
    private String type; // e.g., "Bus stop", "Bus station"
    // Optional: for bidirectional stops
    private Integer northboundIndex;
    private Integer southboundIndex;

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