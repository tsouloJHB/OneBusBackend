package com.backend.onebus.repository;

import com.backend.onebus.model.RegisteredBus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegisteredBusRepository extends JpaRepository<RegisteredBus, Long> {

    /**
     * Find all registered buses for a specific company
     */
    List<RegisteredBus> findByCompanyId(Long companyId);

    /**
     * Find registered buses by company ID and status
     */
    List<RegisteredBus> findByCompanyIdAndStatus(Long companyId, RegisteredBus.BusStatus status);

    /**
     * Find registered bus by registration number
     */
    RegisteredBus findByRegistrationNumber(String registrationNumber);

    /**
     * Find registered buses by bus number
     */
    List<RegisteredBus> findByBusNumber(String busNumber);

    /**
     * Find active registered buses for a company
     */
    @Query("SELECT rb FROM RegisteredBus rb WHERE rb.company.id = :companyId AND rb.status = 'ACTIVE'")
    List<RegisteredBus> findActiveByCompanyId(@Param("companyId") Long companyId);

    /**
     * Count registered buses by company
     */
    long countByCompanyId(Long companyId);

    /**
     * Find buses that have trackerImei but no tracker entity (for migration)
     * Uses JOIN FETCH to eagerly load company relationship
     */
    @Query("SELECT b FROM RegisteredBus b LEFT JOIN FETCH b.company WHERE b.trackerImei IS NOT NULL AND b.tracker IS NULL")
    List<RegisteredBus> findBusesNeedingTrackerMigration();
}