package com.backend.onebus.service.routing;

import com.backend.onebus.model.BusLocation;
import com.backend.onebus.model.Route;
import com.backend.onebus.model.RouteStop;

import java.util.List;

/**
 * Rea Vaya routing strategy.
 * 
 * Rea Vaya is a bus rapid transit system with well-defined start/end stops.
 * The strategy detects when buses are at these terminal points and automatically
 * switches direction, ensuring buses always complete their full route loops.
 * 
 * Rules:
 * 1. When bus is at the start stop of current direction → Direction = current
 * 2. When bus is at the end stop of current direction → Direction = opposite (switch)
 * 3. During route progression → Update busStopIndex based on proximity to stops
 */
public class ReaVayaRoutingStrategy extends BusCompanyRoutingStrategy {
    
    @Override
    public String getCompanyName() {
        return "Rea Vaya";
    }
    
    /**
     * Infer direction for Rea Vaya buses based on terminal point detection.
     * 
     * Logic:
     * 1. Check if bus is at the start stop (if so, confirm current direction)
     * 2. Check if bus is at the end stop (if so, switch direction)
     * 3. For intermediate positions, keep last known direction
     */
    @Override
    public String inferDirection(BusLocation current, BusLocation previous, Route route) {
        if (route == null || current.getTripDirection() == null) {
            return null;
        }
        
        String currentDirection = current.getTripDirection();
        
        // Check if bus is at the end stop - should switch direction
        if (isAtEndStop(current, route, currentDirection)) {
            String newDirection = switchDirection(currentDirection);
            logger.info("[Rea Vaya] Bus {} at end of {} route, switching to {}", 
                current.getBusNumber(), currentDirection, newDirection);
            return newDirection;
        }
        
        // Check if bus is at the start stop - confirm direction
        if (isAtStartStop(current, route, currentDirection)) {
            logger.info("[Rea Vaya] Bus {} at start of {} route", 
                current.getBusNumber(), currentDirection);
            return currentDirection;
        }
        
        // For Rea Vaya, maintain the current direction during normal operation
        // Global rules will handle automatic direction switches when needed
        return null; // Return null to indicate no change needed
    }
    
    /**
     * Handle end-of-route logic for Rea Vaya.
     * When a bus reaches the end of its route, it switches direction to return.
     */
    @Override
    public String handleEndOfRoute(BusLocation current, Route route) {
        if (current.getTripDirection() != null) {
            String newDirection = switchDirection(current.getTripDirection());
            logger.info("[Rea Vaya] End-of-route handler: {} → {}", 
                current.getTripDirection(), newDirection);
            return newDirection;
        }
        return null;
    }
}
