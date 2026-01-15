package com.backend.onebus.service;

import com.backend.onebus.model.BusLocation;
import com.backend.onebus.repository.BusLocationRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class BusPersistenceService {
    
    private static final Logger logger = LoggerFactory.getLogger(BusPersistenceService.class);
    
    @Autowired
    private BusLocationRepository busLocationRepository;

    @Value("${app.tracking.persistence.enabled:true}")
    private boolean persistenceEnabled;

    @jakarta.annotation.PostConstruct
    public void init() {
        logger.info("[PERSISTENCE-CONFIG] Tracking persistence is {}", persistenceEnabled ? "ENABLED" : "DISABLED");
    }
    
    /**
     * Persist bus location to database asynchronously to avoid blocking the real-time pipeline.
     */
    @Async
    public void saveLocationAsync(BusLocation payload) {
        if (!persistenceEnabled) {
            return;
        }
        long dbStart = System.currentTimeMillis();
        try {
            // Always stamp the payload before saving
            payload.setLastSavedTimestamp(Instant.now().toEpochMilli());
            
            busLocationRepository.save(payload);
            long dbEnd = System.currentTimeMillis();
            logger.debug("[ASYNC-DB] [IMEI:{}] Saved to DB in {}ms", 
                payload.getTrackerImei(), (dbEnd - dbStart));
        } catch (Exception e) {
            logger.error("[ASYNC-DB] Error saving position for bus {}: {}", 
                payload.getBusId(), e.getMessage());
        }
    }
}
