package com.backend.onebus.repository;

import com.backend.onebus.model.Driver;
import com.backend.onebus.model.Driver.DriverStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface DriverRepository extends JpaRepository<Driver, Long> {
    
    /**
     * Find driver by driver ID
     */
    Optional<Driver> findByDriverId(String driverId);
    
    /**
     * Find driver by email
     */
    Optional<Driver> findByEmail(String email);
    
    /**
     * Check if driver ID exists
     */
    boolean existsByDriverId(String driverId);
    
    /**
     * Check if email exists
     */
    boolean existsByEmail(String email);
    
    /**
     * Find all drivers by status
     */
    List<Driver> findByStatus(DriverStatus status);
    
    /**
     * Find all drivers by company ID
     */
    @Query("SELECT d FROM Driver d WHERE d.company.id = :companyId")
    List<Driver> findByCompanyId(@Param("companyId") Long companyId);
    
    /**
     * Find all active drivers
     */
    @Query("SELECT d FROM Driver d WHERE d.status = 'ACTIVE'")
    List<Driver> findAllActiveDrivers();
    
    /**
     * Find drivers by company ID and status
     */
    @Query("SELECT d FROM Driver d WHERE d.company.id = :companyId AND d.status = :status")
    List<Driver> findByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") DriverStatus status);
    
    /**
     * Search drivers by name (case-insensitive)
     */
    @Query("SELECT d FROM Driver d WHERE LOWER(d.fullName) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<Driver> searchByName(@Param("name") String name);
    
    /**
     * Find all drivers registered by admin
     */
    @Query("SELECT d FROM Driver d WHERE d.isRegisteredByAdmin = true")
    List<Driver> findAllRegisteredByAdmin();
    
    /**
     * Count active drivers
     */
    @Query("SELECT COUNT(d) FROM Driver d WHERE d.status = 'ACTIVE'")
    long countActiveDrivers();
    
    /**
     * Count drivers by company
     */
    @Query("SELECT COUNT(d) FROM Driver d WHERE d.company.id = :companyId")
    long countByCompanyId(@Param("companyId") Long companyId);
}
