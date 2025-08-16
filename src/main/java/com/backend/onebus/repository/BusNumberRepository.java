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
    
    // Find by bus number and company name
    Optional<BusNumber> findByBusNumberAndCompanyName(String busNumber, String companyName);
    
    // Find all routes (both directions) for a specific bus number and company
    List<BusNumber> findAllByBusNumberAndCompanyName(String busNumber, String companyName);
    
    // Find all bus numbers by company name
    List<BusNumber> findByCompanyName(String companyName);
    
    // Find all bus numbers by company name (case insensitive)
    List<BusNumber> findByCompanyNameIgnoreCase(String companyName);
    
    // Find all active bus numbers
    List<BusNumber> findByIsActiveTrue();
    
    // Find all active bus numbers by company name
    List<BusNumber> findByCompanyNameAndIsActiveTrue(String companyName);
    
    // Find all bus numbers by route name
    List<BusNumber> findByRouteName(String routeName);
    
    // Find all bus numbers by direction
    List<BusNumber> findByDirection(String direction);
    
    // Search bus numbers by company name containing
    List<BusNumber> findByCompanyNameContainingIgnoreCase(String companyName);
    
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
    
    // Check if bus number exists for company
    boolean existsByBusNumberAndCompanyName(String busNumber, String companyName);
    
    // Count bus numbers by company
    @Query("SELECT COUNT(bn) FROM BusNumber bn WHERE bn.companyName = :companyName")
    Long countByCompanyName(@Param("companyName") String companyName);
    
    // Custom query to get bus numbers with routes grouped by company
    @Query("SELECT bn FROM BusNumber bn WHERE bn.isActive = true ORDER BY bn.companyName, bn.busNumber")
    List<BusNumber> findAllActiveOrderedByCompanyAndBusNumber();
}
