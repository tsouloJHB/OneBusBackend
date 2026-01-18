package com.backend.onebus.service;

import com.backend.onebus.model.DashboardStats;
import com.backend.onebus.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardStatsService {

    @Autowired
    private DashboardStatsRepository dashboardStatsRepository;

    @Autowired
    private RegisteredBusRepository registeredBusRepository;

    @Autowired
    private RouteRepository routeRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BusCompanyRepository busCompanyRepository;

    @Autowired
    private TrackerRepository trackerRepository;

    /**
     * Get current dashboard statistics (creates if doesn't exist)
     */
    @Transactional
    public DashboardStats getStats() {
        return dashboardStatsRepository.findLatest()
            .orElseGet(() -> dashboardStatsRepository.save(new DashboardStats()));
    }

    /**
     * Get stats as a map for easy API consumption
     */
    @Transactional
    public Map<String, Object> getStatsMap(Long companyId) {
        if (companyId == null) {
            DashboardStats stats = getStats();
            Map<String, Object> result = new HashMap<>();
            
            result.put("totalRoutes", stats.getTotalRoutes());
            result.put("totalBuses", stats.getTotalBuses());
            result.put("totalUsers", stats.getTotalUsers());
            result.put("totalCompanies", stats.getTotalCompanies());
            result.put("totalTrackers", stats.getTotalTrackers());
            
            // Calculate percentage changes
            result.put("routesChange", calculatePercentageChange(stats.getTotalRoutes(), stats.getRoutesLastMonth()));
            result.put("busesChange", calculatePercentageChange(stats.getTotalBuses(), stats.getBusesLastMonth()));
            result.put("usersChange", calculatePercentageChange(stats.getTotalUsers(), stats.getUsersLastMonth()));
            
            result.put("lastUpdated", stats.getUpdatedAt());
            result.put("lastSnapshot", stats.getLastSnapshotDate());
            
            return result;
        } else {
            // Company-specific stats (calculated on the fly)
            Map<String, Object> result = new HashMap<>();
            
            // Get company name for routes
            String companyName = busCompanyRepository.findById(companyId)
                .map(com.backend.onebus.model.BusCompany::getName)
                .orElse("");
                
            result.put("totalRoutes", routeRepository.countByCompany(companyName));
            result.put("totalBuses", registeredBusRepository.countByCompanyId(companyId));
            result.put("totalUsers", userRepository.countByCompanyId(companyId));
            result.put("totalTrackers", trackerRepository.countByCompanyId(companyId));
            
            // For company stats, we might not have MoM change easily without storage
            result.put("routesChange", 0.0);
            result.put("busesChange", 0.0);
            result.put("usersChange", 0.0);
            
            result.put("lastUpdated", LocalDateTime.now());
            
            return result;
        }
    }

    /**
     * Initialize stats from actual database counts
     * Call this once when setting up the system or to reset stats
     */
    @Transactional
    public DashboardStats initializeStats() {
        DashboardStats stats = getStats();  // Use getStats() instead of getOrCreate()
        
        stats.setTotalRoutes(routeRepository.count());
        stats.setTotalBuses(registeredBusRepository.count());
        stats.setTotalUsers(userRepository.count());
        stats.setTotalCompanies(busCompanyRepository.count());
        stats.setTotalTrackers(trackerRepository.count());
        
        return dashboardStatsRepository.save(stats);
    }

    /**
     * Take a snapshot of current counts for month-over-month comparison
     */
    @Transactional
    public DashboardStats takeMonthlySnapshot() {
        DashboardStats stats = getStats();
        
        stats.setRoutesLastMonth(stats.getTotalRoutes());
        stats.setBusesLastMonth(stats.getTotalBuses());
        stats.setUsersLastMonth(stats.getTotalUsers());
        stats.setLastSnapshotDate(LocalDateTime.now());
        
        return dashboardStatsRepository.save(stats);
    }

    // Increment/Decrement methods for each entity type
    
    @Transactional
    public void incrementRoutes() {
        DashboardStats stats = getStats();
        stats.incrementRoutes();
        dashboardStatsRepository.save(stats);
    }

    @Transactional
    public void decrementRoutes() {
        DashboardStats stats = getStats();
        stats.decrementRoutes();
        dashboardStatsRepository.save(stats);
    }

    @Transactional
    public void incrementBuses() {
        DashboardStats stats = getStats();
        stats.incrementBuses();
        dashboardStatsRepository.save(stats);
    }

    @Transactional
    public void decrementBuses() {
        DashboardStats stats = getStats();
        stats.decrementBuses();
        dashboardStatsRepository.save(stats);
    }

    @Transactional
    public void incrementUsers() {
        DashboardStats stats = getStats();
        stats.incrementUsers();
        dashboardStatsRepository.save(stats);
    }

    @Transactional
    public void decrementUsers() {
        DashboardStats stats = getStats();
        stats.decrementUsers();
        dashboardStatsRepository.save(stats);
    }

    @Transactional
    public void incrementCompanies() {
        DashboardStats stats = getStats();
        stats.incrementCompanies();
        dashboardStatsRepository.save(stats);
    }

    @Transactional
    public void decrementCompanies() {
        DashboardStats stats = getStats();
        stats.decrementCompanies();
        dashboardStatsRepository.save(stats);
    }

    @Transactional
    public void incrementTrackers() {
        DashboardStats stats = getStats();
        stats.incrementTrackers();
        dashboardStatsRepository.save(stats);
    }

    @Transactional
    public void decrementTrackers() {
        DashboardStats stats = getStats();
        stats.decrementTrackers();
        dashboardStatsRepository.save(stats);
    }

    /**
     * Calculate percentage change between current and previous value
     */
    private double calculatePercentageChange(Long current, Long previous) {
        if (previous == null || previous == 0) {
            return current > 0 ? 100.0 : 0.0;
        }
        return ((current - previous) * 100.0) / previous;
    }
}
