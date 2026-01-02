package com.backend.onebus.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.time.LocalDateTime;
import java.time.Duration;

@Service
public class MetricsService {
    
    private static final Logger logger = LoggerFactory.getLogger(MetricsService.class);
    
    // Performance metrics
    private final Map<String, AtomicLong> counters = new ConcurrentHashMap<>();
    private final Map<String, List<Long>> latencyMetrics = new ConcurrentHashMap<>();
    private final Map<String, Object> currentStats = new ConcurrentHashMap<>();
    
    // Real-time tracking pipeline metrics
    private final List<TrackingPipelineMetric> pipelineMetrics = Collections.synchronizedList(new ArrayList<>());
    private final Map<String, WebSocketSessionMetric> sessionMetrics = new ConcurrentHashMap<>();
    
    // Keep only last 1000 pipeline metrics for performance
    private static final int MAX_PIPELINE_METRICS = 1000;
    
    public void recordTrackerPayloadReceived(String busId, String trackerImei) {
        incrementCounter("tracker_payloads_received");
        recordPipelineEvent(busId, "TRACKER_PAYLOAD_RECEIVED", System.currentTimeMillis());
    }
    
    public void recordBusLocationProcessed(String busId, long processingTimeMs) {
        incrementCounter("bus_locations_processed");
        recordLatency("bus_location_processing", processingTimeMs);
        recordPipelineEvent(busId, "BUS_LOCATION_PROCESSED", System.currentTimeMillis(), processingTimeMs);
    }
    
    public void recordWebSocketBroadcast(String busId, int subscriberCount, long broadcastTimeMs) {
        incrementCounter("websocket_broadcasts");
        recordLatency("websocket_broadcast", broadcastTimeMs);
        recordPipelineEvent(busId, "WEBSOCKET_BROADCAST", System.currentTimeMillis(), broadcastTimeMs, subscriberCount);
    }
    
    public void recordWebSocketConnection(String sessionId, String busNumber, String direction) {
        incrementCounter("websocket_connections");
        
        // Check if this session is already recorded to prevent duplicates
        if (sessionMetrics.containsKey(sessionId)) {
            logger.debug("Session {} already recorded, updating bus info to {} {}", sessionId, busNumber, direction);
            WebSocketSessionMetric existingMetric = sessionMetrics.get(sessionId);
            // Update the existing metric with new bus info (in case of smart bus selection)
            existingMetric.setBusNumber(busNumber);
            existingMetric.setDirection(direction);
        } else {
            WebSocketSessionMetric metric = new WebSocketSessionMetric(sessionId, busNumber, direction, LocalDateTime.now());
            sessionMetrics.put(sessionId, metric);
            logger.debug("New session {} recorded for bus {} {}", sessionId, busNumber, direction);
        }
    }
    
    public void recordWebSocketDisconnection(String sessionId) {
        incrementCounter("websocket_disconnections");
        WebSocketSessionMetric metric = sessionMetrics.get(sessionId);
        if (metric != null) {
            metric.setDisconnectedAt(LocalDateTime.now());
            metric.setDuration(Duration.between(metric.getConnectedAt(), metric.getDisconnectedAt()));
        }
    }
    
    public void recordSmartBusSelection(String sessionId, String originalDirection, String selectedBusId, boolean isFallback) {
        incrementCounter("smart_bus_selections");
        if (isFallback) {
            incrementCounter("fallback_selections");
        }
        recordPipelineEvent(selectedBusId, "SMART_BUS_SELECTED", System.currentTimeMillis(), 0, 0, 
                           Map.of("sessionId", sessionId, "originalDirection", originalDirection, "isFallback", isFallback));
    }
    
    private void incrementCounter(String key) {
        counters.computeIfAbsent(key, k -> new AtomicLong(0)).incrementAndGet();
    }
    
    private void recordLatency(String key, long latencyMs) {
        latencyMetrics.computeIfAbsent(key, k -> Collections.synchronizedList(new ArrayList<>())).add(latencyMs);
        // Keep only last 100 latency measurements
        List<Long> latencies = latencyMetrics.get(key);
        if (latencies.size() > 100) {
            latencies.remove(0);
        }
    }
    
    private void recordPipelineEvent(String busId, String event, long timestamp) {
        recordPipelineEvent(busId, event, timestamp, 0, 0, null);
    }
    
    private void recordPipelineEvent(String busId, String event, long timestamp, long processingTime) {
        recordPipelineEvent(busId, event, timestamp, processingTime, 0, null);
    }
    
    private void recordPipelineEvent(String busId, String event, long timestamp, long processingTime, int subscriberCount) {
        recordPipelineEvent(busId, event, timestamp, processingTime, subscriberCount, null);
    }
    
