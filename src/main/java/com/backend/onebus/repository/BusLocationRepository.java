package com.backend.onebus.repository;

import com.backend.onebus.model.BusLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;

public interface BusLocationRepository extends JpaRepository<BusLocation, Long> {
    
    /**
     * Find the most recent location for each distinct bus.
     * Returns buses with lastSavedTimestamp within the last 10 minutes.
     * 
     * @param timeThresholdMillis - timestamp threshold in milliseconds
     * @return List of most recent BusLocation entries, one per bus
     */
    @Query(value = "SELECT DISTINCT ON (bus_number) * FROM bus_locations " +
           "WHERE last_saved_timestamp > :timeThreshold " +
           "ORDER BY bus_number, last_saved_timestamp DESC", 
           nativeQuery = true)
    List<BusLocation> findActiveBuses(@Param("timeThreshold") long timeThresholdMillis);
    
    /**
     * Find the most recent location for a specific bus.
     * 
     * @param busNumber - the bus number to search for
     * @return Most recent BusLocation for the bus, or null if not found
     */
    @Query(value = "SELECT * FROM bus_locations WHERE bus_number = :busNumber " +
           "ORDER BY last_saved_timestamp DESC LIMIT 1", 
           nativeQuery = true)
    BusLocation findLatestLocationByBusNumber(@Param("busNumber") String busNumber);
    
    /**
     * Find active buses by company.
     * 
     * @param busCompany - company name/id
     * @param timeThresholdMillis - timestamp threshold in milliseconds
     * @return List of recent bus locations for the specified company
     */
    @Query(value = "SELECT DISTINCT ON (bus_number) * FROM bus_locations " +
           "WHERE bus_company = :busCompany AND last_saved_timestamp > :timeThreshold " +
           "ORDER BY bus_number, last_saved_timestamp DESC", 
           nativeQuery = true)
    List<BusLocation> findActiveBusesByCompany(@Param("busCompany") String busCompany, 
                                               @Param("timeThreshold") long timeThresholdMillis);
}