package com.backend.onebus.repository;

import com.backend.onebus.model.Route;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    
    @Cacheable(value = "routes", key = "#company + '_' + #busNumber", unless = "#result == null")
    @Query("SELECT r FROM Route r WHERE r.company = :company AND r.busNumber = :busNumber AND r.active = true")
    Optional<Route> findByCompanyAndBusNumber(@Param("company") String company, @Param("busNumber") String busNumber);
    
    @Cacheable(value = "routes_by_company", key = "#company", unless = "#result == null")
    @Query("SELECT r FROM Route r WHERE r.company = :company AND r.active = true")
    List<Route> findByCompany(@Param("company") String company);
    
    @Cacheable(value = "routes_by_bus", key = "#busNumber", unless = "#result == null")
    @Query("SELECT r FROM Route r WHERE r.busNumber = :busNumber AND r.active = true")
    List<Route> findByBusNumber(@Param("busNumber") String busNumber);
    
    @Cacheable(value = "routes_by_dir", key = "#busNumber + '_' + #direction", unless = "#result == null")
    @Query("SELECT r FROM Route r WHERE r.busNumber = :busNumber AND r.direction = :direction AND r.active = true")
    Optional<Route> findByBusNumberAndDirection(@Param("busNumber") String busNumber, @Param("direction") String direction);
    
    @Cacheable(value = "routes_company_dir", key = "#company + '_' + #busNumber + '_' + #direction", unless = "#result == null")
    @Query("SELECT r FROM Route r WHERE r.company = :company AND r.busNumber = :busNumber AND r.direction = :direction")
    Optional<Route> findByCompanyAndBusNumberAndDirection(@Param("company") String company, 
                                                        @Param("busNumber") String busNumber, 
                                                        @Param("direction") String direction);
    
    // Case-insensitive search for routes by bus number and company
    @Cacheable(value = "routes_ignorecase", key = "#company + '_' + #busNumber", unless = "#result == null")
    @Query("SELECT r FROM Route r WHERE LOWER(r.company) = LOWER(:company) AND LOWER(r.busNumber) = LOWER(:busNumber) AND r.active = true")
    List<Route> findByBusNumberAndCompanyIgnoreCase(@Param("busNumber") String busNumber, @Param("company") String company);
    
    List<Route> findByActiveTrue();

    long countByCompany(String company);
}