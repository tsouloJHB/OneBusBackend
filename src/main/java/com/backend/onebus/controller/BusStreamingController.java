package com.backend.onebus.controller;

import com.backend.onebus.service.BusStreamingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class BusStreamingController {
    
    private static final Logger logger = LoggerFactory.getLogger(BusStreamingController.class);
    
    @Autowired
    private BusStreamingService streamingService;
    
    /**
     * Handle subscription request from client
     */
    @MessageMapping("/subscribe")
    @SendToUser("/topic/subscription/status")
    public Map<String, Object> subscribeToBus(
            @Payload Map<String, String> subscriptionRequest,
            SimpMessageHeaderAccessor headerAccessor) {
        
        String sessionId = headerAccessor.getSessionId();
        String busNumber = subscriptionRequest.get("busNumber");
        String direction = subscriptionRequest.get("direction");
        
        if (busNumber == null || direction == null) {
            logger.warn("Invalid subscription request from session {}: missing busNumber or direction", sessionId);
            return Map.of(
                "status", "error",
                "message", "Missing busNumber or direction"
            );
        }
        
        streamingService.subscribeToBus(sessionId, busNumber, direction);
        
        logger.info("Subscription successful for session {} to bus {} direction {}", sessionId, busNumber, direction);
        return Map.of(
            "status", "success",
            "message", "Subscribed to bus " + busNumber + " " + direction,
            "busNumber", busNumber,
            "direction", direction
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
        
        if (busNumber == null || direction == null) {
            logger.warn("Invalid unsubscription request from session {}: missing busNumber or direction", sessionId);
            return Map.of(
                "status", "error",
                "message", "Missing busNumber or direction"
            );
        }
        
        streamingService.unsubscribeFromBus(sessionId, busNumber, direction);
        
        logger.info("Unsubscription successful for session {} from bus {} direction {}", sessionId, busNumber, direction);
        return Map.of(
            "status", "success",
            "message", "Unsubscribed from bus " + busNumber + " " + direction,
            "busNumber", busNumber,
            "direction", direction
        );
    }
} 