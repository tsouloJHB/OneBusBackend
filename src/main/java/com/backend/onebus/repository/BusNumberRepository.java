package com.backend.onebus.repository;

import com.backend.onebus.model.BusNumber;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusNumberRepository extends JpaRepository<BusNumber, Long> {
    List<BusNumber> findByBusCompany_Id(Long busCompanyId);
    List<BusNumber> findByBusCompany_IdAndIsActiveTrue(Long busCompanyId);
    
    // Find by bus number and company name
        Optional<BusNumber> findByBusNumberAndBusCompany_Id(String busNumber, Long busCompanyId);
    
    // Find all routes (both directions) for a specific bus number and company by ID
    List<BusNumber> findAllByBusNumberAndBusCompany_Id(String busNumber, Long busCompanyId);
    
    // Find all active bus numbers
    List<BusNumber> findByIsActiveTrue();
    
    // Find all bus numbers by route name
    List<BusNumber> findByRouteName(String routeName);
    
    // Find all bus numbers by direction
    List<BusNumber> findByDirection(String direction);
    
    // Search bus numbers by company name containing (by relationship)
    List<BusNumber> findByBusCompany_NameContainingIgnoreCase(String companyName);
    
    // Search bus numbers by route name containing
    List<BusNumber> findByRouteNameContainingIgnoreCase(String routeName);
    
    // Find by start or end destination
    @Query("SELECT bn FROM BusNumber bn WHERE " +
           "LOWER(bn.startDestination) LIKE LOWER(CONCAT('%', :destination, '%')) OR " +
           "LOWER(bn.endDestination) LIKE LOWER(CONCAT('%', :destination, '%'))")
    List<BusNumber> findByDestination(@Param("destination") String destination);
    
    // Find by distance range
    @Query("SELECT bn FROM BusNumber bn WHERE bn.distanceKm BETWEEN :minDistance AND :maxDistance")
    List<BusNumber> findByDistanceRange(@Param("minDistance") Double minDistance, 
                                       @Param("maxDistance") Double maxDistance);
    
    // Check if bus number exists for company by ID
    boolean existsByBusNumberAndBusCompany_Id(String busNumber, Long busCompanyId);
    
    // Count bus numbers by company ID
    Long countByBusCompany_Id(Long busCompanyId);
    
    // Custom query to get bus numbers with routes grouped by company
    @Query("SELECT bn FROM BusNumber bn WHERE bn.isActive = true ORDER BY bn.busCompany.name, bn.busNumber")
    List<BusNumber> findAllActiveOrderedByCompanyAndBusNumber();
}
