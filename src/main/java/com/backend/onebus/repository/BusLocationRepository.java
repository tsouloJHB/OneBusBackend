package com.backend.onebus.repository;

import com.backend.onebus.model.BusLocation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusLocationRepository extends JpaRepository<BusLocation, String> {
}