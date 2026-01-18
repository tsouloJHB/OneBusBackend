package com.backend.onebus.controller;

import com.backend.onebus.dto.BusNumberCreateDTO;
import com.backend.onebus.dto.BusNumberResponseDTO;
import com.backend.onebus.service.BusNumberService;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/bus-numbers")
@Tag(name = "Bus Number Management", description = "API endpoints for managing bus numbers with route information")
public class BusNumberController {
    
    @Autowired
    private BusNumberService busNumberService;
    
    // Error response class for consistent error handling
    public static class ErrorResponse {
        private String message;
        
        public ErrorResponse(String message) {
            this.message = message;
        }
        
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    @PostMapping
    @Operation(
        summary = "Create a new bus number",
        description = "Creates a new bus number with route information. Bus number must be unique within each company."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Bus number created successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusNumberResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data or duplicate bus number",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> createBusNumber(
            @Parameter(description = "Bus number details", required = true)
            @Valid @RequestBody BusNumberCreateDTO createDTO) {
        try {
            BusNumberResponseDTO createdBusNumber = busNumberService.createBusNumber(createDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(createdBusNumber);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while creating the bus number"));
        }
    }
    
    @GetMapping
    @Operation(
        summary = "Get all bus numbers",
        description = "Retrieves a list of all bus numbers in the system."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of bus numbers",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusNumberResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<List<BusNumberResponseDTO>> getAllBusNumbers(jakarta.servlet.http.HttpServletRequest request) {
        // Get user role and company ID from JWT token
        String userRole = (String) request.getAttribute("userRole");
        Long userCompanyId = (Long) request.getAttribute("userCompanyId");
        
        List<BusNumberResponseDTO> busNumbers;
        
        // Fleet managers can only see their company's buses
        if ("FLEET_MANAGER".equals(userRole) && userCompanyId != null) {
            busNumbers = busNumberService.getBusNumbersByCompany(userCompanyId);
        } else {
            busNumbers = busNumberService.getAllBusNumbers();
        }
        
        return ResponseEntity.ok(busNumbers);
    }
    
    @GetMapping("/active")
    @Operation(
        summary = "Get all active bus numbers",
        description = "Retrieves a list of all active bus numbers in the system."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved list of active bus numbers",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusNumberResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<List<BusNumberResponseDTO>> getActiveBusNumbers(jakarta.servlet.http.HttpServletRequest request) {
        // Get user role and company ID from JWT token
        String userRole = (String) request.getAttribute("userRole");
        Long userCompanyId = (Long) request.getAttribute("userCompanyId");
        
        List<BusNumberResponseDTO> busNumbers;
        
        // Fleet managers can only see their company's active buses
        if ("FLEET_MANAGER".equals(userRole) && userCompanyId != null) {
            busNumbers = busNumberService.getActiveBusNumbersByCompany(userCompanyId);
        } else {
            busNumbers = busNumberService.getActiveBusNumbers();
        }
        
        return ResponseEntity.ok(busNumbers);
    }
    
    @GetMapping("/{id}")
    @Operation(
        summary = "Get bus number by ID",
        description = "Retrieves a specific bus number by its ID."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus number found",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusNumberResponseDTO.class))),
        @ApiResponse(responseCode = "404", description = "Bus number not found",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> getBusNumberById(
            @Parameter(description = "Bus number ID", required = true)
            @PathVariable Long id) {
        Optional<BusNumberResponseDTO> busNumber = busNumberService.getBusNumberById(id);
        
        if (busNumber.isPresent()) {
            return ResponseEntity.ok(busNumber.get());
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorResponse("Bus number not found with ID: " + id));
        }
    }
    
    @GetMapping("/company/{busCompanyId}")
    @Operation(
        summary = "Get bus numbers by company name",
        description = "Retrieves all bus numbers for a specific company."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved bus numbers for company",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusNumberResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<List<BusNumberResponseDTO>> getBusNumbersByCompany(
            @Parameter(description = "Company ID", required = true)
            @PathVariable Long busCompanyId) {
        List<BusNumberResponseDTO> busNumbers = busNumberService.getBusNumbersByCompany(busCompanyId);
        return ResponseEntity.ok(busNumbers);
    }
    
    @GetMapping("/company/{busCompanyId}/active")
    @Operation(
        summary = "Get active bus numbers by company name",
        description = "Retrieves all active bus numbers for a specific company."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved active bus numbers for company",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusNumberResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<List<BusNumberResponseDTO>> getActiveBusNumbersByCompany(
            @Parameter(description = "Company ID", required = true)
            @PathVariable Long busCompanyId) {
        List<BusNumberResponseDTO> busNumbers = busNumberService.getActiveBusNumbersByCompany(busCompanyId);
        return ResponseEntity.ok(busNumbers);
    }
    
    @GetMapping("/grouped-by-company")
    @Operation(
        summary = "Get bus numbers grouped by company",
        description = "Retrieves all active bus numbers grouped by company name."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved bus numbers grouped by company",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<Map<String, List<BusNumberResponseDTO>>> getBusNumbersGroupedByCompany() {
        Map<String, List<BusNumberResponseDTO>> groupedBusNumbers = busNumberService.getBusNumbersGroupedByCompany();
        return ResponseEntity.ok(groupedBusNumbers);
    }
    
    @GetMapping("/route/{routeName}")
    @Operation(
        summary = "Get bus numbers by route name",
        description = "Retrieves all bus numbers for a specific route (partial match)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved bus numbers for route",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusNumberResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<List<BusNumberResponseDTO>> getBusNumbersByRoute(
            @Parameter(description = "Route name", required = true)
            @PathVariable String routeName) {
        List<BusNumberResponseDTO> busNumbers = busNumberService.getBusNumbersByRoute(routeName);
        return ResponseEntity.ok(busNumbers);
    }
    
    @GetMapping("/destination/{destination}")
    @Operation(
        summary = "Get bus numbers by destination",
        description = "Retrieves all bus numbers that serve a specific destination (start or end)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved bus numbers for destination",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusNumberResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<List<BusNumberResponseDTO>> getBusNumbersByDestination(
            @Parameter(description = "Destination name", required = true)
            @PathVariable String destination) {
        List<BusNumberResponseDTO> busNumbers = busNumberService.getBusNumbersByDestination(destination);
        return ResponseEntity.ok(busNumbers);
    }
    
    @GetMapping("/search/company")
    @Operation(
        summary = "Search bus numbers by company name",
        description = "Searches for bus numbers by company name (partial match)."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved search results",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusNumberResponseDTO.class))),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<List<BusNumberResponseDTO>> searchBusNumbersByCompany(
            @Parameter(description = "Company name to search for", required = true)
            @RequestParam String busCompanyId) {
        List<BusNumberResponseDTO> busNumbers = busNumberService.searchBusNumbersByCompany(busCompanyId);
        return ResponseEntity.ok(busNumbers);
    }
    
    @GetMapping("/company/{busCompanyId}/count")
    @Operation(
        summary = "Get count of bus numbers by company",
        description = "Returns the count of bus numbers for a specific company."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved count"),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<Map<String, Object>> countBusNumbersByCompany(
            @Parameter(description = "Company ID", required = true)
            @PathVariable Long busCompanyId) {
        Long count = busNumberService.countBusNumbersByCompany(busCompanyId);
        Map<String, Object> response = Map.of(
            "busCompanyId", busCompanyId,
            "count", count
        );
        return ResponseEntity.ok(response);
    }
    
    @PutMapping("/{id}")
    @Operation(
        summary = "Update bus number",
        description = "Updates an existing bus number with new information."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus number updated successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = BusNumberResponseDTO.class))),
        @ApiResponse(responseCode = "400", description = "Invalid input data or duplicate bus number",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "404", description = "Bus number not found",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> updateBusNumber(
            @Parameter(description = "Bus number ID", required = true)
            @PathVariable Long id,
            @Parameter(description = "Updated bus number details", required = true)
            @Valid @RequestBody BusNumberCreateDTO updateDTO) {
        try {
            BusNumberResponseDTO updatedBusNumber = busNumberService.updateBusNumber(id, updateDTO);
            return ResponseEntity.ok(updatedBusNumber);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while updating the bus number"));
        }
    }
    
    @PatchMapping("/{id}/deactivate")
    @Operation(
        summary = "Deactivate bus number",
        description = "Deactivates a bus number, making it inactive in the system."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus number deactivated successfully"),
        @ApiResponse(responseCode = "404", description = "Bus number not found",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> deactivateBusNumber(
            @Parameter(description = "Bus number ID", required = true)
            @PathVariable Long id) {
        try {
            busNumberService.deactivateBusNumber(id);
            return ResponseEntity.ok(Map.of("message", "Bus number deactivated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while deactivating the bus number"));
        }
    }
    
    @PatchMapping("/{id}/activate")
    @Operation(
        summary = "Activate bus number",
        description = "Activates a bus number, making it active in the system."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus number activated successfully"),
        @ApiResponse(responseCode = "404", description = "Bus number not found",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> activateBusNumber(
            @Parameter(description = "Bus number ID", required = true)
            @PathVariable Long id) {
        try {
            busNumberService.activateBusNumber(id);
            return ResponseEntity.ok(Map.of("message", "Bus number activated successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while activating the bus number"));
        }
    }
    
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete bus number",
        description = "Permanently deletes a bus number from the system."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Bus number deleted successfully"),
        @ApiResponse(responseCode = "404", description = "Bus number not found",
                content = @Content(mediaType = "application/json")),
        @ApiResponse(responseCode = "500", description = "Internal server error",
                content = @Content(mediaType = "application/json"))
    })
    public ResponseEntity<?> deleteBusNumber(
            @Parameter(description = "Bus number ID", required = true)
            @PathVariable Long id) {
        try {
            busNumberService.deleteBusNumber(id);
            return ResponseEntity.ok(Map.of("message", "Bus number deleted successfully"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponse("An error occurred while deleting the bus number"));
        }
    }
}
