package com.backend.onebus.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

@Schema(description = "DTO for creating a new route with direction support")
public class RouteCreateDTO {

    @NotBlank(message = "Company is required")
    @Size(max = 100, message = "Company name must not exceed 100 characters")
    @Schema(description = "Bus company name", example = "SimulatedCo", required = true)
    private String company;

    @NotBlank(message = "Bus number is required")
    @Size(max = 20, message = "Bus number must not exceed 20 characters")
    @Schema(description = "Bus route number", example = "C5", required = true)
    private String busNumber;

    @NotBlank(message = "Route name is required")
    @Size(max = 200, message = "Route name must not exceed 200 characters")
    @Schema(description = "Route name", example = "C5 Working Test", required = true)
    private String routeName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Route description", example = "Testing after fixing H2 scope issue")
    private String description;

    @NotBlank(message = "Direction is required")
    @Pattern(regexp = "^(Northbound|Southbound|Eastbound|Westbound|Bidirectional)$", 
             message = "Direction must be one of: Northbound, Southbound, Eastbound, Westbound, Bidirectional")
    @Schema(description = "Route direction", example = "Northbound", required = true, 
            allowableValues = {"Northbound", "Southbound", "Eastbound", "Westbound", "Bidirectional"})
    private String direction;

    @Size(max = 255, message = "Start point must not exceed 255 characters")
    @Schema(description = "Optional starting location name", example = "Johannesburg CBD")
    private String startPoint;

    @Size(max = 255, message = "End point must not exceed 255 characters")
    @Schema(description = "Optional ending location name", example = "Sandton City")
    private String endPoint;

    @Schema(description = "Route active status", example = "true")
    private Boolean active = true;

    @Schema(description = "Optional list of stops to create with the route (includes busStopIndex)")
    private List<RouteStopUpdateDTO> stops;

    // Constructors
    public RouteCreateDTO() {}

    public RouteCreateDTO(String company, String busNumber, String routeName, String direction) {
        this.company = company;
        this.busNumber = busNumber;
        this.routeName = routeName;
        this.direction = direction;
        this.active = true;
    }

    public List<RouteStopUpdateDTO> getStops() { return stops; }
    public void setStops(List<RouteStopUpdateDTO> stops) { this.stops = stops; }

    // Getters and Setters
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }

    public String getBusNumber() { return busNumber; }
    public void setBusNumber(String busNumber) { this.busNumber = busNumber; }

    public String getRouteName() { return routeName; }
    public void setRouteName(String routeName) { this.routeName = routeName; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDirection() { return direction; }
    public void setDirection(String direction) { this.direction = direction; }

    public String getStartPoint() { return startPoint; }
    public void setStartPoint(String startPoint) { this.startPoint = startPoint; }

    public String getEndPoint() { return endPoint; }
    public void setEndPoint(String endPoint) { this.endPoint = endPoint; }

    public Boolean getActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    @Override
    public String toString() {
        return "RouteCreateDTO{" +
                "company='" + company + '\'' +
                ", busNumber='" + busNumber + '\'' +
                ", routeName='" + routeName + '\'' +
                ", direction='" + direction + '\'' +
                ", startPoint='" + startPoint + '\'' +
                ", endPoint='" + endPoint + '\'' +
                ", active=" + active +
                '}';
    }
}
