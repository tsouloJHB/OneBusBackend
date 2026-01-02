package com.backend.onebus.service;

import com.backend.onebus.model.BusLocation;
import com.backend.onebus.service.MetricsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BusStreamingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BusStreamingService.class);
    private static final String BUS_LOCATION_KEY = "bus:location:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    @Autowired
    private MetricsService metricsService;
    
    // Track active subscriptions: Map<busNumber_direction, Set<sessionId>>
    private final Map<String, Set<String>> activeSubscriptions = new ConcurrentHashMap<>();
    // Map canonical (lowercase) subscription key -> original subscriptionKey(s)
    private final Map<String, Set<String>> canonicalToOriginalKeys = new ConcurrentHashMap<>();
    
    // Track specific bus subscriptions: Map<busId, Set<sessionId>>
    private final Map<String, Set<String>> specificBusSubscriptions = new ConcurrentHashMap<>();
    
    // Track client subscriptions for dynamic re-evaluation: Map<sessionId, ClientSubscription>
    private final Map<String, ClientSubscription> clientSubscriptions = new ConcurrentHashMap<>();
    
    /**
     * Subscribe a client to a specific bus and direction
     */
    public void subscribeToBus(String sessionId, String busNumber, String direction) {
        String subscriptionKey = busNumber + "_" + direction;
        activeSubscriptions.computeIfAbsent(subscriptionKey, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        // Also register the canonical key mapping
        String canonical = (busNumber + "_" + direction).toLowerCase();
        canonicalToOriginalKeys.computeIfAbsent(canonical, k -> ConcurrentHashMap.newKeySet()).add(subscriptionKey);
        
        // Record metrics
        metricsService.recordWebSocketConnection(sessionId, busNumber, direction);
        
        logger.info("Client {} subscribed to bus {} direction {} (key: {})", sessionId, busNumber, direction, subscriptionKey);
        logger.debug("Current active subscriptions: {}", activeSubscriptions.keySet());
        
        // Send current location immediately if available
        BusLocation currentLocation = getCurrentBusLocation(busNumber, direction);
        if (currentLocation != null) {
            String destination = "/topic/bus/" + subscriptionKey;
            logger.debug("Sending current location to session {} at destination: {}", sessionId, destination);
            messagingTemplate.convertAndSendToUser(sessionId, destination, currentLocation);
        } else {
            logger.debug("No current location available for bus {} direction {}", busNumber, direction);
        }
    }
    
    /**
     * Unsubscribe a client from a specific bus and direction
     */
    public void unsubscribeFromBus(String sessionId, String busNumber, String direction) {
        String subscriptionKey = busNumber + "_" + direction;
        Set<String> subscribers = activeSubscriptions.get(subscriptionKey);
        if (subscribers != null) {
            subscribers.remove(sessionId);
            if (subscribers.isEmpty()) {
                activeSubscriptions.remove(subscriptionKey);
                // Remove from canonical mapping as well
                String canonical = subscriptionKey.toLowerCase();
                Set<String> originals = canonicalToOriginalKeys.get(canonical);
                if (originals != null) {
                    originals.remove(subscriptionKey);
                    if (originals.isEmpty()) canonicalToOriginalKeys.remove(canonical);
                }
            }
        }
        logger.info("Client {} unsubscribed from bus {} direction {}", sessionId, busNumber, direction);
    }
    
    /**
     * Subscribe a client to a specific bus by bus ID
     */
    public void subscribeToSpecificBus(String sessionId, String busId) {
        specificBusSubscriptions.computeIfAbsent(busId, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        logger.info("Client {} subscribed to specific bus {}", sessionId, busId);
        
        // Send current location immediately if available
        BusLocation currentLocation = getBusLocationById(busId);
        if (currentLocation != null) {
            // Broadcast on the public topic so any subscriber to /topic/bus/{busId} receives it
            messagingTemplate.convertAndSend("/topic/bus/" + busId, currentLocation);
        }
    }
    
    /**
     * Unsubscribe a client from a specific bus by bus ID
     */
    public void unsubscribeFromSpecificBus(String sessionId, String busId) {
        Set<String> subscribers = specificBusSubscriptions.get(busId);
        if (subscribers != null) {
            subscribers.remove(sessionId);
            if (subscribers.isEmpty()) {
                specificBusSubscriptions.remove(busId);
            }
        }
        clientSubscriptions.remove(sessionId);
        logger.info("Client {} unsubscribed from specific bus {}", sessionId, busId);
    }
    
    /**
     * Store client subscription details for dynamic re-evaluation
     */
    public void storeClientSubscription(String sessionId, String busNumber, String direction, 
                                      double clientLat, double clientLon, int clientBusStopIndex, String busId) {
        ClientSubscription subscription = new ClientSubscription(
            sessionId, busNumber, direction, clientLat, clientLon, clientBusStopIndex, busId);
        clientSubscriptions.put(sessionId, subscription);
    }
    
    /**
     * Remove all subscriptions for a disconnected client
     */
    public void removeClientSubscriptions(String sessionId) {
        // Record disconnection metrics
        metricsService.recordWebSocketDisconnection(sessionId);
        
        // Remove from route-based subscriptions
        activeSubscriptions.values().forEach(subscribers -> subscribers.remove(sessionId));
        activeSubscriptions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // Remove from specific bus subscriptions
        specificBusSubscriptions.values().forEach(subscribers -> subscribers.remove(sessionId));
        specificBusSubscriptions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
        
        // Remove client subscription details
        clientSubscriptions.remove(sessionId);
        
        logger.info("Removed all subscriptions for client {}", sessionId);
    }
    
    /**
     * Get current bus location from Redis
     */
    private BusLocation getCurrentBusLocation(String busNumber, String direction) {
        Set<String> keys = redisTemplate.keys(BUS_LOCATION_KEY + "*");
        if (keys != null) {
            for (String key : keys) {
                BusLocation location = (BusLocation) redisTemplate.opsForValue().get(key);
                if (location != null && 
                    busNumber.equalsIgnoreCase(location.getBusNumber()) && 
                    direction.equalsIgnoreCase(location.getTripDirection())) {
                    return location;
                }
            }
        }
        return null;
    }
    
    /**
     * Get bus location by bus ID
     */
    private BusLocation getBusLocationById(String busId) {
        return (BusLocation) redisTemplate.opsForValue().get(BUS_LOCATION_KEY + busId);
    }
    
    /**
     * Broadcast bus location update to all subscribed clients
     * Implements "Shadow Bus" strategy - sends selected bus data to requested route topics
     */
    public void broadcastBusUpdate(BusLocation location) {
        long startTime = System.currentTimeMillis();
        int totalSubscribers = 0;
        
        if (location.getBusId() == null) {
            logger.warn("Cannot broadcast: busId is null");
            return;
        }
        
        // SHADOW BUS STRATEGY: Check if this bus is selected for any client subscriptions
        // and send its data to the route topics they subscribed to
        for (ClientSubscription clientSub : clientSubscriptions.values()) {
            if (location.getBusId().equals(clientSub.getBusId())) {
                // This bus is selected for a client - send data to their requested route topic
                String requestedTopic = "/topic/bus/" + clientSub.getBusNumber() + "_" + clientSub.getDirection();
                
                // Create enhanced location data with fallback information
                Map<String, Object> enhancedLocation = new HashMap<>();
                enhancedLocation.put("busId", location.getBusId());
                enhancedLocation.put("busNumber", location.getBusNumber());
                enhancedLocation.put("lat", location.getLat());
                enhancedLocation.put("lon", location.getLon());
                enhancedLocation.put("speedKmh", location.getSpeedKmh());
                enhancedLocation.put("tripDirection", location.getTripDirection());
                enhancedLocation.put("timestamp", location.getTimestamp());
                enhancedLocation.put("busStopIndex", location.getBusStopIndex());
                
                // Add fallback information if the selected bus direction differs from requested
                boolean isFallback = !location.getTripDirection().equalsIgnoreCase(clientSub.getDirection());
                if (isFallback) {
                    enhancedLocation.put("isFallback", true);
                    enhancedLocation.put("requestedDirection", clientSub.getDirection());
                    enhancedLocation.put("actualDirection", location.getTripDirection());
                    logger.info("SHADOW BUS: Sending fallback data - client requested {} but selected bus {} is running {}", 
                               clientSub.getDirection(), location.getBusId(), location.getTripDirection());
                } else {
                    enhancedLocation.put("isFallback", false);
                }
                
                logger.info("SHADOW BUS: Broadcasting {} data for bus {} to requested topic {} (session: {})", 
                           isFallback ? "FALLBACK" : "DIRECT", location.getBusId(), requestedTopic, clientSub.getSessionId());
                
                // Send to the requested route topic, not the bus ID topic
                messagingTemplate.convertAndSend(requestedTopic, enhancedLocation);
                totalSubscribers++;
            }
        }
        
        // Also broadcast to route-based subscribers (for backward compatibility)
        if (location.getBusNumber() != null && location.getTripDirection() != null) {
            String subscriptionKey = location.getBusNumber() + "_" + location.getTripDirection();

            // Lookup by canonical key (lowercase) to find all original subscription keys
            String canonical = subscriptionKey.toLowerCase();
            Set<String> originals = canonicalToOriginalKeys.get(canonical);
            if (originals != null && !originals.isEmpty()) {
                for (String activeKey : originals) {
                    Set<String> routeSubscribers = activeSubscriptions.get(activeKey);
                    if (routeSubscribers != null && !routeSubscribers.isEmpty()) {
                        logger.info("Broadcasting update for route {} {} to {} subscribers (matched key: {})", 
                                    location.getBusNumber(), location.getTripDirection(), routeSubscribers.size(), activeKey);
                        logger.debug("Route subscribers: {}", routeSubscribers);
                        String destination = "/topic/bus/" + activeKey; // send to the stored key so clients receive it
                        logger.debug("Broadcasting to destination: {}", destination);
                        messagingTemplate.convertAndSend(destination, location);
                        totalSubscribers += routeSubscribers.size();
                    }
                }
            } else {
                logger.debug("No route subscribers found for key: {}", subscriptionKey);
                logger.debug("Active subscriptions: {}", activeSubscriptions.keySet());
            }
        }
        
        // Record broadcast metrics
        long broadcastTime = System.currentTimeMillis() - startTime;
        metricsService.recordWebSocketBroadcast(location.getBusId(), totalSubscribers, broadcastTime);
    }

    /**
     * Broadcast that a bus went offline so clients can drop it immediately.
     */
    public void broadcastBusOffline(BusLocation offline) {
        if (offline == null) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "bus-offline");
        payload.put("busId", offline.getBusId());
        payload.put("busNumber", offline.getBusNumber());
        payload.put("direction", offline.getTripDirection());

        // Notify specific bus subscribers
        if (offline.getBusId() != null) {
            Set<String> specificSubscribers = specificBusSubscriptions.get(offline.getBusId());
            if (specificSubscribers != null && !specificSubscribers.isEmpty()) {
                for (String sessionId : specificSubscribers) {
                    String destination = "/topic/bus/" + offline.getBusId();
                    messagingTemplate.convertAndSendToUser(sessionId, destination, payload);
                }
            }
        }

        // Notify route-based subscribers (same channels we use for updates)
        if (offline.getBusNumber() != null && offline.getTripDirection() != null) {
            String subscriptionKey = offline.getBusNumber() + "_" + offline.getTripDirection();
            String canonical = subscriptionKey.toLowerCase();
            Set<String> originals = canonicalToOriginalKeys.get(canonical);
            if (originals != null && !originals.isEmpty()) {
                for (String activeKey : originals) {
                    Set<String> routeSubscribers = activeSubscriptions.get(activeKey);
                    if (routeSubscribers != null && !routeSubscribers.isEmpty()) {
                        messagingTemplate.convertAndSend("/topic/bus/" + activeKey, payload);
                    }
                }
            }
        }
    }
    
    /**
     * Scheduled task to periodically check for bus updates and broadcast to subscribers
     * This ensures clients get updates even if they miss the real-time broadcast
     */
    @Scheduled(fixedRate = 5000) // Check every 5 seconds
    public void broadcastPeriodicUpdates() {
        for (Map.Entry<String, Set<String>> entry : activeSubscriptions.entrySet()) {
            String[] parts = entry.getKey().split("_", 2);
            if (parts.length == 2) {
                String busNumber = parts[0];
                String direction = parts[1];
                
                BusLocation currentLocation = getCurrentBusLocation(busNumber, direction);
                if (currentLocation != null) {
                    messagingTemplate.convertAndSend("/topic/bus/" + entry.getKey(), currentLocation);
                }
            }
        }
    }
    
    /**
     * Inner class to store client subscription details
     */
    private static class ClientSubscription {
        private final String sessionId;
        private final String busNumber;
        private final String direction;
        private final double clientLat;
        private final double clientLon;
        private final int clientBusStopIndex;
        private final String busId;
        
        public ClientSubscription(String sessionId, String busNumber, String direction, 
                                double clientLat, double clientLon, int clientBusStopIndex, String busId) {
            this.sessionId = sessionId;
            this.busNumber = busNumber;
            this.direction = direction;
            this.clientLat = clientLat;
            this.clientLon = clientLon;
            this.clientBusStopIndex = clientBusStopIndex;
            this.busId = busId;
        }
        
        // Getters
        public String getSessionId() { return sessionId; }
        public String getBusNumber() { return busNumber; }
        public String getDirection() { return direction; }
        public double getClientLat() { return clientLat; }
        public double getClientLon() { return clientLon; }
        public int getClientBusStopIndex() { return clientBusStopIndex; }
        public String getBusId() { return busId; }
    }
} 