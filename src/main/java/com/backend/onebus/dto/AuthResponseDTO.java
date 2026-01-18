package com.backend.onebus.dto;

import com.backend.onebus.model.UserRole;

import java.time.Instant;

public class AuthResponseDTO {
    private String token;
    private Instant expiresAt;
    private String email;
    private String fullName;
    private UserRole role;
    private Long companyId;

    public AuthResponseDTO(String token, Instant expiresAt, String email, String fullName, UserRole role) {
        this(token, expiresAt, email, fullName, role, null);
    }

    public AuthResponseDTO(String token, Instant expiresAt, String email, String fullName, UserRole role, Long companyId) {
        this.token = token;
        this.expiresAt = expiresAt;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
        this.companyId = companyId;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public Long getCompanyId() {
        return companyId;
    }

    public void setCompanyId(Long companyId) {
        this.companyId = companyId;
    }
}
