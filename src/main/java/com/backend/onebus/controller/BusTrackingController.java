package com.backend.onebus.controller;

import com.backend.onebus.dto.RouteUpdateDTO;
import com.backend.onebus.dto.RouteStopUpdateDTO;
import com.backend.onebus.dto.RouteCreateDTO;
import com.backend.onebus.dto.RegisteredBusCreateDTO;
import com.backend.onebus.dto.RegisteredBusResponseDTO;
import com.backend.onebus.dto.ActiveBusDTO;
import com.backend.onebus.model.Bus;
import com.backend.onebus.model.BusLocation;
import com.backend.onebus.model.Route;
import com.backend.onebus.model.RouteStop;
import com.backend.onebus.model.FullRoute;
import com.backend.onebus.repository.BusRepository;
import com.backend.onebus.repository.BusLocationRepository;
import com.backend.onebus.repository.RouteRepository;
import com.backend.onebus.repository.RouteStopRepository;
import com.backend.onebus.repository.FullRouteRepository;
import com.backend.onebus.service.BusTrackingService;
import com.backend.onebus.service.BusSelectionService;
import com.backend.onebus.service.RegisteredBusService;
import com.backend.onebus.service.DashboardStatsService;
import com.backend.onebus.service.MetricsService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.transaction.Transactional;
import java.util.Optional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
@Tag(name = "Bus Tracking", description = "API endpoints for bus tracking and route management")
public class BusTrackingController {
    @Autowired
    private BusTrackingService trackingService;
    @Autowired
    private BusRepository busRepository;
    @Autowired
    private RouteRepository routeRepository;
    @Autowired
    private RouteStopRepository routeStopRepository;
    @Autowired
    private FullRouteRepository fullRouteRepository;
    @Autowired
    private BusLocationRepository busLocationRepository;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private BusSelectionService busSelectionService;
    @Autowired
    private RegisteredBusService registeredBusService;
    @Autowired
    private DashboardStatsService dashboardStatsService;
    @Autowired
    private MetricsService metricsService;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BusTrackingController.class);

    @PostMapping("/tracker/payload")
    @Operation(summary = "Receive tracker payload", description = "Receives GPS tracking data from bus tracking devices")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payload received successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid payload format")
    })
    public ResponseEntity<?> receiveTrackerPayload(@RequestBody BusLocation payload) {
        long startTime = System.currentTimeMillis();
        logger.info("Received tracker payload: {}", payload);
        
        // Record metrics
        metricsService.recordTrackerPayloadReceived(payload.getBusId(), payload.getTrackerImei());
        
        trackingService.processTrackerPayload(payload);
        
        long processingTime = System.currentTimeMillis() - startTime;
        metricsService.recordBusLocationProcessed(payload.getBusId(), processingTime);
        
        return ResponseEntity.ok().build();
    }

    @GetMapping("/buses/nearest")
    @Operation(summary = "Find nearest bus", description = "Find the nearest bus to a given location for a specific trip direction")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Nearest bus found", 
                     content = @Content(schema = @Schema(implementation = BusLocation.class))),
        @ApiResponse(responseCode = "404", description = "No bus found")
    })
    public ResponseEntity<BusLocation> getNearestBus(
            @Parameter(description = "Latitude of the location", required = true)
            @RequestParam double lat,
            @Parameter(description = "Longitude of the location", required = true)
            @RequestParam double lon,
            @Parameter(description = "Trip direction (e.g., 'Northbound', 'Southbound')", required = true)
            @RequestParam String tripDirection) {
        BusLocation nearestBus = trackingService.findNearestBus(lat, lon, tripDirection);
        return nearestBus != null ? ResponseEntity.ok(nearestBus) : ResponseEntity.notFound().build();
    }

    @PutMapping("/buses/{busId}")
    @Operation(summary = "Update bus details", description = "Update the details of an existing bus")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid bus data"),
        @ApiResponse(responseCode = "404", description = "Bus not found")
    })
    public ResponseEntity<?> updateBusDetails(
            @Parameter(description = "ID of the bus to update", required = true)
            @PathVariable String busId, 
            @RequestBody Bus bus) {
        bus.setBusId(busId);
        trackingService.updateBusDetails(bus);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/clear")
    @Operation(summary = "Clear tracking data", description = "Clear all bus tracking data from the system")
    @ApiResponse(responseCode = "200", description = "Tracking data cleared successfully")
    public ResponseEntity<?> clearTrackingData() {
        trackingService.clearTrackingData();
        return ResponseEntity.ok().build();
    }

    @PutMapping("/buses/{busId}/status")
    @Operation(summary = "Update bus operational status", 
               description = "Update the operational status of a bus. Only buses with 'active' status will have their GPS data processed and broadcast.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus status updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid status value"),
        @ApiResponse(responseCode = "404", description = "Bus not found")
    })
    public ResponseEntity<?> updateBusStatus(
            @Parameter(description = "ID of the bus", required = true)
            @PathVariable String busId,
            @Parameter(description = "New operational status", required = true)
            @RequestBody Map<String, String> statusUpdate) {
        
        String newStatus = statusUpdate.get("operationalStatus");
        if (newStatus == null || newStatus.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "operationalStatus is required"));
        }
        
        // Validate status value
        if (!Arrays.asList("active", "inactive", "maintenance", "retired").contains(newStatus.toLowerCase())) {
            return ResponseEntity.badRequest()
                .body(Map.of("error", "Invalid status. Must be one of: active, inactive, maintenance, retired"));
        }
        
        boolean updated = trackingService.updateBusStatus(busId, newStatus);
        if (updated) {
            logger.info("Bus {} operational status updated to: {}", busId, newStatus);
            return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Bus operational status updated to '" + newStatus + "'",
                "busId", busId,
                "newStatus", newStatus
            ));
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/buses/{busNumber}/location")
    @Operation(summary = "Get bus location", description = "Get the current location of a specific bus")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus location found", 
                     content = @Content(schema = @Schema(implementation = BusLocation.class))),
        @ApiResponse(responseCode = "404", description = "Bus location not found")
    })
    public ResponseEntity<BusLocation> getBusLocation(
            @Parameter(description = "Bus number", required = true)
            @PathVariable String busNumber,
            @Parameter(description = "Direction of travel", required = true)
            @RequestParam String direction) {
        BusLocation location = trackingService.getBusLocation(busNumber, direction);
        return location != null ? ResponseEntity.ok(location) : ResponseEntity.notFound().build();
    }

    @GetMapping("/buses/active")
    @Operation(summary = "Get active buses", description = "Retrieves buses with recent location updates and details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Active buses retrieved successfully"),
        @ApiResponse(responseCode = "500", description = "Failed to retrieve active buses")
    })
    public ResponseEntity<List<ActiveBusDTO>> getActiveBuses(
            @RequestParam(required = false) String companyId,
            @RequestParam(required = false) String busNumber) {
        try {
            long timeThresholdMillis = System.currentTimeMillis() - (10 * 60 * 1000); // 10 minutes

                List<BusLocation> activeLocations = (companyId != null && !companyId.isEmpty())
                    ? busLocationRepository.findActiveBusesByCompany(companyId, timeThresholdMillis)
                    : busLocationRepository.findActiveBuses(timeThresholdMillis);

                if (busNumber != null && !busNumber.isEmpty()) {
                activeLocations = activeLocations.stream()
                    .filter(loc -> busNumber.equalsIgnoreCase(loc.getBusNumber()))
                    .toList();
                }

            List<ActiveBusDTO> response = new ArrayList<>();

            for (BusLocation location : activeLocations) {
                // Look up bus for richer metadata
                Bus busEntity = busRepository.findByTrackerImei(location.getTrackerImei());

                // Look up route (best effort)
                Route routeEntity = routeRepository
                        .findByCompanyAndBusNumber(location.getBusCompany(), location.getBusNumber())
                        .orElse(null);

                ActiveBusDTO.BusInfo busInfo = new ActiveBusDTO.BusInfo(
                        location.getBusId(),
                        location.getBusNumber(),
                        location.getTrackerImei(),
                        location.getBusDriverId(),
                        location.getBusDriver()
                );

                ActiveBusDTO.RouteInfo routeInfo = null;
                if (routeEntity != null) {
                    routeInfo = new ActiveBusDTO.RouteInfo(
                            routeEntity.getId(),
                            routeEntity.getRouteName(),
                            routeEntity.getCompany(),
                            routeEntity.getBusNumber(),
                            routeEntity.getDescription(),
                            routeEntity.getDirection(),
                            routeEntity.getStartPoint(),
                            routeEntity.getEndPoint(),
                            routeEntity.isActive()
                    );
                }

                ActiveBusDTO.LocationInfo locationInfo = new ActiveBusDTO.LocationInfo(
                        location.getLat(),
                        location.getLon()
                );

                // Look up current and last stop information
                ActiveBusDTO.StopInfo nextStopInfo = null;
                ActiveBusDTO.StopInfo lastStopInfo = null;
                
                if (routeEntity != null && location.getBusStopIndex() != null) {
                    // Get all stops for this route in the given direction
                    List<RouteStop> routeStops = routeStopRepository
                            .findByRouteIdAndDirectionOrderByBusStopIndex(routeEntity.getId(), location.getTripDirection());
                    
                    if (!routeStops.isEmpty()) {
                        int currentIndex = location.getBusStopIndex();
                        
                        // Find current/next stop
                        for (RouteStop stop : routeStops) {
                            if (stop.getBusStopIndex() != null && stop.getBusStopIndex() == currentIndex) {
                                nextStopInfo = new ActiveBusDTO.StopInfo(
                                        String.valueOf(stop.getId()),
                                        stop.getAddress(),
                                        stop.getLatitude(),
                                        stop.getLongitude(),
                                        stop.getBusStopIndex()
                                );
                                break;
                            }
                        }
                        
                        // Find last stop (one before current)
                        if (currentIndex > 0) {
                            for (RouteStop stop : routeStops) {
                                if (stop.getBusStopIndex() != null && stop.getBusStopIndex() == (currentIndex - 1)) {
                                    lastStopInfo = new ActiveBusDTO.StopInfo(
                                            String.valueOf(stop.getId()),
                                            stop.getAddress(),
                                            stop.getLatitude(),
                                            stop.getLongitude(),
                                            stop.getBusStopIndex()
                                    );
                                    break;
                                }
                            }
                        }
                    }
                }

                // Fallback if no route lookup
                if (nextStopInfo == null && location.getBusStopIndex() != null) {
                    nextStopInfo = new ActiveBusDTO.StopInfo(
                            null,
                            "Stop " + location.getBusStopIndex(),
                            null,
                            null,
                            location.getBusStopIndex()
                    );
                }

                String status = location.getSpeedKmh() < 1.0 ? "at_stop" : "on_route";

                ActiveBusDTO dto = new ActiveBusDTO(
                        String.valueOf(location.getId()),
                        busInfo,
                        routeInfo,
                        locationInfo,
                        nextStopInfo,
                        lastStopInfo,
                        status,
                        null, // estimatedArrival not computed
                        null, // passengerCount not tracked
                        location.getLastSavedTimestamp(),
                        location.getSpeedKmh(),
                        location.getHeadingDegrees(),
                        location.getHeadingCardinal(),
                        location.getBusCompany(),
                        location.getTripDirection()
                );

                response.add(dto);
            }

            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to retrieve active buses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/buses")
    @Operation(summary = "Get all buses", description = "Retrieve all buses from the database")
    @ApiResponse(responseCode = "200", description = "List of all buses retrieved successfully")
    public ResponseEntity<List<Bus>> getAllBuses() {
        List<Bus> buses = busRepository.findAll();
        return ResponseEntity.ok(buses);
    }

    @GetMapping("/buses/company/{busCompanyName}")
    @Operation(summary = "Get buses by company", description = "Retrieve all buses belonging to a specific company")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Buses retrieved successfully", 
                     content = @Content(schema = @Schema(implementation = Bus.class))),
        @ApiResponse(responseCode = "404", description = "No buses found for the specified company"),
        @ApiResponse(responseCode = "400", description = "Invalid company name parameter")
    })
    public ResponseEntity<?> getBusesByCompany(
            @Parameter(description = "Name of the bus company", required = true, example = "Rea Vaya")
            @PathVariable String busCompanyName,
            @Parameter(description = "Search type: exact, ignoreCase, or contains", example = "ignoreCase")
            @RequestParam(defaultValue = "ignoreCase") String searchType) {
        
        try {
            logger.info("Searching for buses by company: {} with search type: {}", busCompanyName, searchType);
            
            if (busCompanyName == null || busCompanyName.trim().isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Company name cannot be empty"
                ));
            }
            
            List<Bus> buses;
            
            switch (searchType.toLowerCase()) {
                case "exact":
                    buses = busRepository.findByBusCompanyName(busCompanyName);
                    break;
                case "contains":
                    buses = busRepository.findByBusCompanyNameContainingIgnoreCase(busCompanyName);
                    break;
                case "ignorecase":
                default:
                    buses = busRepository.findByBusCompanyNameIgnoreCase(busCompanyName);
                    break;
            }
            
            if (buses.isEmpty()) {
                logger.info("No buses found for company: {}", busCompanyName);
                return ResponseEntity.ok(Map.of(
                    "message", "No buses found for company: " + busCompanyName,
                    "company", busCompanyName,
                    "buses", buses,
                    "count", 0
                ));
            }
            
            logger.info("Found {} buses for company: {}", buses.size(), busCompanyName);
            return ResponseEntity.ok(Map.of(
                "message", "Buses retrieved successfully",
                "company", busCompanyName,
                "buses", buses,
                "count", buses.size(),
                "searchType", searchType
            ));
            
        } catch (Exception e) {
            logger.error("Error retrieving buses for company {}: {}", busCompanyName, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                "error", "Failed to retrieve buses for company: " + e.getMessage()
            ));
        }
    }

    @PostMapping("/buses")
    @Operation(summary = "Create bus", description = "Create a new bus record in the system")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus created successfully", 
                     content = @Content(schema = @Schema(implementation = Bus.class))),
        @ApiResponse(responseCode = "400", description = "Invalid bus data")
    })
    public ResponseEntity<Bus> createBus(@RequestBody Bus bus) {
        Bus saved = trackingService.saveBus(bus);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/routes")
    @Operation(summary = "Get all routes", description = "Retrieve all active bus routes")
    @ApiResponse(responseCode = "200", description = "List of routes retrieved successfully")
    public ResponseEntity<List<Route>> getAllRoutes() {
        List<Route> routes = routeRepository.findByActiveTrue();
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/routes/{routeId}")
    @Operation(summary = "Get route by ID", description = "Retrieve a specific route by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Route found", 
                     content = @Content(schema = @Schema(implementation = Route.class))),
        @ApiResponse(responseCode = "404", description = "Route not found")
    })
    public ResponseEntity<Route> getRoute(
            @Parameter(description = "ID of the route to retrieve", required = true)
            @PathVariable Long routeId) {
        Route route = routeRepository.findById(routeId).orElse(null);
        return route != null ? ResponseEntity.ok(route) : ResponseEntity.notFound().build();
    }

    @PostMapping("/routes")
    @Operation(summary = "Create a new route", description = "Create a new route with direction support")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Route created successfully", 
                     content = @Content(schema = @Schema(implementation = Route.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data or route already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createRoute(
            @Parameter(description = "Route creation data", required = true)
            @Valid @RequestBody RouteCreateDTO routeCreateDTO) {
        try {
            logger.info("Creating new route: {}", routeCreateDTO);
            
            // Check if route already exists with same company, busNumber, and direction
            Optional<Route> existingRoute = routeRepository.findByCompanyAndBusNumberAndDirection(
                routeCreateDTO.getCompany(), 
                routeCreateDTO.getBusNumber(), 
                routeCreateDTO.getDirection()
            );
            
            if (existingRoute.isPresent()) {
                logger.warn("Route already exists: company={}, busNumber={}, direction={}", 
                           routeCreateDTO.getCompany(), routeCreateDTO.getBusNumber(), routeCreateDTO.getDirection());
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "Route already exists with the same company, bus number, and direction",
                    "existingRouteId", existingRoute.get().getId()
                ));
            }
            
            // Create new route
            Route newRoute = new Route();
            newRoute.setCompany(routeCreateDTO.getCompany());
            newRoute.setBusNumber(routeCreateDTO.getBusNumber());
            newRoute.setRouteName(routeCreateDTO.getRouteName());
            newRoute.setDescription(routeCreateDTO.getDescription());
            newRoute.setDirection(routeCreateDTO.getDirection());
            newRoute.setStartPoint(routeCreateDTO.getStartPoint());
            newRoute.setEndPoint(routeCreateDTO.getEndPoint());
            newRoute.setActive(routeCreateDTO.getActive() != null ? routeCreateDTO.getActive() : true);
            
            // Save the route
            Route savedRoute = routeRepository.save(newRoute);
            logger.info("Route created successfully with ID: {}", savedRoute.getId());
            
            // Update dashboard stats
            dashboardStatsService.incrementRoutes();
            
            // If stops were provided in the creation DTO, process and save them
            List<Map<String, Object>> createdStops = new java.util.ArrayList<>();
            if (routeCreateDTO.getStops() != null && !routeCreateDTO.getStops().isEmpty()) {
                logger.info("Processing {} stops provided during route creation...", routeCreateDTO.getStops().size());
                for (RouteStopUpdateDTO stopDTO : routeCreateDTO.getStops()) {
                    // Validate coordinates for new stop
                    if (stopDTO.getLatitude() == null || stopDTO.getLongitude() == null) {
                        throw new IllegalArgumentException("New stops must have valid latitude and longitude coordinates");
                    }

                    // If a busStopIndex is provided, shift existing stops to avoid conflicts
                    if (stopDTO.getBusStopIndex() != null) {
                        handleBusStopIndexConflict(savedRoute.getId(), stopDTO.getBusStopIndex(), null, stopDTO.getDirection());
                    }

                    RouteStop routeStop = new RouteStop();
                    routeStop.setRoute(savedRoute);
                    routeStop.setLatitude(stopDTO.getLatitude());
                    routeStop.setLongitude(stopDTO.getLongitude());
                    if (stopDTO.getAddress() != null) routeStop.setAddress(stopDTO.getAddress());
                    if (stopDTO.getBusStopIndex() != null) routeStop.setBusStopIndex(stopDTO.getBusStopIndex());
                    if (stopDTO.getDirection() != null) routeStop.setDirection(stopDTO.getDirection());
                    if (stopDTO.getType() != null) routeStop.setType(stopDTO.getType());
                    if (stopDTO.getNorthboundIndex() != null) routeStop.setNorthboundIndex(stopDTO.getNorthboundIndex());
                    if (stopDTO.getSouthboundIndex() != null) routeStop.setSouthboundIndex(stopDTO.getSouthboundIndex());

                    RouteStop savedStop = routeStopRepository.save(routeStop);

                    createdStops.add(createSafeStopMap(savedStop));
                }
                logger.info("Created {} stops for new route ID {}", createdStops.size(), savedRoute.getId());
            }

            // Return the created route with a success message and any created stops
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Route created successfully",
                "route", Map.of(
                    "id", savedRoute.getId(),
                    "company", savedRoute.getCompany(),
                    "busNumber", savedRoute.getBusNumber(),
                    "routeName", savedRoute.getRouteName(),
                    "description", savedRoute.getDescription() != null ? savedRoute.getDescription() : "",
                    "direction", savedRoute.getDirection(),
                    "startPoint", savedRoute.getStartPoint() != null ? savedRoute.getStartPoint() : "",
                    "endPoint", savedRoute.getEndPoint() != null ? savedRoute.getEndPoint() : "",
                    "active", savedRoute.isActive()
                ),
                "stops", createdStops
            ));
            
        } catch (Exception e) {
            logger.error("Failed to create route: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create route: " + e.getMessage()));
        }
    }

    @PostMapping("/routes/import-json")
    @Operation(summary = "Import routes from JSON", description = "Import bus routes and stops from JSON data")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Routes imported successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid JSON data or import failed")
    })
    public ResponseEntity<?> importRoutesFromJson(@RequestBody JsonNode importData) {
        try {
            for (JsonNode routeNode : importData) {
                String company = routeNode.get("company").asText();
                String busNumber = routeNode.get("busNumber").asText();
                String routeName = routeNode.get("routeName").asText();

                // Create or update route
                Route route = routeRepository.findByCompanyAndBusNumber(company, busNumber).orElse(new Route());
                route.setCompany(company);
                route.setBusNumber(busNumber);
                route.setRouteName(routeName);
                route.setDescription(routeNode.has("description") ? routeNode.get("description").asText() : null);
                route.setActive(true);
                route = routeRepository.save(route);

                // Clear existing stops
                List<RouteStop> existingStops = routeStopRepository.findByRouteIdOrderByBusStopIndex(route.getId());
                routeStopRepository.deleteAll(existingStops);

                // Import stops
                JsonNode stopsArray = routeNode.get("stops");
                for (JsonNode stopNode : stopsArray) {
                    RouteStop routeStop = new RouteStop();
                    routeStop.setRoute(route);
                    routeStop.setLatitude(stopNode.get("coordinates").get("latitude").asDouble());
                    routeStop.setLongitude(stopNode.get("coordinates").get("longitude").asDouble());
                    routeStop.setAddress(stopNode.has("address") ? stopNode.get("address").asText() : null);
                    routeStop.setBusStopIndex(stopNode.has("bus_stop_index") ? stopNode.get("bus_stop_index").asInt() : null);
                    routeStop.setDirection(stopNode.has("direction") ? stopNode.get("direction").asText() : null);
                    routeStop.setType(stopNode.has("type") ? stopNode.get("type").asText() : null);

                    if (stopNode.has("bus_stop_indices")) {
                        JsonNode indices = stopNode.get("bus_stop_indices");
                        routeStop.setNorthboundIndex(indices.has("northbound") ? indices.get("northbound").asInt() : null);
                        routeStop.setSouthboundIndex(indices.has("southbound") ? indices.get("southbound").asInt() : null);
                    }

                    routeStopRepository.save(routeStop);
                }
            }

            // Clear route cache to force reload
            // routeStopsCache.clear(); // This line was not in the original file, so it's not added.

            return ResponseEntity.ok().body(Map.of("message", "Routes imported successfully"));
        } catch (Exception e) {
            logger.error("Failed to import routes: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PutMapping("/routes/{routeId}")
    @Operation(summary = "Update an existing route", description = "Update an existing route with the provided details")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Route updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Route not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> updateRoute(
            @Parameter(description = "ID of the route to update", required = true)
            @PathVariable Long routeId,
            @Parameter(description = "Updated route information", required = true)
            @Valid @RequestBody RouteUpdateDTO routeUpdateDTO) {
        try {
            logger.info("Starting route update for ID: {}", routeId);
            
            // Find the existing route
            Optional<Route> optionalRoute = routeRepository.findById(routeId);
            if (optionalRoute.isEmpty()) {
                logger.warn("Route not found: {}", routeId);
                return ResponseEntity.notFound().build();
            }
            
            logger.info("Route found, updating fields...");
            Route existingRoute = optionalRoute.get();
            
            // Update the route fields
            if (routeUpdateDTO.getCompany() != null) {
                existingRoute.setCompany(routeUpdateDTO.getCompany());
            }
            if (routeUpdateDTO.getBusNumber() != null) {
                existingRoute.setBusNumber(routeUpdateDTO.getBusNumber());
            }
            if (routeUpdateDTO.getRouteName() != null) {
                existingRoute.setRouteName(routeUpdateDTO.getRouteName());
            }
            if (routeUpdateDTO.getDescription() != null) {
                existingRoute.setDescription(routeUpdateDTO.getDescription());
            }
            if (routeUpdateDTO.getActive() != null) {
                existingRoute.setActive(routeUpdateDTO.getActive());
            }
            if (routeUpdateDTO.getStartPoint() != null) {
                existingRoute.setStartPoint(routeUpdateDTO.getStartPoint());
            }
            if (routeUpdateDTO.getEndPoint() != null) {
                existingRoute.setEndPoint(routeUpdateDTO.getEndPoint());
            }
            
            logger.info("Saving route...");
            // Save the updated route first
            Route updatedRoute = routeRepository.save(existingRoute);
            logger.info("Route saved successfully");
            
            // Handle route stops if provided
            if (routeUpdateDTO.getStops() != null && !routeUpdateDTO.getStops().isEmpty()) {
                logger.info("Processing route stops...");
                // Get existing stops
                List<RouteStop> existingStops = routeStopRepository.findByRouteIdOrderByBusStopIndex(routeId);
                
                // Process each stop in the update request
                for (RouteStopUpdateDTO stopDTO : routeUpdateDTO.getStops()) {
                    RouteStop routeStop;
                    
                    if (stopDTO.getId() != null) {
                        // Update existing stop
                        routeStop = existingStops.stream()
                                .filter(stop -> stop.getId().equals(stopDTO.getId()))
                                .findFirst()
                                .orElse(new RouteStop()); // Create new if ID not found
                    } else {
                        // Create new stop
                        routeStop = new RouteStop();
                        routeStop.setRoute(updatedRoute);
                    }
                    
                    // Update stop fields
                    if (stopDTO.getLatitude() != null) {
                        routeStop.setLatitude(stopDTO.getLatitude());
                    }
                    if (stopDTO.getLongitude() != null) {
                        routeStop.setLongitude(stopDTO.getLongitude());
                    }
                    if (stopDTO.getAddress() != null) {
                        routeStop.setAddress(stopDTO.getAddress());
                    }
                    if (stopDTO.getBusStopIndex() != null) {
                        Integer newIndex = stopDTO.getBusStopIndex();

                        if (stopDTO.getId() != null) {
                            // Existing stop being updated: find its current index
                            Integer oldIndex = existingStops.stream()
                                    .filter(s -> s.getId().equals(stopDTO.getId()))
                                    .map(RouteStop::getBusStopIndex)
                                    .findFirst()
                                    .orElse(null);

                            if (oldIndex == null) {
                                // If the existing stop has no index, treat as insertion
                                handleBusStopIndexConflict(routeId, newIndex, stopDTO.getId(), stopDTO.getDirection());
                            } else if (!oldIndex.equals(newIndex)) {
                                logger.info("Moving stop id {} from index {} to {}", stopDTO.getId(), oldIndex, newIndex);

                                // Move down the list (e.g., 1 -> 3): decrement intervening stops
                                if (newIndex > oldIndex) {
                                    List<RouteStop> toDecrement = existingStops.stream()
                                            .filter(stop -> stop.getBusStopIndex() != null)
                                            .filter(stop -> stop.getBusStopIndex() > oldIndex && stop.getBusStopIndex() <= newIndex)
                                            .filter(stop -> !stop.getId().equals(stopDTO.getId()))
                                            .filter(stop -> {
                                                if (stopDTO.getDirection() == null) return true;
                                                if (stop.getDirection() == null) return true;
                                                return stop.getDirection().equals(stopDTO.getDirection()) || stop.getDirection().equals("bidirectional");
                                            })
                                            .collect(java.util.stream.Collectors.toList());

                                    for (RouteStop s : toDecrement) {
                                        s.setBusStopIndex(s.getBusStopIndex() - 1);
                                        routeStopRepository.save(s);
                                        logger.debug("Decremented stop {} to {}", s.getId(), s.getBusStopIndex());
                                    }
                                } else { // newIndex < oldIndex: move up the list, increment intervening stops
                                    List<RouteStop> toIncrement = existingStops.stream()
                                            .filter(stop -> stop.getBusStopIndex() != null)
                                            .filter(stop -> stop.getBusStopIndex() >= newIndex && stop.getBusStopIndex() < oldIndex)
                                            .filter(stop -> !stop.getId().equals(stopDTO.getId()))
                                            .filter(stop -> {
                                                if (stopDTO.getDirection() == null) return true;
                                                if (stop.getDirection() == null) return true;
                                                return stop.getDirection().equals(stopDTO.getDirection()) || stop.getDirection().equals("bidirectional");
                                            })
                                            .collect(java.util.stream.Collectors.toList());

                                    for (RouteStop s : toIncrement) {
                                        s.setBusStopIndex(s.getBusStopIndex() + 1);
                                        routeStopRepository.save(s);
                                        logger.debug("Incremented stop {} to {}", s.getId(), s.getBusStopIndex());
                                    }
                                }
                            } // else no change
                        } else {
                            // New stop insertion: shift existing stops up starting from newIndex
                            handleBusStopIndexConflict(routeId, newIndex, stopDTO.getId(), stopDTO.getDirection());
                        }

                        routeStop.setBusStopIndex(newIndex);
                    }
                    if (stopDTO.getDirection() != null) {
                        routeStop.setDirection(stopDTO.getDirection());
                    }
                    if (stopDTO.getType() != null) {
                        routeStop.setType(stopDTO.getType());
                    }
                    if (stopDTO.getNorthboundIndex() != null) {
                        routeStop.setNorthboundIndex(stopDTO.getNorthboundIndex());
                    }
                    if (stopDTO.getSouthboundIndex() != null) {
                        routeStop.setSouthboundIndex(stopDTO.getSouthboundIndex());
                    }
                    
                    // Validate that new stops have required fields
                    if (stopDTO.getId() == null) { // New stop
                        if (stopDTO.getLatitude() == null || stopDTO.getLongitude() == null) {
                            throw new IllegalArgumentException("New stops must have valid latitude and longitude coordinates");
                        }
                    }
                    
                    // Ensure route is set for new stops
                    if (routeStop.getRoute() == null) {
                        routeStop.setRoute(updatedRoute);
                    }
                    
                    // Save the stop
                    routeStopRepository.save(routeStop);
                }
                
                logger.info("Route and {} stops updated successfully for route ID: {}", 
                           routeUpdateDTO.getStops().size(), routeId);
            }
            
            // Fetch the complete updated route with stops for response
            Route completeRoute = routeRepository.findById(routeId).orElse(updatedRoute);
            List<RouteStop> updatedStops = routeStopRepository.findByRouteIdOrderByBusStopIndex(routeId);
            
            logger.info("Route updated successfully: ID={}, Company={}, BusNumber={}, StopsCount={}", 
                       completeRoute.getId(), completeRoute.getCompany(), 
                       completeRoute.getBusNumber(), updatedStops.size());
            
            // Return a simple response to test if the issue is in response building
            return ResponseEntity.ok(Map.of("message", "Route updated successfully", "routeId", routeId));
        } catch (Exception e) {
            logger.error("Failed to update route with ID {}: {}", routeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to update route: " + (e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName())));
        }
    }
    
    /**
     * Create a safe map for route data to avoid null values
     */
    private Map<String, Object> createSafeRouteMap(Route route, int stopsCount) {
        java.util.Map<String, Object> routeMap = new java.util.HashMap<>();
        routeMap.put("id", route.getId());
        routeMap.put("company", route.getCompany() != null ? route.getCompany() : "");
        routeMap.put("busNumber", route.getBusNumber() != null ? route.getBusNumber() : "");
        routeMap.put("routeName", route.getRouteName() != null ? route.getRouteName() : "");
        routeMap.put("description", route.getDescription() != null ? route.getDescription() : "");
        routeMap.put("active", route.isActive());
        routeMap.put("stopsCount", stopsCount);
        return routeMap;
    }
    
    /**
     * Create a safe map for stop data to avoid null values
     */
    private Map<String, Object> createSafeStopMap(RouteStop stop) {
        java.util.Map<String, Object> stopMap = new java.util.HashMap<>();
        stopMap.put("id", stop.getId());
        stopMap.put("latitude", stop.getLatitude());
        stopMap.put("longitude", stop.getLongitude());
        stopMap.put("address", stop.getAddress() != null ? stop.getAddress() : "");
        stopMap.put("busStopIndex", stop.getBusStopIndex() != null ? stop.getBusStopIndex() : 0);
        stopMap.put("direction", stop.getDirection() != null ? stop.getDirection() : "");
        stopMap.put("type", stop.getType() != null ? stop.getType() : "");
        stopMap.put("northboundIndex", stop.getNorthboundIndex() != null ? stop.getNorthboundIndex() : 0);
        stopMap.put("southboundIndex", stop.getSouthboundIndex() != null ? stop.getSouthboundIndex() : 0);
        return stopMap;
    }
    
    /**
     * Handles bus stop index conflicts by shifting existing stops when necessary
     * @param routeId The route ID
     * @param newIndex The new index being assigned
     * @param excludeStopId The stop ID to exclude from conflict checking (for updates)
     * @param direction The direction of the stop being added/updated
     */
    private void handleBusStopIndexConflict(Long routeId, Integer newIndex, Long excludeStopId, String direction) {
        if (newIndex == null) return;
        
        try {
            // Get existing stops with the same or higher index in the same direction
            List<RouteStop> existingStops = routeStopRepository.findByRouteIdOrderByBusStopIndex(routeId);
            
            List<RouteStop> conflictingStops = existingStops.stream()
                .filter(stop -> stop.getBusStopIndex() != null)
                .filter(stop -> stop.getBusStopIndex() >= newIndex)
                .filter(stop -> excludeStopId == null || !stop.getId().equals(excludeStopId))
                .filter(stop -> {
                    // Handle null direction values safely
                    if (direction == null) return true; // If no direction specified, affect all stops
                    if (stop.getDirection() == null) return true; // If stop has no direction, it can be affected
                    return stop.getDirection().equals(direction) || 
                           stop.getDirection().equals("bidirectional");
                })
                .collect(java.util.stream.Collectors.toList());
            
            if (!conflictingStops.isEmpty()) {
                logger.info("Found {} conflicting stops at index {} or higher. Shifting indices...", 
                           conflictingStops.size(), newIndex);
                
                // Shift all conflicting stops by +1
                for (RouteStop conflictingStop : conflictingStops) {
                    Integer currentIndex = conflictingStop.getBusStopIndex();
                    conflictingStop.setBusStopIndex(currentIndex + 1);
                    routeStopRepository.save(conflictingStop);
                    
                    logger.debug("Shifted stop '{}' from index {} to {}", 
                               conflictingStop.getAddress() != null ? conflictingStop.getAddress() : "Unnamed Stop", 
                               currentIndex, currentIndex + 1);
                }
                
                logger.info("Successfully shifted {} stops to accommodate new stop at index {}", 
                           conflictingStops.size(), newIndex);
            }
        } catch (Exception e) {
            logger.error("Error handling bus stop index conflict for route {} at index {}: {}", 
                        routeId, newIndex, e.getMessage());
            // Continue without throwing - let the stop be saved with potential conflict
        }
    }

    @GetMapping("/routes/{busNumber}/{direction}/buses")
    @Operation(summary = "Get available buses for route", description = "Get list of available buses for a specific route and direction")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Available buses retrieved successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid route parameters or error occurred")
    })
    public ResponseEntity<?> getAvailableBusesForRoute(
            @Parameter(description = "Bus route number", required = true)
            @PathVariable String busNumber,
            @Parameter(description = "Direction of travel (e.g., 'Northbound', 'Southbound')", required = true)
            @PathVariable String direction) {
        try {
            List<Map<String, Object>> availableBuses = busSelectionService.getAvailableBusesForRoute(busNumber, direction);
            return ResponseEntity.ok(Map.of(
                "busNumber", busNumber,
                "direction", direction,
                "availableBuses", availableBuses,
                "count", availableBuses.size()
            ));
        } catch (Exception e) {
            logger.error("Error getting available buses for route {} {}: {}", busNumber, direction, e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/routes/{busNumber}/{companyName}")
    @Operation(
        summary = "Get all routes for a bus number and company",
        description = "Retrieves all routes (both directions) for a specific bus number within a company, including all stops. Case-insensitive search."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved all routes for the bus number and company",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "No routes found for the specified bus number and company",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getAllRoutesByBusNumberAndCompany(
            @Parameter(description = "Bus number to search for", required = true, example = "c5")
            @PathVariable String busNumber,
            @Parameter(description = "Company name to search within", required = true, example = "Rea Vaya")
            @PathVariable String companyName) {
        
        try {
            // Use case-insensitive search for routes with stops
            List<Route> routes = routeRepository.findByBusNumberAndCompanyIgnoreCase(busNumber, companyName);
            
            if (routes.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("message", "No routes found for bus number '" + busNumber + 
                                          "' in company '" + companyName + "'"));
            }
            
            // Convert routes to response format with stops
            List<Map<String, Object>> routeResponses = routes.stream()
                .map(route -> {
                    Map<String, Object> routeData = new java.util.LinkedHashMap<>();
                    routeData.put("id", route.getId());
                    routeData.put("company", route.getCompany());
                    routeData.put("busNumber", route.getBusNumber());
                    routeData.put("routeName", route.getRouteName());
                    routeData.put("description", route.getDescription());
                    routeData.put("active", route.isActive());
                    routeData.put("direction", route.getDirection());
                    routeData.put("startPoint", route.getStartPoint());
                    routeData.put("endPoint", route.getEndPoint());
                    
                    // Add stops information
                    List<Map<String, Object>> stopsList = route.getStops().stream()
                        .map(stop -> {
                            Map<String, Object> stopData = new java.util.LinkedHashMap<>();
                            stopData.put("id", stop.getId());
                            stopData.put("latitude", stop.getLatitude());
                            stopData.put("longitude", stop.getLongitude());
                            stopData.put("address", stop.getAddress());
                            stopData.put("busStopIndex", stop.getBusStopIndex());
                            stopData.put("direction", stop.getDirection());
                            stopData.put("type", stop.getType());
                            stopData.put("northboundIndex", stop.getNorthboundIndex());
                            stopData.put("southboundIndex", stop.getSouthboundIndex());
                            return stopData;
                        })
                        .collect(java.util.stream.Collectors.toList());
                    
                    routeData.put("stops", stopsList);
                    return routeData;
                })
                .collect(java.util.stream.Collectors.toList());
            
            return ResponseEntity.ok(Map.of(
                "busNumber", busNumber,
                "companyName", companyName,
                "routes", routeResponses,
                "totalRoutes", routes.size(),
                "directions", routes.stream()
                    .map(Route::getDirection)
                    .distinct()
                    .sorted()
                    .toArray()
            ));
        } catch (Exception e) {
            logger.error("Error retrieving routes for bus {} company {}: {}", busNumber, companyName, e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Error retrieving routes: " + e.getMessage()));
        }
    }

    @GetMapping("/debug/subscriptions")
    @Operation(summary = "Debug subscriptions", description = "Debug endpoint to check subscription status and active buses")
    @ApiResponse(responseCode = "200", description = "Debug information retrieved successfully")
    public ResponseEntity<?> getDebugSubscriptions() {
        try {
            // This would require injecting BusStreamingService, but for now let's create a simple response
            return ResponseEntity.ok(Map.of(
                "message", "Debug endpoint - check server logs for subscription details",
                "timestamp", new java.util.Date(),
                "note", "Look for 'Client X subscribed to bus Y direction Z' in server logs",
                "activeBuses", busSelectionService.getAvailableBusesForRoute("C5", "Northbound"),
                "serverStatus", "running"
            ));
        } catch (Exception e) {
            logger.error("Error in debug endpoint: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/debug/websocket-test")
    @Operation(summary = "Test WebSocket connection", description = "Debug endpoint to test WebSocket connectivity")
    @ApiResponse(responseCode = "200", description = "WebSocket test information retrieved successfully")
    public ResponseEntity<?> testWebSocketConnection() {
        try {
            return ResponseEntity.ok(Map.of(
                "message", "WebSocket test endpoint",
                "timestamp", new java.util.Date(),
                "note", "Use the test button in the client to verify WebSocket connection",
                "websocketEndpoint", "/ws",
                "stompEndpoint", "/ws/websocket"
            ));
        } catch (Exception e) {
            logger.error("Error in WebSocket test endpoint: {}", e.getMessage());
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/routes/{routeId}/stops")
    @Operation(summary = "Create a new bus stop for a route", description = "Create a new bus stop and add it to the specified route")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Stop created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Route not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> createBusStop(
            @Parameter(description = "ID of the route to add the stop to", required = true)
            @PathVariable Long routeId,
            @Parameter(description = "Bus stop creation data", required = true)
            @Valid @RequestBody RouteStopUpdateDTO stopDTO) {
        try {
            logger.info("Creating new bus stop for route {}", routeId);

            // Verify route exists
            Optional<Route> routeOptional = routeRepository.findById(routeId);
            if (routeOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Route route = routeOptional.get();

            // Validate required fields for new stop
            if (stopDTO.getLatitude() == null || stopDTO.getLongitude() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Latitude and longitude are required for new stops"));
            }

            // Create new stop
            RouteStop routeStop = new RouteStop();
            routeStop.setRoute(route);
            routeStop.setLatitude(stopDTO.getLatitude());
            routeStop.setLongitude(stopDTO.getLongitude());

            if (stopDTO.getAddress() != null) {
                routeStop.setAddress(stopDTO.getAddress());
            }
            if (stopDTO.getBusStopIndex() != null) {
                // Handle index conflicts - shift existing stops if necessary
                handleBusStopIndexConflict(routeId, stopDTO.getBusStopIndex(), null, stopDTO.getDirection());
                routeStop.setBusStopIndex(stopDTO.getBusStopIndex());
            }
            if (stopDTO.getDirection() != null) {
                routeStop.setDirection(stopDTO.getDirection());
            }
            if (stopDTO.getType() != null) {
                routeStop.setType(stopDTO.getType());
            }
            if (stopDTO.getNorthboundIndex() != null) {
                routeStop.setNorthboundIndex(stopDTO.getNorthboundIndex());
            }
            if (stopDTO.getSouthboundIndex() != null) {
                routeStop.setSouthboundIndex(stopDTO.getSouthboundIndex());
            }

            // Save the new stop
            RouteStop savedStop = routeStopRepository.save(routeStop);
            logger.info("Successfully created bus stop {} for route {}", savedStop.getId(), routeId);

            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "message", "Bus stop created successfully",
                "stopId", savedStop.getId(),
                "routeId", routeId,
                "stop", Map.of(
                    "id", savedStop.getId(),
                    "latitude", savedStop.getLatitude(),
                    "longitude", savedStop.getLongitude(),
                    "address", savedStop.getAddress(),
                    "busStopIndex", savedStop.getBusStopIndex(),
                    "direction", savedStop.getDirection(),
                    "type", savedStop.getType()
                )
            ));
        } catch (Exception e) {
            logger.error("Failed to create bus stop for route {}: {}", routeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to create bus stop: " + e.getMessage()));
        }
    }

    @DeleteMapping("/routes/{routeId}/stops/{stopId}")
    @Transactional
    @Operation(summary = "Delete a route stop", description = "Delete a specific stop from a route")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Stop deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Route or stop not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteRouteStop(
            @Parameter(description = "ID of the route", required = true)
            @PathVariable Long routeId,
            @Parameter(description = "ID of the stop to delete", required = true)
            @PathVariable Long stopId) {
        try {
            logger.info("Deleting stop {} from route {}", stopId, routeId);
            
            // Verify route exists
            if (!routeRepository.existsById(routeId)) {
                return ResponseEntity.notFound().build();
            }
            
            // Find and delete the stop
            Optional<RouteStop> stopOptional = routeStopRepository.findById(stopId);
            if (stopOptional.isEmpty()) {
                return ResponseEntity.notFound().build();
            }
            
            RouteStop stop = stopOptional.get();
            if (!stop.getRoute().getId().equals(routeId)) {
                return ResponseEntity.badRequest().body(Map.of("error", "Stop does not belong to the specified route"));
            }
            
            Integer deletedIndex = stop.getBusStopIndex();
            routeStopRepository.delete(stop);
            logger.info("Successfully deleted stop {} from route {}", stopId, routeId);
            
            // Normalize indices after deletion - decrement all stops with higher indices
            if (deletedIndex != null) {
                List<RouteStop> remainingStops = routeStopRepository.findByRouteIdOrderByBusStopIndex(routeId);
                for (RouteStop remainingStop : remainingStops) {
                    Integer currentIndex = remainingStop.getBusStopIndex();
                    if (currentIndex != null && currentIndex > deletedIndex) {
                        remainingStop.setBusStopIndex(currentIndex - 1);
                        routeStopRepository.save(remainingStop);
                        logger.debug("Decremented stop {} index from {} to {}", 
                                remainingStop.getId(), currentIndex, currentIndex - 1);
                    }
                }
            }
            
            return ResponseEntity.ok(Map.of(
                "message", "Stop deleted successfully",
                "deletedStopId", stopId,
                "routeId", routeId
            ));
        } catch (Exception e) {
            logger.error("Failed to delete stop {} from route {}: {}", stopId, routeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete stop: " + e.getMessage()));
        }
    }

    @DeleteMapping("/routes/{routeId}")
    @Transactional
    @Operation(summary = "Delete a route", description = "Delete route, its stops, and stored full route geometry")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Route deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Route not found"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> deleteRoute(
            @Parameter(description = "ID of the route to delete", required = true)
            @PathVariable Long routeId) {
        try {
            logger.info("Deleting route {} and associated data", routeId);
            Optional<Route> routeOpt = routeRepository.findById(routeId);
            if (routeOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Map.of("error", "Route not found"));
            }

            List<RouteStop> stops = routeStopRepository.findByRouteIdOrderByBusStopIndex(routeId);
            if (!stops.isEmpty()) {
                routeStopRepository.deleteAll(stops);
            }

            List<FullRoute> fullRoutes = fullRouteRepository.findByRouteId(routeId);
            if (!fullRoutes.isEmpty()) {
                fullRouteRepository.deleteAll(fullRoutes);
            }

            routeRepository.delete(routeOpt.get());
            logger.info("Deleted route {} with {} stops and {} full route geometries", routeId, stops.size(), fullRoutes.size());
            return ResponseEntity.ok(Map.of(
                    "message", "Route deleted successfully",
                    "deletedRouteId", routeId,
                    "deletedStops", stops.size(),
                    "deletedFullRoutes", fullRoutes.size()
            ));
        } catch (Exception e) {
            logger.error("Failed to delete route {}: {}", routeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Failed to delete route: " + e.getMessage()));
        }
    }

    @PostMapping("/routes/{routeId}/stops/{stopId}/index")
    @Operation(summary = "Update a stop's index", description = "Change a stop's busStopIndex and adjust other stops to avoid duplicates")
    public ResponseEntity<?> updateRouteStopIndex(
            @PathVariable Long routeId,
            @PathVariable Long stopId,
            @RequestBody Map<String, Integer> payload) {
        try {
            Integer newIndex = payload.get("busStopIndex");
            if (newIndex == null) return ResponseEntity.badRequest().body(Map.of("error", "busStopIndex required"));

            Optional<RouteStop> stopOptional = routeStopRepository.findById(stopId);
            if (stopOptional.isEmpty()) return ResponseEntity.notFound().build();

            RouteStop target = stopOptional.get();
            if (!target.getRoute().getId().equals(routeId)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "Stop does not belong to route"));
            }

            List<RouteStop> existingStops = routeStopRepository.findByRouteIdOrderByBusStopIndex(routeId);
            Integer oldIndex = target.getBusStopIndex();

            if (oldIndex == null) {
                // treat as insertion
                handleBusStopIndexConflict(routeId, newIndex, stopId, target.getDirection());
            } else if (!oldIndex.equals(newIndex)) {
                if (newIndex > oldIndex) {
                    List<RouteStop> toDecrement = existingStops.stream()
                            .filter(s -> s.getBusStopIndex() != null)
                            .filter(s -> s.getBusStopIndex() > oldIndex && s.getBusStopIndex() <= newIndex)
                            .filter(s -> !s.getId().equals(stopId))
                            .filter(s -> {
                                if (target.getDirection() == null) return true;
                                if (s.getDirection() == null) return true;
                                return s.getDirection().equals(target.getDirection()) || s.getDirection().equals("bidirectional");
                            })
                            .collect(java.util.stream.Collectors.toList());

                    for (RouteStop s : toDecrement) {
                        s.setBusStopIndex(s.getBusStopIndex() - 1);
                        routeStopRepository.save(s);
                    }
                } else {
                    List<RouteStop> toIncrement = existingStops.stream()
                            .filter(s -> s.getBusStopIndex() != null)
                            .filter(s -> s.getBusStopIndex() >= newIndex && s.getBusStopIndex() < oldIndex)
                            .filter(s -> !s.getId().equals(stopId))
                            .filter(s -> {
                                if (target.getDirection() == null) return true;
                                if (s.getDirection() == null) return true;
                                return s.getDirection().equals(target.getDirection()) || s.getDirection().equals("bidirectional");
                            })
                            .collect(java.util.stream.Collectors.toList());

                    for (RouteStop s : toIncrement) {
                        s.setBusStopIndex(s.getBusStopIndex() + 1);
                        routeStopRepository.save(s);
                    }
                }
            }

            target.setBusStopIndex(newIndex);
            routeStopRepository.save(target);

            return ResponseEntity.ok(Map.of("message", "Stop index updated", "stopId", stopId, "busStopIndex", newIndex));
        } catch (Exception e) {
            logger.error("Failed to update stop index {} for route {}: {}", stopId, routeId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", e.getMessage()));
        }
    }

    // ==================== REGISTERED BUS ENDPOINTS ====================

    @PostMapping("/registered-buses")
    @Operation(summary = "Create registered bus", description = "Creates a new registered bus for a company")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Registered bus created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<RegisteredBusResponseDTO> createRegisteredBus(@Valid @RequestBody RegisteredBusCreateDTO createDTO) {
        try {
            RegisteredBusResponseDTO response = registeredBusService.createRegisteredBus(createDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            logger.error("Failed to create registered bus: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @PutMapping("/registered-buses/{id}")
    @Operation(summary = "Update registered bus", description = "Updates an existing registered bus")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Registered bus updated successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input data"),
        @ApiResponse(responseCode = "404", description = "Registered bus not found")
    })
    public ResponseEntity<RegisteredBusResponseDTO> updateRegisteredBus(
            @PathVariable Long id,
            @Valid @RequestBody RegisteredBusCreateDTO updateDTO) {
        try {
            RegisteredBusResponseDTO response = registeredBusService.updateRegisteredBus(id, updateDTO);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(null);
        } catch (Exception e) {
            logger.error("Failed to update registered bus {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @DeleteMapping("/registered-buses/{id}")
    @Operation(summary = "Delete registered bus", description = "Deletes a registered bus")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Registered bus deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Registered bus not found")
    })
    public ResponseEntity<Void> deleteRegisteredBus(@PathVariable Long id) {
        try {
            registeredBusService.deleteRegisteredBus(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to delete registered bus {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/registered-buses/{id}")
    @Operation(summary = "Get registered bus", description = "Retrieves a registered bus by ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Registered bus retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Registered bus not found")
    })
    public ResponseEntity<RegisteredBusResponseDTO> getRegisteredBus(@PathVariable Long id) {
        try {
            RegisteredBusResponseDTO response = registeredBusService.getRegisteredBusById(id);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to get registered bus {}: {}", id, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/registered-buses/company/{companyId}")
    @Operation(summary = "Get registered buses by company", description = "Retrieves all registered buses for a specific company")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Registered buses retrieved successfully"),
        @ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<List<RegisteredBusResponseDTO>> getRegisteredBusesByCompany(@PathVariable Long companyId) {
        try {
            List<RegisteredBusResponseDTO> response = registeredBusService.getRegisteredBusesByCompany(companyId);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build();
        } catch (Exception e) {
            logger.error("Failed to get registered buses for company {}: {}", companyId, e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/registered-buses")
    @Operation(summary = "Get all registered buses", description = "Retrieves all registered buses")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Registered buses retrieved successfully")
    })
    public ResponseEntity<List<RegisteredBusResponseDTO>> getAllRegisteredBuses() {
        try {
            List<RegisteredBusResponseDTO> response = registeredBusService.getAllRegisteredBuses();
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to get all registered buses: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}