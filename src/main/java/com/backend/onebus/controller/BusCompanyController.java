package com.backend.onebus.controller;

import com.backend.onebus.dto.BusCompanyCreateDTO;
import com.backend.onebus.dto.BusCompanyResponseDTO;
import com.backend.onebus.service.BusCompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/bus-companies")
@Tag(name = "Bus Company Management", description = "API endpoints for managing bus companies")
public class BusCompanyController {
    
    @Autowired
    private BusCompanyService busCompanyService;
    
    @PostMapping
    @Operation(
        summary = "Create a new bus company",
        description = "Creates a new bus company with the provided details. Registration number and company code must be unique."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Bus company created successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusCompanyResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data or duplicate registration number/company code",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> createBusCompany(
            @Parameter(description = "Bus company details", required = true)
            @Valid @RequestBody BusCompanyCreateDTO createDTO) {
        try {
            BusCompanyResponseDTO createdCompany = busCompanyService.createBusCompany(createDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCompany);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while creating the bus company"));
        }
    }
    
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Create a new bus company with image",
        description = "Creates a new bus company with the provided details and an optional image file. Registration number and company code must be unique."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Bus company created successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusCompanyResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data, duplicate registration number/company code, or invalid image file",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> createBusCompanyWithImage(
            @Parameter(description = "Company name", required = true) @RequestParam String name,
            @Parameter(description = "Registration number", required = true) @RequestParam String registrationNumber,
            @Parameter(description = "Company code", required = true) @RequestParam String companyCode,
            @Parameter(description = "Email address") @RequestParam(required = false) String email,
            @Parameter(description = "Phone number") @RequestParam(required = false) String phone,
            @Parameter(description = "Address") @RequestParam(required = false) String address,
            @Parameter(description = "City") @RequestParam(required = false) String city,
            @Parameter(description = "Postal code") @RequestParam(required = false) String postalCode,
            @Parameter(description = "Country") @RequestParam(required = false) String country,
            @Parameter(description = "Is active") @RequestParam(required = false, defaultValue = "true") Boolean isActive,
            @Parameter(description = "Company logo image") @RequestParam(required = false) MultipartFile image) {
        try {
            // Create DTO from request parameters
            BusCompanyCreateDTO createDTO = new BusCompanyCreateDTO();
            createDTO.setName(name);
            createDTO.setRegistrationNumber(registrationNumber);
            createDTO.setCompanyCode(companyCode);
            createDTO.setEmail(email);
            createDTO.setPhone(phone);
            createDTO.setAddress(address);
            createDTO.setCity(city);
            createDTO.setPostalCode(postalCode);
            createDTO.setCountry(country);
            createDTO.setIsActive(isActive);
            
            BusCompanyResponseDTO createdCompany = busCompanyService.createBusCompanyWithImage(createDTO, image);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdCompany);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while creating the bus company"));
        }
    }
    
    @GetMapping
    @Operation(
        summary = "Get all bus companies",
        description = "Retrieves a list of all bus companies in the system."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of bus companies",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusCompanyResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<List<BusCompanyResponseDTO>> getAllBusCompanies() {
        List<BusCompanyResponseDTO> companies = busCompanyService.getAllBusCompanies();
        return ResponseEntity.ok(companies);
    }
    
    @GetMapping("/active")
    @Operation(
        summary = "Get all active bus companies",
        description = "Retrieves a list of all active bus companies in the system."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of active bus companies",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusCompanyResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<List<BusCompanyResponseDTO>> getActiveBusCompanies() {
        List<BusCompanyResponseDTO> companies = busCompanyService.getActiveBusCompanies();
        return ResponseEntity.ok(companies);
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Get bus company by ID",
        description = "Retrieves a specific bus company by its ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus company found",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusCompanyResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Bus company not found",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getBusCompanyById(
            @Parameter(description = "Bus company ID", required = true)
            @PathVariable Long id) {
        Optional<BusCompanyResponseDTO> company = busCompanyService.getBusCompanyById(id);
        
        if (company.isPresent()) {
            return ResponseEntity.ok(company.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Bus company not found with ID: " + id));
        }
    }
    
    @GetMapping("/registration/{registrationNumber}")
    @Operation(
        summary = "Get bus company by registration number",
        description = "Retrieves a specific bus company by its registration number."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus company found",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusCompanyResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Bus company not found",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getBusCompanyByRegistrationNumber(
            @Parameter(description = "Bus company registration number", required = true)
            @PathVariable String registrationNumber) {
        Optional<BusCompanyResponseDTO> company = busCompanyService.getBusCompanyByRegistrationNumber(registrationNumber);
        
        if (company.isPresent()) {
            return ResponseEntity.ok(company.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Bus company not found with registration number: " + registrationNumber));
        }
    }
    
    @GetMapping("/code/{companyCode}")
    @Operation(
        summary = "Get bus company by company code",
        description = "Retrieves a specific bus company by its company code."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus company found",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusCompanyResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Bus company not found",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getBusCompanyByCode(
            @Parameter(description = "Bus company code", required = true)
            @PathVariable String companyCode) {
        Optional<BusCompanyResponseDTO> company = busCompanyService.getBusCompanyByCode(companyCode);
        
        if (company.isPresent()) {
            return ResponseEntity.ok(company.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Bus company not found with code: " + companyCode));
        }
    }
    
    @GetMapping("/search")
    @Operation(
        summary = "Search bus companies by name",
        description = "Searches for bus companies by name using case-insensitive partial matching."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Search completed successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusCompanyResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid search parameters",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> searchBusCompaniesByName(
            @Parameter(description = "Name to search for", required = true)
            @RequestParam String name) {
        if (name == null || name.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Search name cannot be empty"));
        }
        
        List<BusCompanyResponseDTO> companies = busCompanyService.searchBusCompaniesByName(name.trim());
        return ResponseEntity.ok(companies);
    }
    
    @GetMapping("/city/{city}")
    @Operation(
        summary = "Get bus companies by city",
        description = "Retrieves all bus companies located in a specific city."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved bus companies by city",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusCompanyResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<List<BusCompanyResponseDTO>> getBusCompaniesByCity(
            @Parameter(description = "City name", required = true)
            @PathVariable String city) {
        List<BusCompanyResponseDTO> companies = busCompanyService.getBusCompaniesByCity(city);
        return ResponseEntity.ok(companies);
    }
    
    @PutMapping("/{id}")
    @Operation(
        summary = "Update bus company",
        description = "Updates an existing bus company with the provided details."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus company updated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusCompanyResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data or duplicate registration number/company code",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Bus company not found",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateBusCompany(
            @Parameter(description = "Bus company ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated bus company details", required = true)
            @Valid @RequestBody BusCompanyCreateDTO updateDTO) {
        try {
            BusCompanyResponseDTO updatedCompany = busCompanyService.updateBusCompany(id, updateDTO);
            return ResponseEntity.ok(updatedCompany);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while updating the bus company"));
        }
    }
    
    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(
        summary = "Update bus company with image",
        description = "Updates an existing bus company with the provided details and an optional new image file."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus company updated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusCompanyResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data, duplicate registration number/company code, or invalid image file",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Bus company not found",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateBusCompanyWithImage(
            @Parameter(description = "Bus company ID", required = true) @PathVariable Long id,
            @Parameter(description = "Company name") @RequestParam(required = false) String name,
            @Parameter(description = "Registration number") @RequestParam(required = false) String registrationNumber,
            @Parameter(description = "Company code") @RequestParam(required = false) String companyCode,
            @Parameter(description = "Email address") @RequestParam(required = false) String email,
            @Parameter(description = "Phone number") @RequestParam(required = false) String phone,
            @Parameter(description = "Address") @RequestParam(required = false) String address,
            @Parameter(description = "City") @RequestParam(required = false) String city,
            @Parameter(description = "Postal code") @RequestParam(required = false) String postalCode,
            @Parameter(description = "Country") @RequestParam(required = false) String country,
            @Parameter(description = "Is active") @RequestParam(required = false) Boolean isActive,
            @Parameter(description = "New company logo image") @RequestParam(required = false) MultipartFile image) {
        try {
            // Create DTO from request parameters (only include non-null values)
            BusCompanyCreateDTO updateDTO = new BusCompanyCreateDTO();
            if (name != null) updateDTO.setName(name);
            if (registrationNumber != null) updateDTO.setRegistrationNumber(registrationNumber);
            if (companyCode != null) updateDTO.setCompanyCode(companyCode);
            if (email != null) updateDTO.setEmail(email);
            if (phone != null) updateDTO.setPhone(phone);
            if (address != null) updateDTO.setAddress(address);
            if (city != null) updateDTO.setCity(city);
            if (postalCode != null) updateDTO.setPostalCode(postalCode);
            if (country != null) updateDTO.setCountry(country);
            if (isActive != null) updateDTO.setIsActive(isActive);
            
            BusCompanyResponseDTO updatedCompany = busCompanyService.updateBusCompanyWithImage(id, updateDTO, image);
            return ResponseEntity.ok(updatedCompany);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while updating the bus company"));
        }
    }
    
    @PatchMapping("/{id}/deactivate")
    @Operation(
        summary = "Deactivate bus company",
        description = "Deactivates a bus company, making it inactive in the system."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus company deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "Bus company not found",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> deactivateBusCompany(
            @Parameter(description = "Bus company ID", required = true)
            @PathVariable Long id) {
        try {
            busCompanyService.deactivateBusCompany(id);
            return ResponseEntity.ok().body(new SuccessResponse("Bus company deactivated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while deactivating the bus company"));
        }
    }
    
    @PatchMapping("/{id}/activate")
    @Operation(
        summary = "Activate bus company",
        description = "Activates a bus company, making it active in the system."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus company activated successfully"),
        @ApiResponse(responseCode = "404", description = "Bus company not found",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> activateBusCompany(
            @Parameter(description = "Bus company ID", required = true)
            @PathVariable Long id) {
        try {
            busCompanyService.activateBusCompany(id);
            return ResponseEntity.ok().body(new SuccessResponse("Bus company activated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while activating the bus company"));
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete bus company",
        description = "Permanently deletes a bus company from the system."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus company deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Bus company not found",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> deleteBusCompany(
            @Parameter(description = "Bus company ID", required = true)
            @PathVariable Long id) {
        try {
            busCompanyService.deleteBusCompany(id);
            return ResponseEntity.ok().body(new SuccessResponse("Bus company deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while deleting the bus company"));
        }
    }
    
    // Helper classes for responses
    public static class ErrorResponse {
        private String error;
        private long timestamp;
        
        public ErrorResponse(String error) {
            this.error = error;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getError() {
            return error;
        }
        
        public void setError(String error) {
            this.error = error;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
    
    public static class SuccessResponse {
        private String message;
        private long timestamp;
        
        public SuccessResponse(String message) {
            this.message = message;
            this.timestamp = System.currentTimeMillis();
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public long getTimestamp() {
            return timestamp;
        }
        
        public void setTimestamp(long timestamp) {
            this.timestamp = timestamp;
        }
    }
}
