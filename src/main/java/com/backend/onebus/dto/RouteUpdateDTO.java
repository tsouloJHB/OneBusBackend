package com.backend.onebus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "Data Transfer Object for updating route information")
public class RouteUpdateDTO {
    
    @Schema(description = "Name of the bus company", example = "Rea Vaya")
    @Size(max = 100, message = "Company name must be less than 100 characters")
    private String company;
    
    @Schema(description = "Bus route number", example = "C5")
    @Size(max = 20, message = "Bus number must be less than 20 characters")
    private String busNumber;
    
    @Schema(description = "Human-readable name of the route", example = "Thokoza to Johannesburg CBD")
    @Size(max = 200, message = "Route name must be less than 200 characters")
    private String routeName;
    
    @Schema(description = "Detailed description of the route", example = "Express route connecting Thokoza to Johannesburg CBD via Ellis Park")
    @Size(max = 500, message = "Description must be less than 500 characters")
    private String description;
    
    @Schema(description = "Whether the route is currently active", example = "true")
    private Boolean active;
    
    @Schema(description = "List of route stops to add or update")
    @Valid
    private List<RouteStopUpdateDTO> stops;
    
    // Constructors
    public RouteUpdateDTO() {}
    
    public RouteUpdateDTO(String company, String busNumber, String routeName, String description, Boolean active, List<RouteStopUpdateDTO> stops) {
        this.company = company;
        this.busNumber = busNumber;
        this.routeName = routeName;
        this.description = description;
        this.active = active;
        this.stops = stops;
    }
    
    // Getters and Setters
    public String getCompany() {
        return company;
    }
    
    public void setCompany(String company) {
        this.company = company;
    }
    
    public String getBusNumber() {
        return busNumber;
    }
    
    public void setBusNumber(String busNumber) {
        this.busNumber = busNumber;
    }
    
    public String getRouteName() {
        return routeName;
    }
    
    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public Boolean getActive() {
        return active;
    }
    
    public void setActive(Boolean active) {
        this.active = active;
    }
    
    public List<RouteStopUpdateDTO> getStops() {
        return stops;
    }
    
    public void setStops(List<RouteStopUpdateDTO> stops) {
        this.stops = stops;
    }
    
    @Override
    public String toString() {
        return "RouteUpdateDTO{" +
                "company='" + company + '\'' +
                ", busNumber='" + busNumber + '\'' +
                ", routeName='" + routeName + '\'' +
                ", description='" + description + '\'' +
                ", active=" + active +
                ", stops=" + (stops != null ? stops.size() + " stops" : "null") +
                '}';
    }
}
