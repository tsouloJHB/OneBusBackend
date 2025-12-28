package com.backend.onebus.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Entity to store pre-calculated dashboard statistics
 * This avoids expensive counting queries on every dashboard load
 */
@Entity
@Table(name = "dashboard_stats")
public class DashboardStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_routes", nullable = false)
    private Long totalRoutes = 0L;

    @Column(name = "total_buses", nullable = false)
    private Long totalBuses = 0L;

    @Column(name = "total_users", nullable = false)
    private Long totalUsers = 0L;

    @Column(name = "total_companies", nullable = false)
    private Long totalCompanies = 0L;

    @Column(name = "total_trackers", nullable = false)
    private Long totalTrackers = 0L;

    // Previous month counts for percentage calculations
    @Column(name = "routes_last_month")
    private Long routesLastMonth = 0L;

    @Column(name = "buses_last_month")
    private Long busesLastMonth = 0L;

    @Column(name = "users_last_month")
    private Long usersLastMonth = 0L;

    @Column(name = "last_snapshot_date")
    private LocalDateTime lastSnapshotDate;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Constructors
    public DashboardStats() {
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getTotalRoutes() {
        return totalRoutes;
    }

    public void setTotalRoutes(Long totalRoutes) {
        this.totalRoutes = totalRoutes;
    }

    public Long getTotalBuses() {
        return totalBuses;
    }

    public void setTotalBuses(Long totalBuses) {
        this.totalBuses = totalBuses;
    }

    public Long getTotalUsers() {
        return totalUsers;
    }

    public void setTotalUsers(Long totalUsers) {
        this.totalUsers = totalUsers;
    }

    public Long getTotalCompanies() {
        return totalCompanies;
    }

    public void setTotalCompanies(Long totalCompanies) {
        this.totalCompanies = totalCompanies;
    }

    public Long getTotalTrackers() {
        return totalTrackers;
    }

    public void setTotalTrackers(Long totalTrackers) {
        this.totalTrackers = totalTrackers;
    }

    public Long getRoutesLastMonth() {
        return routesLastMonth;
    }

    public void setRoutesLastMonth(Long routesLastMonth) {
        this.routesLastMonth = routesLastMonth;
    }

    public Long getBusesLastMonth() {
        return busesLastMonth;
    }

    public void setBusesLastMonth(Long busesLastMonth) {
        this.busesLastMonth = busesLastMonth;
    }

    public Long getUsersLastMonth() {
        return usersLastMonth;
    }

    public void setUsersLastMonth(Long usersLastMonth) {
        this.usersLastMonth = usersLastMonth;
    }

    public LocalDateTime getLastSnapshotDate() {
        return lastSnapshotDate;
    }

    public void setLastSnapshotDate(LocalDateTime lastSnapshotDate) {
        this.lastSnapshotDate = lastSnapshotDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    // Helper methods to increment/decrement counts
    public void incrementRoutes() {
        this.totalRoutes++;
    }

    public void decrementRoutes() {
        if (this.totalRoutes > 0) {
            this.totalRoutes--;
        }
    }

    public void incrementBuses() {
        this.totalBuses++;
    }

    public void decrementBuses() {
        if (this.totalBuses > 0) {
            this.totalBuses--;
        }
    }

    public void incrementUsers() {
        this.totalUsers++;
    }

    public void decrementUsers() {
        if (this.totalUsers > 0) {
            this.totalUsers--;
        }
    }

    public void incrementCompanies() {
        this.totalCompanies++;
    }

    public void decrementCompanies() {
        if (this.totalCompanies > 0) {
            this.totalCompanies--;
        }
    }

    public void incrementTrackers() {
        this.totalTrackers++;
    }

    public void decrementTrackers() {
        if (this.totalTrackers > 0) {
            this.totalTrackers--;
        }
    }
}
