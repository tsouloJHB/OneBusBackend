package com.backend.onebus.service;

import com.backend.onebus.dto.DriverDTO;
import com.backend.onebus.dto.DriverRegistrationRequest;
import com.backend.onebus.model.BusCompany;
import com.backend.onebus.model.Driver;
import com.backend.onebus.model.Driver.DriverStatus;
import com.backend.onebus.model.RegisteredBus;
import com.backend.onebus.repository.BusCompanyRepository;
import com.backend.onebus.repository.DriverRepository;
import com.backend.onebus.repository.RegisteredBusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class DriverService {
    
    private static final Logger logger = LoggerFactory.getLogger(DriverService.class);
    
    @Autowired
    private DriverRepository driverRepository;
    
    @Autowired
    private BusCompanyRepository busCompanyRepository;
    
    @Autowired
    private RegisteredBusRepository registeredBusRepository;
    
    @Autowired
    private PasswordEncoder passwordEncoder;
    
    // Default password for admin-registered drivers
    private static final String DEFAULT_PASSWORD = "Driver@123";
    
    /**
     * Admin registers a driver with default password
     */
    @Transactional
    public DriverDTO registerDriverByAdmin(DriverRegistrationRequest request) {
        // Validate driver ID doesn't exist
        if (driverRepository.existsByDriverId(request.getDriverId())) {
            throw new RuntimeException("Driver ID already exists: " + request.getDriverId());
        }
        
        // Validate email doesn't exist
        if (driverRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }
        
        Driver driver = new Driver();
        driver.setDriverId(request.getDriverId());
        driver.setFullName(request.getFullName());
        driver.setEmail(request.getEmail());

        // Normalize optional fields
        driver.setPhoneNumber(normalizePhone(request.getPhoneNumber()));
        driver.setLicenseNumber(normalizeBlankToNull(request.getLicenseNumber()));
        driver.setLicenseExpiryDate(parseLicenseDate(request.getLicenseExpiryDate()));
        driver.setStatus(DriverStatus.ACTIVE);
        driver.setIsRegisteredByAdmin(true);
        
        // Set default password for admin-registered drivers
        driver.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        
        // Set company if provided
        if (request.getCompanyId() != null) {
            BusCompany company = busCompanyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + request.getCompanyId()));
            driver.setCompany(company);
        }
        
        Driver savedDriver = driverRepository.save(driver);
        return new DriverDTO(savedDriver);
    }
    
    /**
     * Driver self-registration
     */
    @Transactional
    public DriverDTO registerDriver(DriverRegistrationRequest request) {
        // Validate driver ID doesn't exist
        if (driverRepository.existsByDriverId(request.getDriverId())) {
            throw new RuntimeException("Driver ID already exists: " + request.getDriverId());
        }
        
        // Validate email doesn't exist
        if (driverRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already exists: " + request.getEmail());
        }
        
        // Validate password is provided for self-registration
        if (request.getPassword() == null || request.getPassword().trim().isEmpty()) {
            throw new RuntimeException("Password is required for driver registration");
        }
        
        Driver driver = new Driver();
        driver.setDriverId(request.getDriverId());
        driver.setFullName(request.getFullName());
        driver.setEmail(request.getEmail());

        // Normalize optional fields
        driver.setPhoneNumber(normalizePhone(request.getPhoneNumber()));
        driver.setLicenseNumber(normalizeBlankToNull(request.getLicenseNumber()));
        driver.setLicenseExpiryDate(parseLicenseDate(request.getLicenseExpiryDate()));
        driver.setStatus(DriverStatus.ACTIVE);
        driver.setIsRegisteredByAdmin(false);
        
        // Encode provided password
        driver.setPassword(passwordEncoder.encode(request.getPassword()));
        
        // Set company if provided
        if (request.getCompanyId() != null) {
            BusCompany company = busCompanyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + request.getCompanyId()));
            driver.setCompany(company);
        }
        
        Driver savedDriver = driverRepository.save(driver);
        return new DriverDTO(savedDriver);
    }
    
    /**
     * Get all drivers
     */
    public List<DriverDTO> getAllDrivers() {
        return driverRepository.findAll()
            .stream()
            .map(DriverDTO::new)
            .collect(Collectors.toList());
    }
    
    /**
     * Get driver by ID
     */
    public DriverDTO getDriverById(Long id) {
        Driver driver = driverRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + id));
        return new DriverDTO(driver);
    }
    
    /**
     * Get driver by driver ID
     */
    public DriverDTO getDriverByDriverId(String driverId) {
        Driver driver = driverRepository.findByDriverId(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found with driver ID: " + driverId));
        return new DriverDTO(driver);
    }
    
    /**
     * Get driver by email
     */
    public DriverDTO getDriverByEmail(String email) {
        Driver driver = driverRepository.findByEmail(email)
            .orElseThrow(() -> new RuntimeException("Driver not found with email: " + email));
        return new DriverDTO(driver);
    }
    
    /**
     * Get all active drivers
     */
    public List<DriverDTO> getAllActiveDrivers() {
        return driverRepository.findAllActiveDrivers()
            .stream()
            .map(DriverDTO::new)
            .collect(Collectors.toList());
    }
    
    /**
     * Get drivers by company ID
     */
    public List<DriverDTO> getDriversByCompanyId(Long companyId) {
        return driverRepository.findByCompanyId(companyId)
            .stream()
            .map(DriverDTO::new)
            .collect(Collectors.toList());
    }
    
    /**
     * Search drivers by name
     */
    public List<DriverDTO> searchDriversByName(String name) {
        return driverRepository.searchByName(name)
            .stream()
            .map(DriverDTO::new)
            .collect(Collectors.toList());
    }
    
    /**
     * Update driver
     */
    @Transactional
    public DriverDTO updateDriver(Long id, DriverDTO driverDTO) {
        Driver driver = driverRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + id));
        
        // Update fields
        if (driverDTO.getFullName() != null) {
            driver.setFullName(driverDTO.getFullName());
        }
        if (driverDTO.getEmail() != null && !driverDTO.getEmail().equals(driver.getEmail())) {
            // Check if new email already exists
            if (driverRepository.existsByEmail(driverDTO.getEmail())) {
                throw new RuntimeException("Email already exists: " + driverDTO.getEmail());
            }
            driver.setEmail(driverDTO.getEmail());
        }
        if (driverDTO.getPhoneNumber() != null) {
            driver.setPhoneNumber(driverDTO.getPhoneNumber());
        }
        if (driverDTO.getLicenseNumber() != null) {
            driver.setLicenseNumber(driverDTO.getLicenseNumber());
        }
        if (driverDTO.getLicenseExpiryDate() != null) {
            driver.setLicenseExpiryDate(driverDTO.getLicenseExpiryDate());
        }
        if (driverDTO.getStatus() != null) {
            driver.setStatus(driverDTO.getStatus());
        }
        if (driverDTO.getCompanyId() != null) {
            BusCompany company = busCompanyRepository.findById(driverDTO.getCompanyId())
                .orElseThrow(() -> new RuntimeException("Company not found with ID: " + driverDTO.getCompanyId()));
            driver.setCompany(company);
        }
        if (driverDTO.getOnDuty() != null) {
            driver.setOnDuty(driverDTO.getOnDuty());
        }
        if (driverDTO.getCurrentlyAssignedBusId() != null) {
            driver.setCurrentlyAssignedBusId(driverDTO.getCurrentlyAssignedBusId());
        }
        if (driverDTO.getCurrentlyAssignedBusNumber() != null) {
            driver.setCurrentlyAssignedBusNumber(driverDTO.getCurrentlyAssignedBusNumber());
        }
        if (driverDTO.getLastAssignedBusId() != null) {
            driver.setLastAssignedBusId(driverDTO.getLastAssignedBusId());
        }
        if (driverDTO.getLastAssignedBusNumber() != null) {
            driver.setLastAssignedBusNumber(driverDTO.getLastAssignedBusNumber());
        }
        if (driverDTO.getAssignedRouteId() != null) {
            driver.setAssignedRouteId(driverDTO.getAssignedRouteId());
        }
        if (driverDTO.getAssignedRouteName() != null) {
            driver.setAssignedRouteName(driverDTO.getAssignedRouteName());
        }
        if (driverDTO.getShiftStartTime() != null) {
            driver.setShiftStartTime(driverDTO.getShiftStartTime());
        }
        if (driverDTO.getShiftEndTime() != null) {
            driver.setShiftEndTime(driverDTO.getShiftEndTime());
        }
        if (driverDTO.getTotalHoursWorked() != null) {
            driver.setTotalHoursWorked(driverDTO.getTotalHoursWorked());
        }
        
        Driver updatedDriver = driverRepository.save(driver);
        return new DriverDTO(updatedDriver);
    }

    /**
     * Convert date string (YYYY-MM-DD) to LocalDateTime at start of day. Returns null if blank.
     */
    private LocalDateTime parseLicenseDate(String dateStr) {
        if (dateStr == null || dateStr.trim().isEmpty()) {
            return null;
        }
        try {
            LocalDate date = LocalDate.parse(dateStr.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
            return date.atStartOfDay();
        } catch (Exception e) {
            throw new RuntimeException("Invalid license expiry date format. Expected YYYY-MM-DD");
        }
    }

    /**
     * Convert blank strings to null for optional fields.
     */
    private String normalizeBlankToNull(String value) {
        return (value == null || value.trim().isEmpty()) ? null : value.trim();
    }

    /**
     * Normalize phone number: return null if blank, otherwise trimmed.
     */
    private String normalizePhone(String phone) {
        return normalizeBlankToNull(phone);
    }
    
    /**
     * Update driver status
     */
    @Transactional
    public DriverDTO updateDriverStatus(Long id, DriverStatus status) {
        Driver driver = driverRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + id));
        
        driver.setStatus(status);
        Driver updatedDriver = driverRepository.save(driver);
        return new DriverDTO(updatedDriver);
    }
    
    /**
     * Update driver's last login
     */
    @Transactional
    public void updateLastLogin(String driverId) {
        Driver driver = driverRepository.findByDriverId(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found with driver ID: " + driverId));
        driver.setLastLogin(LocalDateTime.now());
        driverRepository.save(driver);
    }
    
    /**
     * Delete driver
     */
    @Transactional
    public void deleteDriver(Long id) {
        if (!driverRepository.existsById(id)) {
            throw new RuntimeException("Driver not found with ID: " + id);
        }
        driverRepository.deleteById(id);
    }
    
    /**
     * Get driver count
     */
    public long getDriverCount() {
        return driverRepository.count();
    }
    
    /**
     * Get active driver count
     */
    public long getActiveDriverCount() {
        return driverRepository.countActiveDrivers();
    }

    /**
     * Assign a driver to a bus
     * If the bus already has a driver, that driver will be removed and set to INACTIVE
     */
    @Transactional
    public DriverDTO assignDriverToBus(Long driverId, String busRegistrationNumber) {
        // Find the driver
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + driverId));
        
        // Find the bus
        RegisteredBus bus = registeredBusRepository.findByRegistrationNumber(busRegistrationNumber);
        if (bus == null) {
            throw new RuntimeException("Bus not found with registration number: " + busRegistrationNumber);
        }
        
        // Check if the bus already has a driver
        String existingDriverId = bus.getDriverId();
        if (existingDriverId != null && !existingDriverId.trim().isEmpty()) {
            // Remove the existing driver from the bus
            if (!existingDriverId.equals(driver.getDriverId())) {
                driverRepository.findByDriverId(existingDriverId).ifPresent(existingDriver -> {
                    existingDriver.setStatus(DriverStatus.INACTIVE);
                    driverRepository.save(existingDriver);
                    logger.info("Set existing driver {} to INACTIVE (replaced by driver {})", 
                        existingDriverId, driver.getDriverId());
                });
            }
        }
        
        // Assign the driver to the bus
        bus.setDriverId(driver.getDriverId());
        bus.setDriverName(driver.getFullName());
        registeredBusRepository.save(bus);
        
        // Update driver status based on bus status
        DriverStatus newDriverStatus;
        switch (bus.getStatus()) {
            case ACTIVE:
                newDriverStatus = DriverStatus.ACTIVE;
                break;
            case INACTIVE:
            case MAINTENANCE:
            case RETIRED:
                newDriverStatus = DriverStatus.INACTIVE;
                break;
            default:
                newDriverStatus = DriverStatus.INACTIVE;
        }
        
        driver.setStatus(newDriverStatus);
        Driver savedDriver = driverRepository.save(driver);
        
        logger.info("Assigned driver {} to bus {} with status {}", 
            driver.getDriverId(), busRegistrationNumber, newDriverStatus);
        
        return new DriverDTO(savedDriver);
    }

    /**
     * Remove a driver from their current bus assignment
     * Sets the driver status to INACTIVE
     */
    @Transactional
    public DriverDTO removeDriverFromBus(Long driverId) {
        // Find the driver
        Driver driver = driverRepository.findById(driverId)
            .orElseThrow(() -> new RuntimeException("Driver not found with ID: " + driverId));
        
        // Find any buses assigned to this driver
        List<RegisteredBus> assignedBuses = registeredBusRepository.findByDriverId(driver.getDriverId());
        
        if (assignedBuses.isEmpty()) {
            throw new RuntimeException("Driver is not currently assigned to any bus");
        }
        
        // Remove driver from all assigned buses
        for (RegisteredBus bus : assignedBuses) {
            bus.setDriverId(null);
            bus.setDriverName(null);
            registeredBusRepository.save(bus);
            logger.info("Removed driver {} from bus {}", driver.getDriverId(), bus.getRegistrationNumber());
        }
        
        // Set driver status to INACTIVE
        driver.setStatus(DriverStatus.INACTIVE);
        Driver savedDriver = driverRepository.save(driver);
        
        logger.info("Set driver {} to INACTIVE (removed from bus assignment)", driver.getDriverId());
        
        return new DriverDTO(savedDriver);
    }
}
