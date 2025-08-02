package com.backend.onebus.service;

import com.backend.onebus.dto.BusCompanyCreateDTO;
import com.backend.onebus.dto.BusCompanyResponseDTO;
import com.backend.onebus.model.BusCompany;
import com.backend.onebus.repository.BusCompanyRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class BusCompanyService {
    
    @Autowired
    private BusCompanyRepository busCompanyRepository;
    
    /**
     * Create a new bus company
     */
    public BusCompanyResponseDTO createBusCompany(BusCompanyCreateDTO createDTO) {
        // Check if registration number already exists
        if (busCompanyRepository.existsByRegistrationNumber(createDTO.getRegistrationNumber())) {
            throw new IllegalArgumentException("Registration number already exists: " + createDTO.getRegistrationNumber());
        }
        
        // Check if company code already exists
        if (busCompanyRepository.existsByCompanyCode(createDTO.getCompanyCode())) {
            throw new IllegalArgumentException("Company code already exists: " + createDTO.getCompanyCode());
        }
        
        // Check if email already exists (if provided)
        if (createDTO.getEmail() != null && !createDTO.getEmail().trim().isEmpty()) {
            Optional<BusCompany> existingByEmail = busCompanyRepository.findByEmailIgnoreCase(createDTO.getEmail());
            if (existingByEmail.isPresent()) {
                throw new IllegalArgumentException("Email already exists: " + createDTO.getEmail());
            }
        }
        
        // Convert DTO to entity
        BusCompany busCompany = convertCreateDTOToEntity(createDTO);
        
        // Save the entity
        BusCompany savedCompany = busCompanyRepository.save(busCompany);
        
        // Convert and return response DTO
        return convertEntityToResponseDTO(savedCompany);
    }
    
    /**
     * Get all bus companies
     */
    @Transactional(readOnly = true)
    public List<BusCompanyResponseDTO> getAllBusCompanies() {
        List<BusCompany> companies = busCompanyRepository.findAll();
        return companies.stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get all active bus companies
     */
    @Transactional(readOnly = true)
    public List<BusCompanyResponseDTO> getActiveBusCompanies() {
        List<BusCompany> companies = busCompanyRepository.findByIsActiveTrue();
        return companies.stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get bus company by ID
     */
    @Transactional(readOnly = true)
    public Optional<BusCompanyResponseDTO> getBusCompanyById(Long id) {
        return busCompanyRepository.findById(id)
                .map(this::convertEntityToResponseDTO);
    }
    
    /**
     * Get bus company by registration number
     */
    @Transactional(readOnly = true)
    public Optional<BusCompanyResponseDTO> getBusCompanyByRegistrationNumber(String registrationNumber) {
        return busCompanyRepository.findByRegistrationNumber(registrationNumber)
                .map(this::convertEntityToResponseDTO);
    }
    
    /**
     * Get bus company by company code
     */
    @Transactional(readOnly = true)
    public Optional<BusCompanyResponseDTO> getBusCompanyByCode(String companyCode) {
        return busCompanyRepository.findByCompanyCode(companyCode)
                .map(this::convertEntityToResponseDTO);
    }
    
    /**
     * Search bus companies by name
     */
    @Transactional(readOnly = true)
    public List<BusCompanyResponseDTO> searchBusCompaniesByName(String name) {
        List<BusCompany> companies = busCompanyRepository.findByNameContainingIgnoreCase(name);
        return companies.stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Get bus companies by city
     */
    @Transactional(readOnly = true)
    public List<BusCompanyResponseDTO> getBusCompaniesByCity(String city) {
        List<BusCompany> companies = busCompanyRepository.findByCityIgnoreCase(city);
        return companies.stream()
                .map(this::convertEntityToResponseDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * Update bus company
     */
    public BusCompanyResponseDTO updateBusCompany(Long id, BusCompanyCreateDTO updateDTO) {
        BusCompany existingCompany = busCompanyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bus company not found with ID: " + id));
        
        // Check if registration number is being changed and already exists
        if (!existingCompany.getRegistrationNumber().equals(updateDTO.getRegistrationNumber())) {
            if (busCompanyRepository.existsByRegistrationNumber(updateDTO.getRegistrationNumber())) {
                throw new IllegalArgumentException("Registration number already exists: " + updateDTO.getRegistrationNumber());
            }
        }
        
        // Check if company code is being changed and already exists
        if (!existingCompany.getCompanyCode().equals(updateDTO.getCompanyCode())) {
            if (busCompanyRepository.existsByCompanyCode(updateDTO.getCompanyCode())) {
                throw new IllegalArgumentException("Company code already exists: " + updateDTO.getCompanyCode());
            }
        }
        
        // Check if email is being changed and already exists
        if (updateDTO.getEmail() != null && !updateDTO.getEmail().trim().isEmpty()) {
            if (existingCompany.getEmail() == null || !existingCompany.getEmail().equalsIgnoreCase(updateDTO.getEmail())) {
                Optional<BusCompany> existingByEmail = busCompanyRepository.findByEmailIgnoreCase(updateDTO.getEmail());
                if (existingByEmail.isPresent()) {
                    throw new IllegalArgumentException("Email already exists: " + updateDTO.getEmail());
                }
            }
        }
        
        // Update the existing company
        updateEntityFromDTO(existingCompany, updateDTO);
        
        // Save the updated entity
        BusCompany updatedCompany = busCompanyRepository.save(existingCompany);
        
        // Convert and return response DTO
        return convertEntityToResponseDTO(updatedCompany);
    }
    
    /**
     * Deactivate bus company
     */
    public void deactivateBusCompany(Long id) {
        BusCompany company = busCompanyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bus company not found with ID: " + id));
        
        company.setIsActive(false);
        busCompanyRepository.save(company);
    }
    
    /**
     * Activate bus company
     */
    public void activateBusCompany(Long id) {
        BusCompany company = busCompanyRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Bus company not found with ID: " + id));
        
        company.setIsActive(true);
        busCompanyRepository.save(company);
    }
    
    /**
     * Delete bus company
     */
    public void deleteBusCompany(Long id) {
        if (!busCompanyRepository.existsById(id)) {
            throw new IllegalArgumentException("Bus company not found with ID: " + id);
        }
        busCompanyRepository.deleteById(id);
    }
    
    // Helper methods for conversion
    private BusCompany convertCreateDTOToEntity(BusCompanyCreateDTO dto) {
        BusCompany entity = new BusCompany();
        entity.setName(dto.getName());
        entity.setRegistrationNumber(dto.getRegistrationNumber());
        entity.setCompanyCode(dto.getCompanyCode());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setAddress(dto.getAddress());
        entity.setCity(dto.getCity());
        entity.setPostalCode(dto.getPostalCode());
        entity.setCountry(dto.getCountry());
        entity.setIsActive(dto.getIsActive() != null ? dto.getIsActive() : true);
        return entity;
    }
    
    private BusCompanyResponseDTO convertEntityToResponseDTO(BusCompany entity) {
        BusCompanyResponseDTO dto = new BusCompanyResponseDTO();
        dto.setId(entity.getId());
        dto.setName(entity.getName());
        dto.setRegistrationNumber(entity.getRegistrationNumber());
        dto.setCompanyCode(entity.getCompanyCode());
        dto.setEmail(entity.getEmail());
        dto.setPhone(entity.getPhone());
        dto.setAddress(entity.getAddress());
        dto.setCity(entity.getCity());
        dto.setPostalCode(entity.getPostalCode());
        dto.setCountry(entity.getCountry());
        dto.setIsActive(entity.getIsActive());
        dto.setCreatedAt(entity.getCreatedAt());
        dto.setUpdatedAt(entity.getUpdatedAt());
        
        // Set bus count if buses are loaded
        if (entity.getBuses() != null) {
            dto.setBusCount((long) entity.getBuses().size());
        } else {
            dto.setBusCount(0L);
        }
        
        return dto;
    }
    
    private void updateEntityFromDTO(BusCompany entity, BusCompanyCreateDTO dto) {
        entity.setName(dto.getName());
        entity.setRegistrationNumber(dto.getRegistrationNumber());
        entity.setCompanyCode(dto.getCompanyCode());
        entity.setEmail(dto.getEmail());
        entity.setPhone(dto.getPhone());
        entity.setAddress(dto.getAddress());
        entity.setCity(dto.getCity());
        entity.setPostalCode(dto.getPostalCode());
        entity.setCountry(dto.getCountry());
        if (dto.getIsActive() != null) {
            entity.setIsActive(dto.getIsActive());
        }
    }
}
