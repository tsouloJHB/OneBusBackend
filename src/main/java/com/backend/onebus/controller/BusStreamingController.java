package com.backend.onebus.controller;

import com.backend.onebus.service.BusStreamingService;
import com.backend.onebus.service.BusSelectionService;
import com.backend.onebus.service.MetricsService;
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
            String selectedBusId = busSelectionService.selectBestBusForClient(
                busNumber, direction, clientLat, clientLon, clientBusStopIndex);
            
            if (selectedBusId != null) {
                streamingService.subscribeToSpecificBus(sessionId, selectedBusId);
                streamingService.storeClientSubscription(sessionId, busNumber, direction, 
                                                       clientLat, clientLon, clientBusStopIndex, selectedBusId);
                
                // Record smart bus selection metrics
                metricsService.recordSmartBusSelection(sessionId, direction, selectedBusId, false);
                
                logger.info("Smart bus selection for session {}: selected bus {} for route {} {}", 
                           sessionId, selectedBusId, busNumber, direction);
                
                Map<String, Object> response = Map.of(
                    "status", "success",
                    "message", "Subscribed to best bus " + selectedBusId + " for route " + busNumber + " " + direction,
                    "busNumber", busNumber,
                    "direction", direction,
                    "selectedBusId", selectedBusId,
                    "selectionType", "smart",
                    "sessionId", sessionId
                );
                logger.info("Sending subscription response to session {}: {}", sessionId, response);
                return response;
            } else {
                // Fallback to traditional subscription if no suitable bus found
                streamingService.subscribeToBus(sessionId, busNumber, direction);
                logger.info("No suitable bus found, using traditional subscription for session {} to bus {} direction {}", 
                           sessionId, busNumber, direction);
                return Map.of(
                    "status", "success",
                    "message", "Subscribed to bus " + busNumber + " " + direction + " (no specific bus available)",
                    "busNumber", busNumber,
                    "direction", direction,
                    "selectionType", "traditional"
                );
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
} 