package com.backend.onebus.repository;

import com.backend.onebus.model.Route;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface RouteRepository extends JpaRepository<Route, Long> {
    
    @Query("SELECT r FROM Route r WHERE r.company = :company AND r.busNumber = :busNumber AND r.active = true")
    Optional<Route> findByCompanyAndBusNumber(@Param("company") String company, @Param("busNumber") String busNumber);
    
    @Query("SELECT r FROM Route r WHERE r.company = :company AND r.active = true")
    List<Route> findByCompany(@Param("company") String company);
    
    @Query("SELECT r FROM Route r WHERE r.busNumber = :busNumber AND r.active = true")
    List<Route> findByBusNumber(@Param("busNumber") String busNumber);
    
    List<Route> findByActiveTrue();
} 