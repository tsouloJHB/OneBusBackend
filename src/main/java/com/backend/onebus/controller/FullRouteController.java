package com.backend.onebus.controller;

import com.backend.onebus.model.FullRoute;
import com.backend.onebus.repository.FullRouteRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/full-routes")
@Tag(name = "Full Routes", description = "CRUD endpoints for storing full route coordinate geometry")
public class FullRouteController {
    private static final Logger logger = LoggerFactory.getLogger(FullRouteController.class);
    private final FullRouteRepository fullRouteRepository;
    private final ObjectMapper objectMapper;
    private final com.backend.onebus.service.RouteGeometryService routeGeometryService;

    public FullRouteController(FullRouteRepository fullRouteRepository, ObjectMapper objectMapper,
                               com.backend.onebus.service.RouteGeometryService routeGeometryService) {
        this.fullRouteRepository = fullRouteRepository;
        this.objectMapper = objectMapper;
        this.routeGeometryService = routeGeometryService;
    }

    @GetMapping
    @Operation(summary = "List full routes", description = "Fetch full routes filtered by companyId and/or routeId")
    public ResponseEntity<List<FullRouteResponse>> list(
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long routeId) {
        List<FullRoute> routes;
        if (companyId != null && routeId != null) {
            routes = fullRouteRepository.findByCompanyIdAndRouteId(companyId, routeId);
        } else if (companyId != null) {
            routes = fullRouteRepository.findByCompanyId(companyId);
        } else if (routeId != null) {
            routes = fullRouteRepository.findByRouteId(routeId);
        } else {
            routes = fullRouteRepository.findAll();
        }
        return ResponseEntity.ok(routes.stream().map(this::toResponse).toList());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get full route", description = "Fetch a single full route by ID")
    public ResponseEntity<FullRouteResponse> get(@PathVariable Long id) {
        Optional<FullRoute> found = fullRouteRepository.findById(id);
        return found.map(route -> ResponseEntity.ok(toResponse(route)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    @PostMapping
    @Operation(summary = "Create full route", description = "Create a stored route geometry for a company/route")
    public ResponseEntity<?> create(@Valid @RequestBody FullRouteRequest request) {
        try {
            FullRoute entity = new FullRoute();
            applyRequestToEntity(request, entity);
            FullRoute saved = fullRouteRepository.save(entity);
            return ResponseEntity.status(HttpStatus.CREATED).body(toResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update full route", description = "Update stored route geometry")
    public ResponseEntity<?> update(@PathVariable Long id, @Valid @RequestBody FullRouteRequest request) {
        Optional<FullRoute> existingOpt = fullRouteRepository.findById(id);
        if (existingOpt.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Collections.singletonMap("error", "FullRoute not found"));
        }
        try {
            FullRoute entity = existingOpt.get();
            applyRequestToEntity(request, entity);
            FullRoute saved = fullRouteRepository.save(entity);
            return ResponseEntity.ok(toResponse(saved));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Collections.singletonMap("error", e.getMessage()));
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete full route", description = "Remove a stored route geometry")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        if (!fullRouteRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Collections.singletonMap("error", "FullRoute not found"));
        }
        fullRouteRepository.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/maintenance/backfill-distances")
    @Operation(summary = "Backfill cumulative distances", description = "Calculate missing cumulative distances for all existing routes")
    public ResponseEntity<Map<String, Object>> backfillDistances() {
        logger.info("Starting backfill of cumulative distances for all full routes...");
        List<FullRoute> allRoutes = fullRouteRepository.findAll();
        int totalProcessed = 0;
        int updatedCount = 0;
        int skippedCount = 0;
        int errorCount = 0;

        for (FullRoute route : allRoutes) {
            totalProcessed++;
            try {
                // Only process if coordinates exist but cumulative distances are missing
                String coordsJson = route.getCoordinatesJson();
                String cumDistJson = route.getCumulativeDistancesJson();

                if (coordsJson == null || coordsJson.isEmpty() || "[]".equals(coordsJson.trim())) {
                    skippedCount++;
                    continue;
                }

                // If already has distances, skip (unless you want to re-calculate everything)
                if (cumDistJson != null && !cumDistJson.isEmpty() && !"[]".equals(cumDistJson.trim())) {
                    skippedCount++;
                    continue;
                }

                List<Coordinate> coords = objectMapper.readValue(coordsJson, new TypeReference<List<Coordinate>>() {});
                if (!coords.isEmpty()) {
                    List<Double> cumulativeDistances = routeGeometryService.calculateCumulativeDistances(coords);
                    route.setCumulativeDistancesJson(objectMapper.writeValueAsString(cumulativeDistances));
                    fullRouteRepository.save(route);
                    updatedCount++;
                    logger.debug("Backfilled route ID {}: {} points, total {} meters", 
                                route.getId(), coords.size(), cumulativeDistances.get(cumulativeDistances.size() - 1));
                } else {
                    skippedCount++;
                }

            } catch (Exception e) {
                logger.error("Failed to backfill distance for route ID {}: {}", route.getId(), e.getMessage());
                errorCount++;
            }
        }

        logger.info("Backfill complete. Updated: {}, Skipped: {}, Errors: {}, Total: {}", 
                    updatedCount, skippedCount, errorCount, totalProcessed);

        Map<String, Object> response = new HashMap<>();
        response.put("status", "success");
        response.put("updated", updatedCount);
        response.put("skipped", skippedCount);
        response.put("errors", errorCount);
        response.put("total", totalProcessed);
        
        return ResponseEntity.ok(response);
    }

    private void applyRequestToEntity(FullRouteRequest request, FullRoute entity) throws Exception {
        entity.setCompanyId(request.getCompanyId());
        entity.setRouteId(request.getRouteId());
        entity.setName(request.getName());
        entity.setDirection(request.getDirection());
        entity.setDescription(request.getDescription());
        List<Coordinate> coords = request.getCoordinates();
        if (coords == null || coords.isEmpty()) {
            coords = List.of(); // allow saving without coordinates yet
        }
        entity.setCoordinatesJson(objectMapper.writeValueAsString(coords));
        
        // Auto-calculate cumulative distances for route-based distance calculations
        if (!coords.isEmpty()) {
            List<Double> cumulativeDistances = routeGeometryService.calculateCumulativeDistances(coords);
            entity.setCumulativeDistancesJson(objectMapper.writeValueAsString(cumulativeDistances));
            logger.info("Calculated cumulative distances for route {} ({}): {} points, total {} meters",
                       entity.getName(), entity.getDirection(), coords.size(), 
                       cumulativeDistances.isEmpty() ? 0 : cumulativeDistances.get(cumulativeDistances.size() - 1));
        }
    }

    private FullRouteResponse toResponse(FullRoute entity) {
        try {
            String json = entity.getCoordinatesJson();
            if (json == null || json.isEmpty() || "[]".equals(json.trim())) {
                logger.debug("Empty coordinates for FullRoute id={}", entity.getId());
                return new FullRouteResponse(
                        entity.getId(),
                        entity.getCompanyId(),
                        entity.getRouteId(),
                        entity.getName(),
                        entity.getDirection(),
                        entity.getDescription(),
                        List.of(),
                        List.of(),
                        entity.getCreatedAt(),
                        entity.getUpdatedAt()
                );
            }
            List<Coordinate> coords = objectMapper.readValue(json, new TypeReference<List<Coordinate>>() {});
            
            // Parse cumulative distances if available
            List<Double> cumulativeDistances = List.of();
            String cumDistJson = entity.getCumulativeDistancesJson();
            if (cumDistJson != null && !cumDistJson.isEmpty() && !"[]".equals(cumDistJson.trim())) {
                try {
                    cumulativeDistances = objectMapper.readValue(cumDistJson, new TypeReference<List<Double>>() {});
                } catch (Exception e) {
                    logger.warn("Failed to parse cumulative distances for FullRoute id={}", entity.getId());
                }
            }
            
            return new FullRouteResponse(
                    entity.getId(),
                    entity.getCompanyId(),
                    entity.getRouteId(),
                    entity.getName(),
                    entity.getDirection(),
                    entity.getDescription(),
                    coords,
                    cumulativeDistances,
                    entity.getCreatedAt(),
                    entity.getUpdatedAt()
            );
        } catch (Exception e) {
            logger.error("Failed to parse coordinates for FullRoute id={}: coordinatesJson={}", entity.getId(), entity.getCoordinatesJson(), e);
            // Fallback in case JSON is corrupted
            return new FullRouteResponse(
                    entity.getId(),
                    entity.getCompanyId(),
                    entity.getRouteId(),
                    entity.getName(),
                    entity.getDirection(),
                    entity.getDescription(),
                    List.of(),
                    List.of(),
                    entity.getCreatedAt(),
                    entity.getUpdatedAt()
            );
        }
    }

    // --- DTOs ---
    public static class Coordinate {
        @JsonProperty("lat")
        private double lat;
        @JsonProperty("lon")
        private double lon;
        public Coordinate() {}
        public Coordinate(double lat, double lon) { this.lat = lat; this.lon = lon; }
        public double getLat() { return lat; }
        public void setLat(double lat) { this.lat = lat; }
        public double getLon() { return lon; }
        public void setLon(double lon) { this.lon = lon; }
    }

    public static class FullRouteRequest {
        @NotNull
        private Long companyId;
        @NotNull
        private Long routeId;
        @NotBlank
        private String name;
        private String direction;
        private String description;
        private List<Coordinate> coordinates;

        public Long getCompanyId() { return companyId; }
        public void setCompanyId(Long companyId) { this.companyId = companyId; }
        public Long getRouteId() { return routeId; }
        public void setRouteId(Long routeId) { this.routeId = routeId; }
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<Coordinate> getCoordinates() { return coordinates; }
        public void setCoordinates(List<Coordinate> coordinates) { this.coordinates = coordinates; }
    }

    public record FullRouteResponse(
            Long id,
            Long companyId,
            Long routeId,
            String name,
            String direction,
            String description,
            List<Coordinate> coordinates,
            List<Double> cumulativeDistances,
            java.time.LocalDateTime createdAt,
            java.time.LocalDateTime updatedAt
    ) {}
}
