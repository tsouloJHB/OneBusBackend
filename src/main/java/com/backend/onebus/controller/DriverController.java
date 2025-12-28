package com.backend.onebus.controller;

import com.backend.onebus.dto.DriverDTO;
import com.backend.onebus.dto.DriverRegistrationRequest;
import com.backend.onebus.model.Driver.DriverStatus;
import com.backend.onebus.service.DriverService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/drivers")
@Tag(name = "Driver Management", description = "APIs for managing bus drivers")
@CrossOrigin
public class DriverController {
    
    @Autowired
    private DriverService driverService;
    
    /**
     * Admin endpoint to register a driver with default password
     */
    @PostMapping("/register-by-admin")
    @Operation(summary = "Register driver by admin", 
               description = "Admin registers a new driver with default password (Driver@123)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Driver registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or driver already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> registerDriverByAdmin(@Valid @RequestBody DriverRegistrationRequest request) {
        try {
            DriverDTO driver = driverService.registerDriverByAdmin(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Driver registered successfully by admin");
            response.put("driver", driver);
            response.put("defaultPassword", "Driver@123");
            response.put("note", "Please ask the driver to change their password after first login");
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * Driver self-registration endpoint
     */
    @PostMapping("/register")
    @Operation(summary = "Driver self-registration", 
               description = "Allows drivers to register themselves with their own password")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Driver registered successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid input or driver already exists"),
        @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<?> registerDriver(@Valid @RequestBody DriverRegistrationRequest request) {
        try {
            DriverDTO driver = driverService.registerDriver(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Driver registered successfully");
            response.put("driver", driver);
            
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * Get all drivers
     */
    @GetMapping
    @Operation(summary = "Get all drivers", description = "Retrieves all registered drivers")
    public ResponseEntity<List<DriverDTO>> getAllDrivers() {
        List<DriverDTO> drivers = driverService.getAllDrivers();
        return ResponseEntity.ok(drivers);
    }
    
    /**
     * Get driver by ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Get driver by ID", description = "Retrieves a driver by their database ID")
    public ResponseEntity<?> getDriverById(@PathVariable Long id) {
        try {
            DriverDTO driver = driverService.getDriverById(id);
            return ResponseEntity.ok(driver);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * Get driver by driver ID
     */
    @GetMapping("/driver-id/{driverId}")
    @Operation(summary = "Get driver by driver ID", description = "Retrieves a driver by their driver ID")
    public ResponseEntity<?> getDriverByDriverId(@PathVariable String driverId) {
        try {
            DriverDTO driver = driverService.getDriverByDriverId(driverId);
            return ResponseEntity.ok(driver);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * Get active drivers
     */
    @GetMapping("/active")
    @Operation(summary = "Get active drivers", description = "Retrieves all active drivers")
    public ResponseEntity<List<DriverDTO>> getActiveDrivers() {
        List<DriverDTO> drivers = driverService.getAllActiveDrivers();
        return ResponseEntity.ok(drivers);
    }
    
    /**
     * Get drivers by company
     */
    @GetMapping("/company/{companyId}")
    @Operation(summary = "Get drivers by company", description = "Retrieves all drivers for a specific company")
    public ResponseEntity<List<DriverDTO>> getDriversByCompany(@PathVariable Long companyId) {
        List<DriverDTO> drivers = driverService.getDriversByCompanyId(companyId);
        return ResponseEntity.ok(drivers);
    }
    
    /**
     * Search drivers by name
     */
    @GetMapping("/search")
    @Operation(summary = "Search drivers by name", description = "Search drivers by name (case-insensitive)")
    public ResponseEntity<List<DriverDTO>> searchDrivers(
        @Parameter(description = "Name to search for") @RequestParam String name) {
        List<DriverDTO> drivers = driverService.searchDriversByName(name);
        return ResponseEntity.ok(drivers);
    }
    
    /**
     * Update driver
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update driver", description = "Updates driver information")
    public ResponseEntity<?> updateDriver(
        @PathVariable Long id,
        @Valid @RequestBody DriverDTO driverDTO) {
        try {
            DriverDTO updatedDriver = driverService.updateDriver(id, driverDTO);
            return ResponseEntity.ok(updatedDriver);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
        }
    }
    
    /**
     * Update driver status
     */
    @PatchMapping("/{id}/status")
    @Operation(summary = "Update driver status", description = "Updates driver status (ACTIVE, INACTIVE, SUSPENDED, ON_LEAVE)")
    public ResponseEntity<?> updateDriverStatus(
        @PathVariable Long id,
        @Parameter(description = "New status") @RequestParam DriverStatus status) {
        try {
            DriverDTO updatedDriver = driverService.updateDriverStatus(id, status);
            return ResponseEntity.ok(updatedDriver);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * Delete driver
     */
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete driver", description = "Deletes a driver from the system")
    public ResponseEntity<?> deleteDriver(@PathVariable Long id) {
        try {
            driverService.deleteDriver(id);
            Map<String, String> response = new HashMap<>();
            response.put("message", "Driver deleted successfully");
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            Map<String, String> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
        }
    }
    
    /**
     * Get driver statistics
     */
    @GetMapping("/statistics")
    @Operation(summary = "Get driver statistics", description = "Get statistics about drivers")
    public ResponseEntity<Map<String, Long>> getDriverStatistics() {
        Map<String, Long> stats = new HashMap<>();
        stats.put("totalDrivers", driverService.getDriverCount());
        stats.put("activeDrivers", driverService.getActiveDriverCount());
        return ResponseEntity.ok(stats);
    }
}
