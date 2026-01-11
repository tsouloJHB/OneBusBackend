package com.backend.onebus.repository;

import com.backend.onebus.model.Bus;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BusRepository extends JpaRepository<Bus, String> {
    @Cacheable(value = "buses", key = "#trackerImei", unless = "#result == null")
    @Query("SELECT b FROM Bus b LEFT JOIN FETCH b.busCompany WHERE b.trackerImei = :trackerImei")
    Bus findByTrackerImei(@Param("trackerImei") String trackerImei);
    
    /**
     * Find all buses by company name
     */
    List<Bus> findByBusCompanyName(String busCompanyName);
    
    /**
     * Find all buses by company name (case-insensitive)
     */
    @Query("SELECT b FROM Bus b WHERE LOWER(b.busCompanyName) = LOWER(:companyName)")
    List<Bus> findByBusCompanyNameIgnoreCase(@Param("companyName") String busCompanyName);
    
    /**
     * Find all buses by company name containing the given string (case-insensitive)
     */
    @Query("SELECT b FROM Bus b WHERE LOWER(b.busCompanyName) LIKE LOWER(CONCAT('%', :companyName, '%'))")
    List<Bus> findByBusCompanyNameContainingIgnoreCase(@Param("companyName") String busCompanyName);
}