package com.backend.onebus.service;

import com.backend.onebus.dto.TrackerDTO;
import com.backend.onebus.model.BusCompany;
import com.backend.onebus.model.Tracker;
import com.backend.onebus.model.RegisteredBus;
import com.backend.onebus.repository.TrackerRepository;
import com.backend.onebus.repository.BusCompanyRepository;
import com.backend.onebus.repository.RegisteredBusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class TrackerService {

    @Autowired
    private TrackerRepository trackerRepository;

    @Autowired
    private BusCompanyRepository companyRepository;

    @Autowired
    private RegisteredBusRepository registeredBusRepository;

    @Autowired
    private DashboardStatsService dashboardStatsService;

    /**
     * Get all trackers
     */
    public List<TrackerDTO> getAllTrackers() {
        return trackerRepository.findAll().stream()
                .map(TrackerDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get tracker by ID
     */
    public TrackerDTO getTrackerById(Long id) {
        Tracker tracker = trackerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tracker not found with id: " + id));
        return new TrackerDTO(tracker);
    }

    /**
     * Get tracker by IMEI
     */
    public TrackerDTO getTrackerByImei(String imei) {
        Tracker tracker = trackerRepository.findByImei(imei)
                .orElseThrow(() -> new RuntimeException("Tracker not found with IMEI: " + imei));
        return new TrackerDTO(tracker);
    }

    /**
     * Get all trackers by company
     */
    public List<TrackerDTO> getTrackersByCompanyId(Long companyId) {
        return trackerRepository.findByCompanyId(companyId).stream()
                .map(TrackerDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get all trackers by status
     */
    public List<TrackerDTO> getTrackersByStatus(Tracker.TrackerStatus status) {
        return trackerRepository.findByStatus(status).stream()
                .map(TrackerDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get available trackers
     */
    public List<TrackerDTO> getAvailableTrackers() {
        return trackerRepository.findAvailableTrackers().stream()
                .map(TrackerDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Get available trackers for a company
     */
    public List<TrackerDTO> getAvailableTrackersByCompanyId(Long companyId) {
        return trackerRepository.findAvailableTrackersByCompanyId(companyId).stream()
                .map(TrackerDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Search trackers
     */
    public List<TrackerDTO> searchTrackers(String searchTerm) {
        return trackerRepository.searchTrackers(searchTerm).stream()
                .map(TrackerDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Create new tracker
     */
    public TrackerDTO createTracker(TrackerDTO trackerDTO) {
        // Check if IMEI already exists
        if (trackerRepository.existsByImei(trackerDTO.getImei())) {
            throw new RuntimeException("Tracker with IMEI " + trackerDTO.getImei() + " already exists");
        }

        Tracker tracker = new Tracker();
        tracker.setImei(trackerDTO.getImei());
        tracker.setBrand(trackerDTO.getBrand());
        tracker.setModel(trackerDTO.getModel());
        tracker.setPurchaseDate(trackerDTO.getPurchaseDate());
        tracker.setStatus(trackerDTO.getStatus() != null ? trackerDTO.getStatus() : Tracker.TrackerStatus.AVAILABLE);
        tracker.setNotes(trackerDTO.getNotes());

        // Set company if provided
        if (trackerDTO.getCompanyId() != null) {
            BusCompany company = companyRepository.findById(trackerDTO.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found with id: " + trackerDTO.getCompanyId()));
            tracker.setCompany(company);
        }

        Tracker savedTracker = trackerRepository.save(tracker);
        
        // Update dashboard stats
        dashboardStatsService.incrementTrackers();
        
        return new TrackerDTO(savedTracker);
    }

    /**
     * Update tracker
     */
    public TrackerDTO updateTracker(Long id, TrackerDTO trackerDTO) {
        Tracker tracker = trackerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tracker not found with id: " + id));

        // Check if IMEI is being changed and if new IMEI already exists
        if (!tracker.getImei().equals(trackerDTO.getImei()) && 
            trackerRepository.existsByImei(trackerDTO.getImei())) {
            throw new RuntimeException("Tracker with IMEI " + trackerDTO.getImei() + " already exists");
        }

        tracker.setImei(trackerDTO.getImei());
        tracker.setBrand(trackerDTO.getBrand());
        tracker.setModel(trackerDTO.getModel());
        tracker.setPurchaseDate(trackerDTO.getPurchaseDate());
        tracker.setStatus(trackerDTO.getStatus());
        tracker.setNotes(trackerDTO.getNotes());

        // Update company if provided
        if (trackerDTO.getCompanyId() != null) {
            BusCompany company = companyRepository.findById(trackerDTO.getCompanyId())
                    .orElseThrow(() -> new RuntimeException("Company not found with id: " + trackerDTO.getCompanyId()));
            tracker.setCompany(company);
        }

        Tracker updatedTracker = trackerRepository.save(tracker);
        return new TrackerDTO(updatedTracker);
    }

    /**
     * Delete tracker
     */
    public void deleteTracker(Long id) {
        Tracker tracker = trackerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Tracker not found with id: " + id));

        // Check if tracker is assigned to a bus
        if (tracker.getAssignedBus() != null) {
        
        // Update dashboard stats
        dashboardStatsService.decrementTrackers();
            throw new RuntimeException("Cannot delete tracker that is assigned to a bus. Please unassign first.");
        }

        trackerRepository.delete(tracker);
    }

    /**
     * Assign tracker to bus
     */
    public TrackerDTO assignTrackerToBus(Long trackerId, Long busId) {
        Tracker tracker = trackerRepository.findById(trackerId)
                .orElseThrow(() -> new RuntimeException("Tracker not found with id: " + trackerId));

        RegisteredBus bus = registeredBusRepository.findById(busId)
                .orElseThrow(() -> new RuntimeException("Bus not found with id: " + busId));

        // Check if tracker is already assigned
        if (tracker.getAssignedBus() != null) {
            throw new RuntimeException("Tracker is already assigned to bus: " + tracker.getAssignedBus().getBusNumber());
        }

        // Check if bus already has a tracker
        if (bus.getTracker() != null) {
            throw new RuntimeException("Bus already has a tracker assigned");
        }

        // Assign tracker to bus
        bus.setTracker(tracker);
        bus.setTrackerImei(tracker.getImei()); // Keep legacy field in sync
        tracker.setStatus(Tracker.TrackerStatus.IN_USE);

        registeredBusRepository.save(bus);
        Tracker updatedTracker = trackerRepository.save(tracker);

        return new TrackerDTO(updatedTracker);
    }

    /**
     * Unassign tracker from bus
     */
    public TrackerDTO unassignTrackerFromBus(Long trackerId) {
        Tracker tracker = trackerRepository.findById(trackerId)
                .orElseThrow(() -> new RuntimeException("Tracker not found with id: " + trackerId));

        if (tracker.getAssignedBus() == null) {
            throw new RuntimeException("Tracker is not assigned to any bus");
        }

        RegisteredBus bus = tracker.getAssignedBus();
        bus.setTracker(null);
        tracker.setStatus(Tracker.TrackerStatus.AVAILABLE);

        registeredBusRepository.save(bus);
        Tracker updatedTracker = trackerRepository.save(tracker);

        return new TrackerDTO(updatedTracker);
    }

    /**
     * Migrate existing buses with IMEI to create tracker records
     * This is used for buses that have trackerImei but no tracker reference
     */
    public int migrateExistingBusTrackers() {
        // Use custom query with JOIN FETCH to eagerly load company relationship
        List<RegisteredBus> busesWithoutTracker = registeredBusRepository.findBusesNeedingTrackerMigration();

        int migratedCount = 0;

        for (RegisteredBus bus : busesWithoutTracker) {
            try {
                // Check if tracker with this IMEI already exists
                Tracker tracker = trackerRepository.findByImei(bus.getTrackerImei())
                        .orElseGet(() -> {
                            // Create new tracker
                            Tracker newTracker = new Tracker();
                            newTracker.setImei(bus.getTrackerImei());
                            newTracker.setBrand("Unknown"); // Default value
                            newTracker.setModel("Unknown"); // Default value
                            newTracker.setStatus(Tracker.TrackerStatus.IN_USE);
                            newTracker.setCompany(bus.getCompany());
                            newTracker.setNotes("Migrated from existing bus data");
                            return trackerRepository.save(newTracker);
                        });

                // Assign tracker to bus
                bus.setTracker(tracker);
                registeredBusRepository.save(bus);
                migratedCount++;
            } catch (Exception e) {
                System.err.println("Failed to migrate tracker for bus " + bus.getBusNumber() + ": " + e.getMessage());
            }
        }

        return migratedCount;
    }

    /**
     * Get tracker statistics
     */
    public TrackerStatistics getTrackerStatistics() {
        long total = trackerRepository.count();
        long available = trackerRepository.countByStatus(Tracker.TrackerStatus.AVAILABLE);
        long inUse = trackerRepository.countByStatus(Tracker.TrackerStatus.IN_USE);
        long maintenance = trackerRepository.countByStatus(Tracker.TrackerStatus.MAINTENANCE);
        long damaged = trackerRepository.countByStatus(Tracker.TrackerStatus.DAMAGED);
        long retired = trackerRepository.countByStatus(Tracker.TrackerStatus.RETIRED);

        return new TrackerStatistics(total, available, inUse, maintenance, damaged, retired);
    }

    // Inner class for statistics
    public static class TrackerStatistics {
        public long total;
        public long available;
        public long inUse;
        public long maintenance;
        public long damaged;
        public long retired;

        public TrackerStatistics(long total, long available, long inUse, long maintenance, long damaged, long retired) {
            this.total = total;
            this.available = available;
            this.inUse = inUse;
            this.maintenance = maintenance;
            this.damaged = damaged;
            this.retired = retired;
        }
    }
}
