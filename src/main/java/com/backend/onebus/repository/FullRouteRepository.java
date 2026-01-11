package com.backend.onebus.repository;

import com.backend.onebus.model.FullRoute;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface FullRouteRepository extends JpaRepository<FullRoute, Long> {
    List<FullRoute> findByCompanyId(Long companyId);
    List<FullRoute> findByRouteId(Long routeId);
    List<FullRoute> findByCompanyIdAndRouteId(Long companyId, Long routeId);
    
    @Cacheable(value = "full_routes", key = "#routeId + '_' + #direction", unless = "#result == null")
    List<FullRoute> findByRouteIdAndDirection(Long routeId, String direction);

}
