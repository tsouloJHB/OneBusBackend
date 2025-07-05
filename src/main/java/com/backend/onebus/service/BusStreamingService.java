package com.backend.onebus.service;

import com.backend.onebus.model.BusLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class BusStreamingService {
    
    private static final Logger logger = LoggerFactory.getLogger(BusStreamingService.class);
    private static final String BUS_LOCATION_KEY = "bus:location:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    @Autowired
    private SimpMessagingTemplate messagingTemplate;
    
    // Track active subscriptions: Map<busNumber_direction, Set<sessionId>>
    private final Map<String, Set<String>> activeSubscriptions = new ConcurrentHashMap<>();
    
    /**
     * Subscribe a client to a specific bus and direction
     */
    public void subscribeToBus(String sessionId, String busNumber, String direction) {
        String subscriptionKey = busNumber + "_" + direction;
        activeSubscriptions.computeIfAbsent(subscriptionKey, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
        logger.info("Client {} subscribed to bus {} direction {}", sessionId, busNumber, direction);
        
        // Send current location immediately if available
        BusLocation currentLocation = getCurrentBusLocation(busNumber, direction);
        if (currentLocation != null) {
            messagingTemplate.convertAndSendToUser(sessionId, "/topic/bus/" + subscriptionKey, currentLocation);
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
            }
        }
        logger.info("Client {} unsubscribed from bus {} direction {}", sessionId, busNumber, direction);
    }
    
    /**
     * Remove all subscriptions for a disconnected client
     */
    public void removeClientSubscriptions(String sessionId) {
        activeSubscriptions.values().forEach(subscribers -> subscribers.remove(sessionId));
        activeSubscriptions.entrySet().removeIf(entry -> entry.getValue().isEmpty());
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
                    busNumber.equals(location.getBusNumber()) && 
                    direction.equals(location.getTripDirection())) {
                    return location;
                }
            }
        }
        return null;
    }
    
    /**
     * Broadcast bus location update to all subscribed clients
     */
    public void broadcastBusUpdate(BusLocation location) {
        if (location.getBusNumber() == null || location.getTripDirection() == null) {
            logger.warn("Cannot broadcast: busNumber or tripDirection is null");
            return;
        }
        
        String subscriptionKey = location.getBusNumber() + "_" + location.getTripDirection();
        Set<String> subscribers = activeSubscriptions.get(subscriptionKey);
        
        if (subscribers != null && !subscribers.isEmpty()) {
            logger.info("Broadcasting update for bus {} direction {} to {} subscribers", 
                        location.getBusNumber(), location.getTripDirection(), subscribers.size());
            
            for (String sessionId : subscribers) {
                String destination = "/topic/bus/" + subscriptionKey;
                logger.debug("Sending to session {} at destination: {}", sessionId, destination);
                messagingTemplate.convertAndSend(destination, location);
            }
        } else {
            logger.warn("No subscribers found for bus {} direction {}", 
                       location.getBusNumber(), location.getTripDirection());
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
                    for (String sessionId : entry.getValue()) {
                        messagingTemplate.convertAndSendToUser(sessionId, "/topic/bus/" + entry.getKey(), currentLocation);
                    }
                }
            }
        }
    }
} 