package com.backend.onebus.controller;

import com.backend.onebus.dto.RouteUpdateDTO;
import com.backend.onebus.dto.RouteStopUpdateDTO;
import com.backend.onebus.model.Bus;
import com.backend.onebus.model.BusLocation;
import com.backend.onebus.model.Route;
import com.backend.onebus.model.RouteStop;
import com.backend.onebus.repository.BusRepository;
import com.backend.onebus.repository.RouteRepository;
import com.backend.onebus.repository.RouteStopRepository;
import com.backend.onebus.service.BusTrackingService;
import com.backend.onebus.service.BusSelectionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import java.util.Optional;

import java.io.IOException;
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
    private ObjectMapper objectMapper;
    @Autowired
    private BusSelectionService busSelectionService;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BusTrackingController.class);

    @PostMapping("/tracker/payload")
    @Operation(summary = "Receive tracker payload", description = "Receives GPS tracking data from bus tracking devices")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Payload received successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid payload format")
    })
    public ResponseEntity<?> receiveTrackerPayload(@RequestBody BusLocation payload) {
        logger.info("Received tracker payload: {}", payload);
        trackingService.processTrackerPayload(payload);
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
    @Operation(summary = "Get active buses", description = "Retrieve a list of all currently active bus identifiers")
    @ApiResponse(responseCode = "200", description = "List of active buses retrieved successfully")
    public ResponseEntity<Set<String>> getActiveBuses() {
        Set<String> activeBuses = trackingService.getActiveBuses();
        return ResponseEntity.ok(activeBuses);
    }

    @GetMapping("/buses")
    @Operation(summary = "Get all buses", description = "Retrieve all buses from the database")
    @ApiResponse(responseCode = "200", description = "List of all buses retrieved successfully")
    public ResponseEntity<List<Bus>> getAllBuses() {
        List<Bus> buses = busRepository.findAll();
        return ResponseEntity.ok(buses);
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
                        // Handle index conflicts - shift existing stops if necessary
                        handleBusStopIndexConflict(routeId, stopDTO.getBusStopIndex(), stopDTO.getId(), stopDTO.getDirection());
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

    @DeleteMapping("/routes/{routeId}/stops/{stopId}")
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
            
            routeStopRepository.delete(stop);
            logger.info("Successfully deleted stop {} from route {}", stopId, routeId);
            
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
}