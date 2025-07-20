package com.backend.onebus.repository;

import com.backend.onebus.model.RouteStop;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {
    
    @Query("SELECT rs FROM RouteStop rs WHERE rs.route.id = :routeId ORDER BY rs.busStopIndex")
    List<RouteStop> findByRouteIdOrderByBusStopIndex(@Param("routeId") Long routeId);
    
    @Query("SELECT rs FROM RouteStop rs WHERE rs.route.id = :routeId AND rs.direction = :direction ORDER BY rs.busStopIndex")
    List<RouteStop> findByRouteIdAndDirectionOrderByBusStopIndex(@Param("routeId") Long routeId, @Param("direction") String direction);
} 