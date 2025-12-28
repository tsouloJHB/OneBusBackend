package com.backend.onebus.service;

import com.backend.onebus.dto.AuthResponseDTO;
import com.backend.onebus.dto.LoginRequestDTO;
import com.backend.onebus.dto.RegisterRequestDTO;
import com.backend.onebus.model.User;
import com.backend.onebus.model.UserRole;
import com.backend.onebus.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final DashboardStatsService dashboardStatsService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       DashboardStatsService dashboardStatsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.dashboardStatsService = dashboardStatsService;
    }

    public AuthResponseDTO registerCustomer(RegisterRequestDTO request) {
        return registerWithRole(request, UserRole.CUSTOMER);
    }

    public AuthResponseDTO registerAdmin(RegisterRequestDTO request) {
        return registerWithRole(request, UserRole.ADMIN);
    }

    public AuthResponseDTO registerCompanyAdmin(RegisterRequestDTO request) {
        return registerWithRole(request, UserRole.COMPANY_ADMIN);
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtTokenService.generateToken(user);
        return new AuthResponseDTO(token, jwtTokenService.getExpiryInstant(), user.getEmail(), user.getFullName(), user.getRole());
    }

    /**
     * Extract user information from JWT token
     * Used for token verification endpoints (/api/auth/me, /api/auth/refresh)
     */
    public User getUserFromToken(String token) {
        try {
            // Check if token is expired
            if (jwtTokenService.isTokenExpired(token)) {
                throw new IllegalArgumentException("Token has expired");
            }

            // Extract email from token
            String email = jwtTokenService.getEmailFromToken(token);

            // Fetch user from database
            return userRepository.findByEmail(email)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
        } catch (JwtException ex) {
            throw new IllegalArgumentException("Invalid token: " + ex.getMessage());
        }
    }

    private AuthResponseDTO registerWithRole(RegisterRequestDTO request, UserRole role) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email already registered");
        }

        User user = new User();
        user.setEmail(request.getEmail());
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));

        User saved = userRepository.save(user);
        
        // Update dashboard stats
        dashboardStatsService.incrementUsers();
        
        String token = jwtTokenService.generateToken(saved);
        return new AuthResponseDTO(token, jwtTokenService.getExpiryInstant(), saved.getEmail(), saved.getFullName(), saved.getRole());
    }
}
