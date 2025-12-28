package com.backend.onebus.repository;

import com.backend.onebus.model.DashboardStats;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface DashboardStatsRepository extends JpaRepository<DashboardStats, Long> {

    /**
     * Get the single stats record (there should only be one row)
     */
    @Query("SELECT d FROM DashboardStats d ORDER BY d.id DESC")
    Optional<DashboardStats> findLatest();
}
