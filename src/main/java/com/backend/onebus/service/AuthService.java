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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Transactional
public class AuthService {
    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final com.backend.onebus.repository.BusCompanyRepository busCompanyRepository;
    private final DashboardStatsService dashboardStatsService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenService jwtTokenService,
                       com.backend.onebus.repository.BusCompanyRepository busCompanyRepository,
                       DashboardStatsService dashboardStatsService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.busCompanyRepository = busCompanyRepository;
        this.dashboardStatsService = dashboardStatsService;
    }

    public AuthResponseDTO registerCustomer(RegisterRequestDTO request) {
        return registerWithRole(request, UserRole.CUSTOMER);
    }

    public AuthResponseDTO registerAdmin(RegisterRequestDTO request) {
        return registerWithRole(request, UserRole.ADMIN);
    }

    public AuthResponseDTO registerFleetManager(RegisterRequestDTO request) {
        if (request.getCompanyId() == null) {
            throw new IllegalArgumentException("Company ID is required for fleet manager");
        }
        
        // Find company to get name for password generation
        com.backend.onebus.model.BusCompany company = busCompanyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
                
        // Auto-generate password: [CompanyName]onebus#[Year]
        String cleanCompanyName = company.getName().replaceAll("\\s+", "");
        int year = java.time.Year.now().getValue();
        String autoPassword = String.format("%sonebus#%d", cleanCompanyName, year);
        
        request.setPassword(autoPassword);
        
        return registerWithRole(request, UserRole.FLEET_MANAGER);
    }

    public AuthResponseDTO registerCompanyAdmin(RegisterRequestDTO request) {
        if (request.getCompanyId() == null) {
            throw new IllegalArgumentException("Company ID is required for company admin");
        }
        
        // Find company to get name for password generation
        com.backend.onebus.model.BusCompany company = busCompanyRepository.findById(request.getCompanyId())
                .orElseThrow(() -> new IllegalArgumentException("Company not found"));
                
        // Auto-generate password: [CompanyName]onebus#[Year]
        String cleanCompanyName = company.getName().replaceAll("\\s+", "");
        int year = java.time.Year.now().getValue();
        String autoPassword = String.format("%sonebus#%d", cleanCompanyName, year);
        
        request.setPassword(autoPassword);
        
        return registerWithRole(request, UserRole.ADMIN); // Fixed: Should be ADMIN or COMPANY_ADMIN (if added), but currently using ADMIN for company admins
    }

    public AuthResponseDTO login(LoginRequestDTO request) {
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));

        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String token = jwtTokenService.generateToken(user);
        return new AuthResponseDTO(token, jwtTokenService.getExpiryInstant(), user.getEmail(), user.getFullName(), user.getRole(), user.getCompanyId());
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
        String normalizedEmail = request.getEmail().trim().toLowerCase();
        logger.debug("[AuthService] Registering user with email: {} and role: {}", normalizedEmail, role);
        
        if (userRepository.existsByEmail(normalizedEmail)) {
            logger.warn("[AuthService] Registration failed: Email {} already exists", normalizedEmail);
            throw new IllegalArgumentException("Email already registered");
        }
        
        if (request.getPassword() == null || request.getPassword().isEmpty()) {
            throw new IllegalArgumentException("Password is required");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setFullName(request.getFullName());
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setRawPassword(request.getPassword()); // Store raw password for display
        
        // Set additional fields
        if (request.getCompanyId() != null) {
            user.setCompanyId(request.getCompanyId());
        }
        if (request.getSurname() != null) {
            user.setSurname(request.getSurname());
        }
        if (request.getPosition() != null) {
            user.setPosition(request.getPosition());
        }

        User saved = userRepository.save(user);
        logger.info("[AuthService] Registration successful: User saved with ID {} and email {}", saved.getId(), saved.getEmail());
        
        // Update dashboard stats (now asynchronous)
        dashboardStatsService.incrementUsers();
        
        String token = jwtTokenService.generateToken(saved);
        return new AuthResponseDTO(token, jwtTokenService.getExpiryInstant(), saved.getEmail(), saved.getFullName(), saved.getRole(), saved.getCompanyId());
    }

    public boolean emailExists(String email) {
        String normalizedEmail = email.trim().toLowerCase();
        boolean exists = userRepository.existsByEmail(normalizedEmail);
        logger.debug("[AuthService] Debug check for email '{}': {}", normalizedEmail, exists);
        return exists;
    }
}
