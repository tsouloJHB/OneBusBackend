package com.backend.onebus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

@Schema(description = "Data Transfer Object for updating route stop information")
public class RouteStopUpdateDTO {

    @Schema(description = "ID of the route stop (null for new stops)", example = "58")
    private Long id;

    @Schema(description = "Latitude coordinate of the stop", example = "-26.20282", required = true)
    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @Schema(description = "Longitude coordinate of the stop", example = "28.04011", required = true)
    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;

    @Schema(description = "Address or name of the stop", example = "Library Gardens Bus Station")
    @Size(max = 255, message = "Address must not exceed 255 characters")
    private String address;

    @Schema(description = "Index of the bus stop in the route (auto-managed for conflicts)", example = "2")
    private Integer busStopIndex;

    @Schema(description = "Direction of travel at this stop", example = "Southbound")
    @Size(max = 50, message = "Direction must not exceed 50 characters")
    private String direction;

    @Schema(description = "Type of the stop", example = "Bus station")
    @Size(max = 50, message = "Type must not exceed 50 characters")
    private String type;

    @Schema(description = "Index for northbound direction", example = "null")
    private Integer northboundIndex;

    @Schema(description = "Index for southbound direction", example = "null")
    private Integer southboundIndex;

    // Constructors
    public RouteStopUpdateDTO() {}

    public RouteStopUpdateDTO(Long id, Double latitude, Double longitude, String address, 
                             Integer busStopIndex, String direction, String type, 
                             Integer northboundIndex, Integer southboundIndex) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.address = address;
        this.busStopIndex = busStopIndex;
        this.direction = direction;
        this.type = type;
        this.northboundIndex = northboundIndex;
        this.southboundIndex = southboundIndex;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Integer getBusStopIndex() {
        return busStopIndex;
    }

    public void setBusStopIndex(Integer busStopIndex) {
        this.busStopIndex = busStopIndex;
    }

    public String getDirection() {
        return direction;
    }

    public void setDirection(String direction) {
        this.direction = direction;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Integer getNorthboundIndex() {
        return northboundIndex;
    }

    public void setNorthboundIndex(Integer northboundIndex) {
        this.northboundIndex = northboundIndex;
    }

    public Integer getSouthboundIndex() {
        return southboundIndex;
    }

    public void setSouthboundIndex(Integer southboundIndex) {
        this.southboundIndex = southboundIndex;
    }

    @Override
    public String toString() {
        return "RouteStopUpdateDTO{" +
                "id=" + id +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", address='" + address + '\'' +
                ", busStopIndex=" + busStopIndex +
                ", direction='" + direction + '\'' +
                ", type='" + type + '\'' +
                ", northboundIndex=" + northboundIndex +
                ", southboundIndex=" + southboundIndex +
                '}';
    }
}