    private void recordPipelineEvent(String busId, String event, long timestamp, long processingTime, int subscriberCount, Map<String, Object> metadata) {
        TrackingPipelineMetric metric = new TrackingPipelineMetric(
            busId, event, timestamp, processingTime, subscriberCount, metadata
        );
        
        pipelineMetrics.add(metric);
        
        // Remove old metrics to prevent memory issues
        if (pipelineMetrics.size() > MAX_PIPELINE_METRICS) {
            pipelineMetrics.remove(0);
        }
    }
    
    public Map<String, Object> getCurrentMetrics() {
        Map<String, Object> metrics = new HashMap<>();
        
        // Basic counters
        Map<String, Long> counterValues = new HashMap<>();
        counters.forEach((key, value) -> counterValues.put(key, value.get()));
        metrics.put("counters", counterValues);
        
        // Latency statistics
        Map<String, Map<String, Double>> latencyStats = new HashMap<>();
        latencyMetrics.forEach((key, values) -> {
            if (!values.isEmpty()) {
                Map<String, Double> stats = new HashMap<>();
                stats.put("avg", values.stream().mapToLong(Long::longValue).average().orElse(0.0));
                stats.put("min", (double) values.stream().mapToLong(Long::longValue).min().orElse(0));
                stats.put("max", (double) values.stream().mapToLong(Long::longValue).max().orElse(0));
                stats.put("p95", calculatePercentile(values, 95));
                latencyStats.put(key, stats);
            }
        });
        metrics.put("latency", latencyStats);
        
        // Active WebSocket sessions
        long activeSessions = sessionMetrics.values().stream()
            .filter(session -> session.getDisconnectedAt() == null)
            .count();
        metrics.put("active_websocket_sessions", activeSessions);
        
        // Recent pipeline events (last 50)
        List<TrackingPipelineMetric> recentEvents = pipelineMetrics.stream()
            .skip(Math.max(0, pipelineMetrics.size() - 50))
            .toList();
        metrics.put("recent_pipeline_events", recentEvents);
        
        return metrics;
    }
    
    public List<TrackingPipelineMetric> getPipelineMetrics(int limit) {
        return pipelineMetrics.stream()
            .skip(Math.max(0, pipelineMetrics.size() - limit))
            .toList();
    }
    
    public List<WebSocketSessionMetric> getSessionMetrics() {
        return new ArrayList<>(sessionMetrics.values());
    }
    
    private double calculatePercentile(List<Long> values, int percentile) {
        if (values.isEmpty()) return 0.0;
        List<Long> sorted = new ArrayList<>(values);
        sorted.sort(Long::compareTo);
        int index = (int) Math.ceil(percentile / 100.0 * sorted.size()) - 1;
        return sorted.get(Math.max(0, Math.min(index, sorted.size() - 1))).doubleValue();
    }
    
    // Inner classes for metrics data
    public static class TrackingPipelineMetric {
        private final String busId;
        private final String event;
        private final long timestamp;
        private final long processingTime;
        private final int subscriberCount;
        private final Map<String, Object> metadata;
        
        public TrackingPipelineMetric(String busId, String event, long timestamp, long processingTime, int subscriberCount, Map<String, Object> metadata) {
            this.busId = busId;
            this.event = event;
            this.timestamp = timestamp;
            this.processingTime = processingTime;
            this.subscriberCount = subscriberCount;
            this.metadata = metadata != null ? metadata : new HashMap<>();
        }
        
        // Getters
        public String getBusId() { return busId; }
        public String getEvent() { return event; }
        public long getTimestamp() { return timestamp; }
        public long getProcessingTime() { return processingTime; }
        public int getSubscriberCount() { return subscriberCount; }
        public Map<String, Object> getMetadata() { return metadata; }
    }
    
    public static class WebSocketSessionMetric {
        private final String sessionId;
        private String busNumber;
        private String direction;
        private final LocalDateTime connectedAt;
        private LocalDateTime disconnectedAt;
        private Duration duration;
        
        public WebSocketSessionMetric(String sessionId, String busNumber, String direction, LocalDateTime connectedAt) {
            this.sessionId = sessionId;
            this.busNumber = busNumber;
            this.direction = direction;
            this.connectedAt = connectedAt;
        }
        
        // Getters and setters
        public String getSessionId() { return sessionId; }
        public String getBusNumber() { return busNumber; }
        public void setBusNumber(String busNumber) { this.busNumber = busNumber; }
        public String getDirection() { return direction; }
        public void setDirection(String direction) { this.direction = direction; }
        public LocalDateTime getConnectedAt() { return connectedAt; }
        public LocalDateTime getDisconnectedAt() { return disconnectedAt; }
        public void setDisconnectedAt(LocalDateTime disconnectedAt) { this.disconnectedAt = disconnectedAt; }
        public Duration getDuration() { return duration; }
        public void setDuration(Duration duration) { this.duration = duration; }
    }
}