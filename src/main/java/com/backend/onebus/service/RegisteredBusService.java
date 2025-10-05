package com.backend.onebus.service;

import com.backend.onebus.dto.RegisteredBusCreateDTO;
import com.backend.onebus.dto.RegisteredBusResponseDTO;
import com.backend.onebus.model.BusCompany;
import com.backend.onebus.model.RegisteredBus;
import com.backend.onebus.repository.BusCompanyRepository;
import com.backend.onebus.repository.RegisteredBusRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class RegisteredBusService {

    @Autowired
    private RegisteredBusRepository registeredBusRepository;

    @Autowired
    private BusCompanyRepository busCompanyRepository;

    /**
     * Create a new registered bus
     */
    public RegisteredBusResponseDTO createRegisteredBus(RegisteredBusCreateDTO createDTO) {
        // Validate company exists
        BusCompany company = busCompanyRepository.findById(createDTO.getCompanyId())
            .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + createDTO.getCompanyId()));

        // Check if registration number already exists
        RegisteredBus existingBus = registeredBusRepository.findByRegistrationNumber(createDTO.getRegistrationNumber());
        if (existingBus != null) {
            throw new IllegalArgumentException("Registration number already exists: " + createDTO.getRegistrationNumber());
        }

        // Create new registered bus
        RegisteredBus registeredBus = new RegisteredBus();
        registeredBus.setCompany(company);
        registeredBus.setRegistrationNumber(createDTO.getRegistrationNumber());
        registeredBus.setBusNumber(createDTO.getBusNumber());
        registeredBus.setBusId(createDTO.getBusId());
        registeredBus.setTrackerImei(createDTO.getTrackerImei());
        registeredBus.setDriverId(createDTO.getDriverId());
        registeredBus.setDriverName(createDTO.getDriverName());
        registeredBus.setModel(createDTO.getModel());
        registeredBus.setYear(createDTO.getYear());
        registeredBus.setCapacity(createDTO.getCapacity());
        registeredBus.setStatus(RegisteredBus.BusStatus.valueOf(createDTO.getStatus().toUpperCase()));
        registeredBus.setRouteId(createDTO.getRouteId());
        registeredBus.setRouteName(createDTO.getRouteName());

        RegisteredBus savedBus = registeredBusRepository.save(registeredBus);

        return convertToResponseDTO(savedBus);
    }

    /**
     * Update an existing registered bus
     */
    public RegisteredBusResponseDTO updateRegisteredBus(Long id, RegisteredBusCreateDTO updateDTO) {
        RegisteredBus registeredBus = registeredBusRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Registered bus not found with ID: " + id));

        // Validate company exists if companyId is provided
        if (updateDTO.getCompanyId() != null && !updateDTO.getCompanyId().equals(registeredBus.getCompany().getId())) {
            BusCompany company = busCompanyRepository.findById(updateDTO.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found with ID: " + updateDTO.getCompanyId()));
            registeredBus.setCompany(company);
        }

        // Check registration number uniqueness (excluding current bus)
        if (!registeredBus.getRegistrationNumber().equals(updateDTO.getRegistrationNumber())) {
            RegisteredBus existingBus = registeredBusRepository.findByRegistrationNumber(updateDTO.getRegistrationNumber());
            if (existingBus != null && !existingBus.getId().equals(id)) {
                throw new IllegalArgumentException("Registration number already exists: " + updateDTO.getRegistrationNumber());
            }
        }

        // Update fields
        registeredBus.setRegistrationNumber(updateDTO.getRegistrationNumber());
        registeredBus.setBusNumber(updateDTO.getBusNumber());
        registeredBus.setBusId(updateDTO.getBusId());
        registeredBus.setTrackerImei(updateDTO.getTrackerImei());
        registeredBus.setDriverId(updateDTO.getDriverId());
        registeredBus.setDriverName(updateDTO.getDriverName());
        registeredBus.setModel(updateDTO.getModel());
        registeredBus.setYear(updateDTO.getYear());
        registeredBus.setCapacity(updateDTO.getCapacity());
        registeredBus.setStatus(RegisteredBus.BusStatus.valueOf(updateDTO.getStatus().toUpperCase()));
        registeredBus.setRouteId(updateDTO.getRouteId());
        registeredBus.setRouteName(updateDTO.getRouteName());

        RegisteredBus savedBus = registeredBusRepository.save(registeredBus);

        return convertToResponseDTO(savedBus);
    }

    /**
     * Delete a registered bus
     */
    public void deleteRegisteredBus(Long id) {
        if (!registeredBusRepository.existsById(id)) {
            throw new IllegalArgumentException("Registered bus not found with ID: " + id);
        }
        registeredBusRepository.deleteById(id);
    }

    /**
     * Get registered bus by ID
     */
    public RegisteredBusResponseDTO getRegisteredBusById(Long id) {
        RegisteredBus registeredBus = registeredBusRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Registered bus not found with ID: " + id));
        return convertToResponseDTO(registeredBus);
    }

    /**
     * Get all registered buses for a company
     */
    public List<RegisteredBusResponseDTO> getRegisteredBusesByCompany(Long companyId) {
        // Validate company exists
        if (!busCompanyRepository.existsById(companyId)) {
            throw new IllegalArgumentException("Company not found with ID: " + companyId);
        }

        List<RegisteredBus> buses = registeredBusRepository.findByCompanyId(companyId);
        return buses.stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Get all registered buses
     */
    public List<RegisteredBusResponseDTO> getAllRegisteredBuses() {
        List<RegisteredBus> buses = registeredBusRepository.findAll();
        return buses.stream()
            .map(this::convertToResponseDTO)
            .collect(Collectors.toList());
    }

    /**
     * Convert entity to response DTO
     */
    private RegisteredBusResponseDTO convertToResponseDTO(RegisteredBus bus) {
        RegisteredBusResponseDTO dto = new RegisteredBusResponseDTO();
        dto.setId(bus.getId());
        dto.setCompanyId(bus.getCompany().getId());
        dto.setCompanyName(bus.getCompany().getName());
        dto.setRegistrationNumber(bus.getRegistrationNumber());
        dto.setBusNumber(bus.getBusNumber());
        dto.setBusId(bus.getBusId());
        dto.setTrackerImei(bus.getTrackerImei());
        dto.setDriverId(bus.getDriverId());
        dto.setDriverName(bus.getDriverName());
        dto.setModel(bus.getModel());
        dto.setYear(bus.getYear());
        dto.setCapacity(bus.getCapacity());
        dto.setStatus(bus.getStatus().toString());
        dto.setRouteId(bus.getRouteId());
        dto.setRouteName(bus.getRouteName());
        dto.setRouteAssignedAt(bus.getRouteAssignedAt());
        dto.setLastInspection(bus.getLastInspection());
        dto.setNextInspection(bus.getNextInspection());
        dto.setCreatedAt(bus.getCreatedAt());
        dto.setUpdatedAt(bus.getUpdatedAt());
        return dto;
    }
}