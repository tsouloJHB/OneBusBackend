package com.backend.onebus.controller;

import com.backend.onebus.model.Bus;
import com.backend.onebus.model.BusLocation;
import com.backend.onebus.model.Route;
import com.backend.onebus.model.RouteStop;
import com.backend.onebus.repository.RouteRepository;
import com.backend.onebus.repository.RouteStopRepository;
import com.backend.onebus.service.BusTrackingService;
import com.backend.onebus.service.BusSelectionService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api")
public class BusTrackingController {
    @Autowired
    private BusTrackingService trackingService;
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
    public ResponseEntity<?> receiveTrackerPayload(@RequestBody BusLocation payload) {
        logger.info("Received tracker payload: {}", payload);
        trackingService.processTrackerPayload(payload);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/buses/nearest")
    public ResponseEntity<BusLocation> getNearestBus(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam String tripDirection) {
        BusLocation nearestBus = trackingService.findNearestBus(lat, lon, tripDirection);
        return nearestBus != null ? ResponseEntity.ok(nearestBus) : ResponseEntity.notFound().build();
    }

    @PutMapping("/buses/{busId}")
    public ResponseEntity<?> updateBusDetails(@PathVariable String busId, @RequestBody Bus bus) {
        bus.setBusId(busId);
        trackingService.updateBusDetails(bus);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/clear")
    public ResponseEntity<?> clearTrackingData() {
        trackingService.clearTrackingData();
        return ResponseEntity.ok().build();
    }

    @GetMapping("/buses/{busNumber}/location")
    public ResponseEntity<BusLocation> getBusLocation(
            @PathVariable String busNumber,
            @RequestParam String direction) {
        BusLocation location = trackingService.getBusLocation(busNumber, direction);
        return location != null ? ResponseEntity.ok(location) : ResponseEntity.notFound().build();
    }

    @GetMapping("/buses/active")
    public ResponseEntity<Set<String>> getActiveBuses() {
        Set<String> activeBuses = trackingService.getActiveBuses();
        return ResponseEntity.ok(activeBuses);
    }

    @PostMapping("/buses")
    public ResponseEntity<Bus> createBus(@RequestBody Bus bus) {
        Bus saved = trackingService.saveBus(bus);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/routes")
    public ResponseEntity<Route> createRoute(@RequestBody Route route) {
        Route saved = routeRepository.save(route);
        return ResponseEntity.ok(saved);
    }

    @PostMapping("/routes/{routeId}/stops")
    public ResponseEntity<RouteStop> addRouteStop(@PathVariable Long routeId, @RequestBody RouteStop routeStop) {
        Route route = routeRepository.findById(routeId).orElse(null);
        if (route == null) {
            return ResponseEntity.notFound().build();
        }
        routeStop.setRoute(route);
        RouteStop saved = routeStopRepository.save(routeStop);
        return ResponseEntity.ok(saved);
    }

    @GetMapping("/routes")
    public ResponseEntity<List<Route>> getAllRoutes() {
        List<Route> routes = routeRepository.findByActiveTrue();
        return ResponseEntity.ok(routes);
    }

    @GetMapping("/routes/{routeId}")
    public ResponseEntity<Route> getRoute(@PathVariable Long routeId) {
        Route route = routeRepository.findById(routeId).orElse(null);
        return route != null ? ResponseEntity.ok(route) : ResponseEntity.notFound().build();
    }

    @PostMapping("/routes/import-json")
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

    @GetMapping("/routes/{busNumber}/{direction}/buses")
    public ResponseEntity<?> getAvailableBusesForRoute(
            @PathVariable String busNumber,
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
}