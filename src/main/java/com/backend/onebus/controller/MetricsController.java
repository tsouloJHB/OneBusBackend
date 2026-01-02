package com.backend.onebus.controller;

import com.backend.onebus.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/metrics")
@CrossOrigin(origins = "*")
@Tag(name = "Metrics", description = "Real-time performance monitoring and metrics")
public class MetricsController {
    
    @Autowired
    private MetricsService metricsService;
    
    @GetMapping("/current")
    @Operation(summary = "Get current system metrics", description = "Returns real-time performance metrics including counters, latency stats, and recent events")
    public ResponseEntity<Map<String, Object>> getCurrentMetrics() {
        return ResponseEntity.ok(metricsService.getCurrentMetrics());
    }
    
    @GetMapping("/pipeline")
    @Operation(summary = "Get tracking pipeline metrics", description = "Returns detailed tracking pipeline events with timing information")
    public ResponseEntity<List<MetricsService.TrackingPipelineMetric>> getPipelineMetrics(
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(metricsService.getPipelineMetrics(limit));
    }
    
    @GetMapping("/sessions")
    @Operation(summary = "Get WebSocket session metrics", description = "Returns information about WebSocket connections and their duration")
    public ResponseEntity<List<MetricsService.WebSocketSessionMetric>> getSessionMetrics() {
        return ResponseEntity.ok(metricsService.getSessionMetrics());
    }
    
    @PostMapping("/cleanup-sessions")
    @Operation(summary = "Manual session cleanup for testing", description = "Manually triggers cleanup of orphaned WebSocket sessions (for testing purposes)")
    public ResponseEntity<Map<String, Object>> cleanupOrphanedSessions() {
        // This is a test endpoint - in production, sessions should be cleaned up automatically
        // when WebSocket connections are properly closed
        
        List<MetricsService.WebSocketSessionMetric> sessions = metricsService.getSessionMetrics();
        long activeSessions = sessions.stream()
            .filter(session -> session.getDisconnectedAt() == null)
            .count();
        
        return ResponseEntity.ok(Map.of(
            "status", "info",
            "message", "Found " + activeSessions + " active sessions",
            "note", "Sessions should be cleaned up automatically when WebSocket connections close properly",
            "issue", "If sessions persist, the WebSocket connections are not being closed from the Flutter client",
            "active_sessions", activeSessions
        ));
    }
    
    @GetMapping("/health")
    @Operation(summary = "Get system health status", description = "Returns basic health indicators for the tracking system")
    public ResponseEntity<Map<String, Object>> getHealthStatus() {
        Map<String, Object> metrics = metricsService.getCurrentMetrics();
        
        // Calculate health indicators
        Map<String, Object> health = Map.of(
            "status", "healthy", // Could be enhanced with actual health checks
            "active_sessions", metrics.get("active_websocket_sessions"),
            "total_buses_tracked", ((Map<String, Long>) metrics.get("counters")).getOrDefault("bus_locations_processed", 0L),
            "total_broadcasts", ((Map<String, Long>) metrics.get("counters")).getOrDefault("websocket_broadcasts", 0L),
            "uptime_seconds", System.currentTimeMillis() / 1000 // Simple uptime
        );
        
        return ResponseEntity.ok(health);
    }
}