package com.backend.onebus.controller;

import com.backend.onebus.model.DashboardStats;
import com.backend.onebus.model.User;
import com.backend.onebus.repository.UserRepository;
import com.backend.onebus.service.DashboardStatsService;
import com.backend.onebus.service.BusTrackingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/dashboard")
@Tag(name = "Dashboard", description = "Dashboard statistics API")
@CrossOrigin
public class DashboardController {

    @Autowired
    private DashboardStatsService dashboardStatsService;

    @Autowired
    private BusTrackingService busTrackingService;

    @Autowired
    private UserRepository userRepository;

    /**
     * Get dashboard statistics
     * Returns pre-calculated counts for quick dashboard loading
     * For fleet managers, only returns data for their company
     */
    @GetMapping("/stats")
    @Operation(summary = "Get dashboard statistics", 
               description = "Returns optimized dashboard statistics including total counts and percentage changes")
    public ResponseEntity<Map<String, Object>> getDashboardStats(
            @RequestParam(required = false) Long companyId,
            HttpServletRequest request) {
        
        // Get user info from request attributes (set by JWT filter)
        String userEmail = (String) request.getAttribute("userEmail");
        String userRole = (String) request.getAttribute("userRole");
        
        // If user is fleet manager, override companyId with their company
        if ("FLEET_MANAGER".equals(userRole) && userEmail != null) {
            User user = userRepository.findByEmail(userEmail).orElse(null);
            if (user != null && user.getCompanyId() != null) {
                companyId = user.getCompanyId();
            }
        }
        
        Map<String, Object> stats = dashboardStatsService.getStatsMap(companyId);
        
        // Add active buses count (from in-memory cache, not DB)
        long activeBusesCount = (companyId != null) ? 
            busTrackingService.getActiveBusesCountForCompany(companyId) : 
            busTrackingService.getActiveBusesCount();
        stats.put("activeBuses", activeBusesCount);
        
        return ResponseEntity.ok(stats);
    }

    /**
     * Initialize/reset dashboard statistics
     * Admin endpoint to recalculate stats from actual database counts
     */
    @PostMapping("/stats/initialize")
    @Operation(summary = "Initialize dashboard statistics", 
               description = "Recalculates all statistics from actual database counts. Use when setting up system or if stats become inaccurate.")
    public ResponseEntity<DashboardStats> initializeStats() {
        DashboardStats stats = dashboardStatsService.initializeStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * Take monthly snapshot
     * Should be called at the beginning of each month to store previous month's counts
     */
    @PostMapping("/stats/snapshot")
    @Operation(summary = "Take monthly snapshot", 
               description = "Stores current counts as 'last month' values for percentage calculations")
    public ResponseEntity<DashboardStats> takeSnapshot() {
        DashboardStats stats = dashboardStatsService.takeMonthlySnapshot();
        return ResponseEntity.ok(stats);
    }

    /**
     * Get detailed stats including active buses breakdown
     */
    @GetMapping("/stats/detailed")
    @Operation(summary = "Get detailed statistics", 
               description = "Returns detailed dashboard statistics with breakdown by company and status")
    public ResponseEntity<Map<String, Object>> getDetailedStats() {
        Map<String, Object> detailedStats = new HashMap<>();
        
        // Get base stats (null = all companies for admin)
        Map<String, Object> baseStats = dashboardStatsService.getStatsMap(null);
        detailedStats.putAll(baseStats);
        
        // Add active buses info
        long activeBusesCount = busTrackingService.getActiveBusesCount();
        detailedStats.put("activeBuses", activeBusesCount);
        
        // Could add more detailed breakdowns here in future
        // e.g., busesPerCompany, routesPerCompany, etc.
        
        return ResponseEntity.ok(detailedStats);
    }
}
