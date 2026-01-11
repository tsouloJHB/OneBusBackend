package com.backend.onebus.repository;

import com.backend.onebus.model.RouteStop;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface RouteStopRepository extends JpaRepository<RouteStop, Long> {
    
    @Cacheable(value = "route_stops", key = "#routeId", unless = "#result == null")
    @Query("SELECT rs FROM RouteStop rs WHERE rs.route.id = :routeId ORDER BY rs.busStopIndex")
    List<RouteStop> findByRouteIdOrderByBusStopIndex(@Param("routeId") Long routeId);
    
    @Cacheable(value = "route_stops_by_dir", key = "#routeId + '_' + #direction", unless = "#result == null")
    @Query("SELECT rs FROM RouteStop rs WHERE rs.route.id = :routeId AND rs.direction = :direction ORDER BY rs.busStopIndex")
    List<RouteStop> findByRouteIdAndDirectionOrderByBusStopIndex(@Param("routeId") Long routeId, @Param("direction") String direction);
} 