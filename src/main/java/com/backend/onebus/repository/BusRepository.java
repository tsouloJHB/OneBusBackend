package com.backend.onebus.repository;

import com.backend.onebus.model.Bus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BusRepository extends JpaRepository<Bus, String> {
    Bus findByTrackerImei(String trackerImei);
}