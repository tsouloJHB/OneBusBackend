package com.backend.onebus.controller;

import com.backend.onebus.dto.TrackerDTO;
import com.backend.onebus.model.Tracker;
import com.backend.onebus.security.RoleBasedAccessControl;
import com.backend.onebus.service.TrackerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/trackers")
@CrossOrigin(origins = "*")
@RoleBasedAccessControl(allowedRoles = {"ADMIN"})
public class TrackerController {

    @Autowired
    private TrackerService trackerService;

    /**
     * Get all trackers
     */
    @GetMapping
    public ResponseEntity<List<TrackerDTO>> getAllTrackers(@RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(trackerService.getAllTrackers(companyId));
    }

    /**
     * Get tracker by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<TrackerDTO> getTrackerById(@PathVariable Long id) {
        return ResponseEntity.ok(trackerService.getTrackerById(id));
    }

    /**
     * Get tracker by IMEI
     */
    @GetMapping("/imei/{imei}")
    public ResponseEntity<TrackerDTO> getTrackerByImei(@PathVariable String imei) {
        return ResponseEntity.ok(trackerService.getTrackerByImei(imei));
    }

    /**
     * Get trackers by company
     */
    @GetMapping("/company/{companyId}")
    public ResponseEntity<List<TrackerDTO>> getTrackersByCompanyId(@PathVariable Long companyId) {
        return ResponseEntity.ok(trackerService.getTrackersByCompanyId(companyId));
    }

    /**
     * Get trackers by status
     */
    @GetMapping("/status/{status}")
    public ResponseEntity<List<TrackerDTO>> getTrackersByStatus(@PathVariable Tracker.TrackerStatus status) {
        return ResponseEntity.ok(trackerService.getTrackersByStatus(status));
    }

    /**
     * Get available trackers
     */
    @GetMapping("/available")
    public ResponseEntity<List<TrackerDTO>> getAvailableTrackers() {
        return ResponseEntity.ok(trackerService.getAvailableTrackers());
    }

    /**
     * Get available trackers for a company
     */
    @GetMapping("/available/company/{companyId}")
    public ResponseEntity<List<TrackerDTO>> getAvailableTrackersByCompanyId(@PathVariable Long companyId) {
        return ResponseEntity.ok(trackerService.getAvailableTrackersByCompanyId(companyId));
    }

    /**
     * Search trackers
     */
    @GetMapping("/search")
    public ResponseEntity<List<TrackerDTO>> searchTrackers(@RequestParam String q, @RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(trackerService.searchTrackers(q, companyId));
    }

    /**
     * Create new tracker
     */
    @PostMapping
    public ResponseEntity<TrackerDTO> createTracker(@Valid @RequestBody TrackerDTO trackerDTO) {
        TrackerDTO created = trackerService.createTracker(trackerDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /**
     * Update tracker
     */
    @PutMapping("/{id}")
    public ResponseEntity<TrackerDTO> updateTracker(
            @PathVariable Long id,
            @Valid @RequestBody TrackerDTO trackerDTO) {
        return ResponseEntity.ok(trackerService.updateTracker(id, trackerDTO));
    }

    /**
     * Delete tracker
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTracker(@PathVariable Long id) {
        trackerService.deleteTracker(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * Assign tracker to bus
     */
    @PostMapping("/{trackerId}/assign/{busId}")
    public ResponseEntity<TrackerDTO> assignTrackerToBus(
            @PathVariable Long trackerId,
            @PathVariable Long busId) {
        return ResponseEntity.ok(trackerService.assignTrackerToBus(trackerId, busId));
    }

    /**
     * Unassign tracker from bus
     */
    @PostMapping("/{trackerId}/unassign")
    public ResponseEntity<TrackerDTO> unassignTrackerFromBus(@PathVariable Long trackerId) {
        return ResponseEntity.ok(trackerService.unassignTrackerFromBus(trackerId));
    }

    /**
     * Migrate existing bus trackers
     * This creates tracker records for buses that have IMEI but no tracker reference
     */
    @PostMapping("/migrate")
    public ResponseEntity<Map<String, Object>> migrateExistingBusTrackers() {
        int migratedCount = trackerService.migrateExistingBusTrackers();
        return ResponseEntity.ok(Map.of(
                "message", "Migration completed",
                "migratedCount", migratedCount
        ));
    }

    /**
     * Get tracker statistics
     */
    @GetMapping("/statistics")
    public ResponseEntity<TrackerService.TrackerStatistics> getTrackerStatistics(@RequestParam(required = false) Long companyId) {
        return ResponseEntity.ok(trackerService.getTrackerStatistics(companyId));
    }
}
