package com.backend.onebus.controller;

import com.backend.onebus.dto.AuthResponseDTO;
import com.backend.onebus.dto.LoginRequestDTO;
import com.backend.onebus.dto.RegisterRequestDTO;
import com.backend.onebus.model.User;
import com.backend.onebus.service.AuthService;
import com.backend.onebus.service.JwtTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin
public class AuthController {

    private final AuthService authService;
    private final JwtTokenService jwtTokenService;

    public AuthController(AuthService authService, JwtTokenService jwtTokenService) {
        this.authService = authService;
        this.jwtTokenService = jwtTokenService;
    }

    // Customer registration
    @PostMapping("/register")
    public ResponseEntity<?> registerCustomer(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            AuthResponseDTO response = authService.registerCustomer(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    // Admin registration (not protected yet)
    @PostMapping("/register-admin")
    public ResponseEntity<?> registerAdmin(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            AuthResponseDTO response = authService.registerAdmin(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    // Bus company admin registration (created by an admin later; currently open)
    @PostMapping("/register-company-admin")
    public ResponseEntity<?> registerCompanyAdmin(@Valid @RequestBody RegisterRequestDTO request) {
        try {
            AuthResponseDTO response = authService.registerCompanyAdmin(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO request) {
        try {
            AuthResponseDTO response = authService.login(request);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ex.getMessage());
        }
    }

    /**
     * Get current authenticated user info
     * Requires JWT token in Authorization header
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(HttpServletRequest request) {
        try {
            // Extract token from Authorization header
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            // Verify and get user from token
            User user = authService.getUserFromToken(token);
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
            }

            // Generate new token with updated expiry
            String newToken = jwtTokenService.generateToken(user);
            
            AuthResponseDTO response = new AuthResponseDTO(
                    newToken,
                    jwtTokenService.getExpiryInstant(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole(),
                    user.getCompanyId()
            );

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Failed to verify token: " + ex.getMessage());
        }
    }

    /**
     * Refresh JWT token
     * Requires JWT token in Authorization header
     */
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Missing or invalid Authorization header");
            }

            String token = authHeader.substring(7);
            User user = authService.getUserFromToken(token);
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or expired token");
            }

            String newToken = jwtTokenService.generateToken(user);
            
            AuthResponseDTO response = new AuthResponseDTO(
                    newToken,
                    jwtTokenService.getExpiryInstant(),
                    user.getEmail(),
                    user.getFullName(),
                    user.getRole(),
                    user.getCompanyId()
            );

            return ResponseEntity.ok(response);
        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token refresh failed: " + ex.getMessage());
        }
    }
}
