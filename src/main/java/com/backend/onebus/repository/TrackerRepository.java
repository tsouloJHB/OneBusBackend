package com.backend.onebus.repository;

import com.backend.onebus.model.Tracker;
import com.backend.onebus.model.BusCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TrackerRepository extends JpaRepository<Tracker, Long> {
    
    /**
     * Find tracker by IMEI number
     */
    Optional<Tracker> findByImei(String imei);
    
    /**
     * Check if tracker with IMEI exists
     */
    boolean existsByImei(String imei);
    
    /**
     * Find all trackers by company
     */
    List<Tracker> findByCompany(BusCompany company);
    
    /**
     * Find all trackers by company ID
     */
    List<Tracker> findByCompanyId(Long companyId);
    
    /**
     * Find all trackers by status
     */
    List<Tracker> findByStatus(Tracker.TrackerStatus status);
    
    /**
     * Find all trackers by company and status
     */
    List<Tracker> findByCompanyIdAndStatus(Long companyId, Tracker.TrackerStatus status);
    
    /**
     * Find available trackers (not assigned to any bus)
     */
    @Query("SELECT t FROM Tracker t WHERE t.status = 'AVAILABLE' AND t.assignedBus IS NULL")
    List<Tracker> findAvailableTrackers();
    
    /**
     * Find available trackers for a specific company
     */
    @Query("SELECT t FROM Tracker t WHERE t.company.id = :companyId AND t.status = 'AVAILABLE' AND t.assignedBus IS NULL")
    List<Tracker> findAvailableTrackersByCompanyId(@Param("companyId") Long companyId);
    
    /**
     * Find trackers by brand
     */
    List<Tracker> findByBrandContainingIgnoreCase(String brand);
    
    /**
     * Find trackers by model
     */
    List<Tracker> findByModelContainingIgnoreCase(String model);
    
    /**
     * Search trackers by IMEI, brand, or model
     */
    @Query("SELECT t FROM Tracker t WHERE " +
           "LOWER(t.imei) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.brand) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.model) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Tracker> searchTrackers(@Param("searchTerm") String searchTerm);
    
    /**
     * Count trackers by company
     */
    long countByCompanyId(Long companyId);
    
    /**
     * Count trackers by status
     */
    long countByStatus(Tracker.TrackerStatus status);
    
    /**
     * Count trackers by status and company
     */
    long countByStatusAndCompanyId(Tracker.TrackerStatus status, Long companyId);

    /**
     * Count available trackers for a company
     */
    @Query("SELECT COUNT(t) FROM Tracker t WHERE t.company.id = :companyId AND t.status = 'AVAILABLE' AND t.assignedBus IS NULL")
    long countAvailableTrackersByCompanyId(@Param("companyId") Long companyId);

    /**
     * Search trackers within a company
     */
    @Query("SELECT t FROM Tracker t WHERE t.company.id = :companyId AND (" +
           "LOWER(t.imei) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.brand) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(t.model) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    List<Tracker> searchTrackersByCompanyId(@Param("searchTerm") String searchTerm, @Param("companyId") Long companyId);
}
