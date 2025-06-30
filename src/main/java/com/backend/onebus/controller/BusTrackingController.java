package com.backend.onebus.controller;

import com.backend.onebus.model.Bus;
import com.backend.onebus.model.BusLocation;
import com.backend.onebus.service.BusTrackingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class BusTrackingController {
    @Autowired
    private BusTrackingService trackingService;
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(BusTrackingController.class);

    @PostMapping("/tracker/payload")
    public ResponseEntity<?> receiveTrackerPayload(@RequestBody BusLocation payload) {
        logger.info("Received tracker payload: {}", payload);
        trackingService.processTrackerPayload(payload);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/buses/nearest")
    public ResponseEntity<BusLocation> getNearestBus(
            @RequestParam double lat,
            @RequestParam double lon,
            @RequestParam String tripDirection) {
        BusLocation nearestBus = trackingService.findNearestBus(lat, lon, tripDirection);
        return nearestBus != null ? ResponseEntity.ok(nearestBus) : ResponseEntity.notFound().build();
    }

    @PutMapping("/buses/{busId}")
    public ResponseEntity<?> updateBusDetails(@PathVariable String busId, @RequestBody Bus bus) {
        bus.setBusId(busId);
        trackingService.updateBusDetails(bus);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/clear")
    public ResponseEntity<?> clearTrackingData() {
        trackingService.clearTrackingData();
        return ResponseEntity.ok().build();
    }
}