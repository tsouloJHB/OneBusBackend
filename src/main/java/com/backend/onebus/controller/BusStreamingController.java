package com.backend.onebus.controller;

import com.backend.onebus.service.BusStreamingService;
import com.backend.onebus.service.BusSelectionService;
import com.backend.onebus.service.MetricsService;
import com.backend.onebus.service.routing.BusCompanyStrategyFactory;
import com.backend.onebus.service.routing.BusCompanyRoutingStrategy;
import com.backend.onebus.repository.BusNumberRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Hidden;

import java.util.HashMap;
import java.util.Map;

import java.util.Map;

@Controller
@Tag(name = "Bus Streaming", description = "WebSocket endpoints for real-time bus location streaming")
public class BusStreamingController {
    
    private static final Logger logger = LoggerFactory.getLogger(BusStreamingController.class);
    
    @Autowired
    private BusStreamingService streamingService;
    
    @Autowired
    private BusSelectionService busSelectionService;
    
    @Autowired
    private MetricsService metricsService;
    
    @Autowired
    private BusCompanyStrategyFactory strategyFactory;
    
    @Autowired
    private BusNumberRepository busNumberRepository;
    
    /**
     * Handle subscription request from client with location and bus stop index
     */
    @MessageMapping("/subscribe")
    @SendTo("/topic/bus/subscription-status")
    @Hidden
    public Map<String, Object> subscribeToBus(
            @Payload Map<String, Object> subscriptionRequest,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        logger.info("========================================");
        logger.info("SUBSCRIPTION REQUEST RECEIVED");
        logger.info("Session ID: {}", sessionId);
        logger.info("Request: {}", subscriptionRequest);
        logger.info("Headers: {}", headerAccessor.getSessionAttributes());
        logger.info("========================================");
        
        String busNumber = (String) subscriptionRequest.get("busNumber");
        String direction = (String) subscriptionRequest.get("direction");
        Double clientLat = (Double) subscriptionRequest.get("latitude");
        Double clientLon = (Double) subscriptionRequest.get("longitude");
        Integer clientBusStopIndex = (Integer) subscriptionRequest.get("busStopIndex");
        
        if (busNumber == null || direction == null) {
            logger.warn("Invalid subscription request from session {}: missing busNumber or direction", sessionId);
            return Map.of(
                "status", "error",
                "message", "Missing busNumber or direction",
                "sessionId", sessionId
            );
        }
        
        // Use smart bus selection if client provides location and bus stop index
        if (clientLat != null && clientLon != null && clientBusStopIndex != null) {
            // Get company name for this bus number to determine routing strategy
            String companyName = getCompanyNameForBusNumber(busNumber);
            BusCompanyRoutingStrategy strategy = strategyFactory.getStrategy(companyName);
            
            String selectedBusId = null;
            
            // Check if this company supports smart bus selection
            if (strategy.supportsSmartBusSelection()) {
                logger.info("Using company-specific smart bus selection for {} ({})", companyName, busNumber);
                selectedBusId = strategy.selectBestBusForClient(busNumber, direction, clientLat, clientLon, clientBusStopIndex);
            } else {
                logger.info("Company {} does not support smart bus selection, using traditional subscription", companyName);
                // Fall back to traditional subscription for companies that don't support smart selection
            }
            
            if (selectedBusId != null) {
                streamingService.subscribeToSpecificBus(sessionId, selectedBusId);
                // CRITICAL: Also register for route-based subscription so Shadow Bus strategy works
                streamingService.subscribeToBus(sessionId, busNumber, direction);
                streamingService.storeClientSubscription(sessionId, busNumber, direction, 
                                                       clientLat, clientLon, clientBusStopIndex, selectedBusId);
                
                // Record smart bus selection metrics
                metricsService.recordSmartBusSelection(sessionId, direction, selectedBusId, false);
                
                logger.info("Smart bus selection for session {}: selected bus {} for route {} {} using {} strategy", 
                           sessionId, selectedBusId, busNumber, direction, companyName);
                
                Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "Subscribed to best bus " + selectedBusId + " for route " + busNumber + " " + direction,
                    "busNumber", busNumber,
                    "direction", direction,
                    "selectedBusId", selectedBusId,
                    "selectionType", "smart",
                    "companyStrategy", companyName,
                    "sessionId", sessionId
                );
                logger.info("Sending subscription response to session {}: {}", sessionId, response);
                return response;
            } else {
                // Fallback to traditional subscription if no suitable bus found
                streamingService.subscribeToBus(sessionId, busNumber, direction);
                logger.info("No suitable bus found, using traditional subscription for session {} to bus {} direction {}", 
                           sessionId, busNumber, direction);
                
                Map<String, Object> response = new HashMap<>();
                response.put("status", "success");
                response.put("message", "Subscribed to bus " + busNumber + " " + direction + " (no specific bus available)");
                response.put("busNumber", busNumber);
                response.put("direction", direction);
                response.put("selectionType", "traditional");
                response.put("companyStrategy", companyName); // Can be null
                response.put("sessionId", sessionId);
                return response;
            }
        } else {
            // Traditional subscription without location
            streamingService.subscribeToBus(sessionId, busNumber, direction);
            logger.info("Traditional subscription for session {} to bus {} direction {}", sessionId, busNumber, direction);
            return Map.of(
                "status", "success",
                "message", "Subscribed to bus " + busNumber + " " + direction,
                "busNumber", busNumber,
                "direction", direction,
                "selectionType", "traditional"
            );
        }
    }
    
    /**
     * Handle manual cleanup request from client (when bus arrives or user stops tracking)
     */
    @MessageMapping("/cleanup")
    @SendToUser("/topic/subscription/status")
    public Map<String, Object> cleanupSession(SimpMessageHeaderAccessor headerAccessor) {
        String sessionId = headerAccessor.getSessionId();
        logger.info("Manual cleanup requested for session {}", sessionId);
        
        // Remove all subscriptions for this session
        streamingService.removeClientSubscriptions(sessionId);
        
        return Map.of(
            "status", "success",
            "message", "Session cleaned up successfully",
            "sessionId", sessionId
        );
    }
    
    /**
     * Handle unsubscription request from client
     */
    @MessageMapping("/unsubscribe")
    @SendToUser("/topic/subscription/status")
    public Map<String, Object> unsubscribeFromBus(
            @Payload Map<String, String> unsubscriptionRequest,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        String busNumber = unsubscriptionRequest.get("busNumber");
        String direction = unsubscriptionRequest.get("direction");
        String busId = unsubscriptionRequest.get("busId");
        
        if (busId != null) {
            // Unsubscribe from specific bus
            streamingService.unsubscribeFromSpecificBus(sessionId, busId);
            logger.info("Unsubscription successful for session {} from specific bus {}", sessionId, busId);
            return Map.of(
                "status", "success",
                "message", "Unsubscribed from specific bus " + busId,
                "busId", busId
            );
        } else if (busNumber != null && direction != null) {
            // Traditional unsubscription
            streamingService.unsubscribeFromBus(sessionId, busNumber, direction);
            logger.info("Unsubscription successful for session {} from bus {} direction {}", sessionId, busNumber, direction);
            return Map.of(
                "status", "success",
                "message", "Unsubscribed from bus " + busNumber + " " + direction,
                "busNumber", busNumber,
                "direction", direction
            );
        } else {
            logger.warn("Invalid unsubscription request from session {}: missing busId or busNumber/direction", sessionId);
            return Map.of(
                "status", "error",
                "message", "Missing busId or busNumber/direction"
            );
        }
    }

    /**
     * Test endpoint to send a test message to a specific session
     */
    @MessageMapping("/test")
    @SendToUser("/topic/test")
    public Map<String, Object> sendTestMessage(
            @Payload Map<String, String> testRequest,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        String message = testRequest.get("message") != null ? testRequest.get("message") : "Hello from server!";
        
        logger.info("Test message received from session {}: {}", sessionId, message);
        logger.info("Test message will be sent to session {} at destination: /topic/test", sessionId);
        
        return Map.of(
            "status", "success",
            "message", message,
            "sessionId", sessionId,
            "timestamp", new java.util.Date().toString()
        );
    }
    
    /**
     * Helper method to get company name for a bus number.
     * This is used to determine which routing strategy to use for smart bus selection.
     */
    private String getCompanyNameForBusNumber(String busNumber) {
        try {
            // Find any active bus number entry for this bus number
            // Note: A bus number might exist for multiple companies, but we'll take the first active one
            var busNumbers = busNumberRepository.findByIsActiveTrue();
            
            for (var busNumberEntity : busNumbers) {
                if (busNumber.equalsIgnoreCase(busNumberEntity.getBusNumber())) {
                    String companyName = busNumberEntity.getBusCompany().getName();
                    logger.debug("Found company '{}' for bus number '{}'", companyName, busNumber);
                    return companyName;
                }
            }
            
            logger.warn("No company found for bus number '{}', using default strategy", busNumber);
            return null; // Will use default strategy
            
        } catch (Exception e) {
            logger.error("Error getting company name for bus number '{}': {}", busNumber, e.getMessage());
            return null; // Will use default strategy
        }
    }
} 