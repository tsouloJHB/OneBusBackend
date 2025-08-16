package com.backend.onebus.service;

import com.backend.onebus.dto.BusNumberCreateDTO;
import com.backend.onebus.dto.BusNumberResponseDTO;
import com.backend.onebus.model.BusNumber;
import com.backend.onebus.repository.BusNumberRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.LinkedHashMap;

@Service
@Transactional
public class BusNumberService {
    
    @Autowired
    private BusNumberRepository busNumberRepository;
    
    /**
     * Create a new bus number
     */
    public BusNumberResponseDTO createBusNumber(BusNumberCreateDTO createDTO) {
        // Check if bus number already exists for this company
        if (busNumberRepository.existsByBusNumberAndCompanyName(createDTO.getBusNumber(), createDTO.getCompanyName())) {
            throw new IllegalArgumentException("Bus number " + createDTO.getBusNumber() + 
                                             " already exists for company: " + createDTO.getCompanyName());
        }
        
        // Convert DTO to entity
        BusNumber busNumber = convertCreateDTOToEntity(createDTO);
        
        // Save the entity
        BusNumber savedBusNumber = busNumberRepository.save(busNumber);
        
        // Convert and return response DTO
        return convertEntityToResponseDTO(savedBusNumber);
    }
    
    /**
     * Get all bus numbers
     */
    @Transactional(readOnly = true)
    public List<BusNumberResponseDTO> getAllBusNumbers() {
        List<BusNumber> busNumbers = busNumberRepository.findAll();
        return busNumbers.stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all active bus numbers
     */
    @Transactional(readOnly = true)
    public List<BusNumberResponseDTO> getActiveBusNumbers() {
        List<BusNumber> busNumbers = busNumberRepository.findByIsActiveTrue();
        return busNumbers.stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get bus number by ID
     */
    @Transactional(readOnly = true)
    public Optional<BusNumberResponseDTO> getBusNumberById(Long id) {
        return busNumberRepository.findById(id)
                .map(this::convertEntityToResponseDTO);
    }
    
    /**
     * Get bus numbers by company name
     */
    @Transactional(readOnly = true)
    public List<BusNumberResponseDTO> getBusNumbersByCompany(String companyName) {
        List<BusNumber> busNumbers = busNumberRepository.findByCompanyNameIgnoreCase(companyName);
        return busNumbers.stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get active bus numbers by company name
     */
    @Transactional(readOnly = true)
    public List<BusNumberResponseDTO> getActiveBusNumbersByCompany(String companyName) {
        List<BusNumber> busNumbers = busNumberRepository.findByCompanyNameAndIsActiveTrue(companyName);
        return busNumbers.stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get bus numbers by route name
     */
    @Transactional(readOnly = true)
    public List<BusNumberResponseDTO> getBusNumbersByRoute(String routeName) {
        List<BusNumber> busNumbers = busNumberRepository.findByRouteNameContainingIgnoreCase(routeName);
        return busNumbers.stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get bus numbers by destination
     */
    @Transactional(readOnly = true)
    public List<BusNumberResponseDTO> getBusNumbersByDestination(String destination) {
        List<BusNumber> busNumbers = busNumberRepository.findByDestination(destination);
        return busNumbers.stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all routes (both directions) for a specific bus number and company
     */
    @Transactional(readOnly = true)
    public List<BusNumberResponseDTO> getAllRoutesByBusNumberAndCompany(String busNumber, String companyName) {
        List<BusNumber> busNumbers = busNumberRepository.findAllByBusNumberAndCompanyName(busNumber, companyName);
        return busNumbers.stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get bus numbers grouped by company
     */
    @Transactional(readOnly = true)
    public Map<String, List<BusNumberResponseDTO>> getBusNumbersGroupedByCompany() {
        List<BusNumber> busNumbers = busNumberRepository.findAllActiveOrderedByCompanyAndBusNumber();
        Map<String, List<BusNumberResponseDTO>> groupedBusNumbers = new LinkedHashMap<>();
        
        for (BusNumber busNumber : busNumbers) {
            String companyName = busNumber.getCompanyName();
            BusNumberResponseDTO responseDTO = convertEntityToResponseDTO(busNumber);
            
            groupedBusNumbers.computeIfAbsent(companyName, k -> new java.util.ArrayList<>()).add(responseDTO);
        }
        
        return groupedBusNumbers;
    }
    
    /**
     * Update bus number
     */
    public BusNumberResponseDTO updateBusNumber(Long id, BusNumberCreateDTO updateDTO) {
        BusNumber existingBusNumber = busNumberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bus number not found with ID: " + id));
        
        // Check if bus number is being changed and already exists for this company
        if (!existingBusNumber.getBusNumber().equals(updateDTO.getBusNumber()) || 
            !existingBusNumber.getCompanyName().equals(updateDTO.getCompanyName())) {
            if (busNumberRepository.existsByBusNumberAndCompanyName(updateDTO.getBusNumber(), updateDTO.getCompanyName())) {
                throw new IllegalArgumentException("Bus number " + updateDTO.getBusNumber() + 
                                                 " already exists for company: " + updateDTO.getCompanyName());
            }
        }
        
        // Update the existing entity
        updateEntityFromDTO(existingBusNumber, updateDTO);
        
        // Save the updated entity
        BusNumber updatedBusNumber = busNumberRepository.save(existingBusNumber);
        
        // Convert and return response DTO
        return convertEntityToResponseDTO(updatedBusNumber);
    }
    
    /**
     * Deactivate bus number
     */
    public void deactivateBusNumber(Long id) {
        BusNumber busNumber = busNumberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bus number not found with ID: " + id));
        
        busNumber.setIsActive(false);
        busNumberRepository.save(busNumber);
    }
    
    /**
     * Activate bus number
     */
    public void activateBusNumber(Long id) {
        BusNumber busNumber = busNumberRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bus number not found with ID: " + id));
        
        busNumber.setIsActive(true);
        busNumberRepository.save(busNumber);
    }
    
    /**
     * Delete bus number
     */
    public void deleteBusNumber(Long id) {
        if (!busNumberRepository.existsById(id)) {
            throw new IllegalArgumentException("Bus number not found with ID: " + id);
        }
        busNumberRepository.deleteById(id);
    }
    
    /**
     * Search bus numbers by company name
     */
    @Transactional(readOnly = true)
    public List<BusNumberResponseDTO> searchBusNumbersByCompany(String companyName) {
        List<BusNumber> busNumbers = busNumberRepository.findByCompanyNameContainingIgnoreCase(companyName);
        return busNumbers.stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get count of bus numbers by company
     */
    @Transactional(readOnly = true)
    public Long countBusNumbersByCompany(String companyName) {
        return busNumberRepository.countByCompanyName(companyName);
    }
    
    // Helper methods for conversion
    private BusNumber convertCreateDTOToEntity(BusNumberCreateDTO dto) {
        BusNumber entity = new BusNumber();
        entity.setBusNumber(dto.getBusNumber());
        entity.setCompanyName(dto.getCompanyName());
        entity.setRouteName(dto.getRouteName());
        entity.setDescription(dto.getDescription());
        entity.setStartDestination(dto.getStartDestination());
        entity.setEndDestination(dto.getEndDestination());
        entity.setDirection(dto.getDirection());
        entity.setDistanceKm(dto.getDistanceKm());
        entity.setEstimatedDurationMinutes(dto.getEstimatedDurationMinutes());
        entity.setFrequencyMinutes(dto.getFrequencyMinutes());
        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return entity;
    }
    
    private BusNumberResponseDTO convertEntityToResponseDTO(BusNumber entity) {
        BusNumberResponseDTO dto = new BusNumberResponseDTO();
        dto.setId(entity.getId());
        dto.setBusNumber(entity.getBusNumber());
        dto.setCompanyName(entity.getCompanyName());
        dto.setRouteName(entity.getRouteName());
        dto.setDescription(entity.getDescription());
        dto.setStartDestination(entity.getStartDestination());
        dto.setEndDestination(entity.getEndDestination());
        dto.setDirection(entity.getDirection());
        dto.setDistanceKm(entity.getDistanceKm());
        dto.setEstimatedDurationMinutes(entity.getEstimatedDurationMinutes());
        dto.setFrequencyMinutes(entity.getFrequencyMinutes());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        return dto;
    }
    
    private void updateEntityFromDTO(BusNumber entity, BusNumberCreateDTO dto) {
        entity.setBusNumber(dto.getBusNumber());
        entity.setCompanyName(dto.getCompanyName());
        entity.setRouteName(dto.getRouteName());
        entity.setDescription(dto.getDescription());
        entity.setStartDestination(dto.getStartDestination());
        entity.setEndDestination(dto.getEndDestination());
        entity.setDirection(dto.getDirection());
        entity.setDistanceKm(dto.getDistanceKm());
        entity.setEstimatedDurationMinutes(dto.getEstimatedDurationMinutes());
        entity.setFrequencyMinutes(dto.getFrequencyMinutes());
        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }
    }
}
