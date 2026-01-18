package com.backend.onebus.dto;

import com.backend.onebus.model.UserRole;
import java.time.LocalDateTime;

public class UserResponseDTO {
    private Long id;
    private String email;
    private String fullName;
    private String surname;
    private String position;
    private UserRole role;
    private Long companyId;
    private String password; // Added for management display
    private LocalDateTime createdAt;

    public UserResponseDTO() {}

    public UserResponseDTO(Long id, String email, String fullName, String surname, String position, UserRole role, Long companyId, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.fullName = fullName;
        this.surname = surname;
        this.position = position;
        this.role = role;
        this.companyId = companyId;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getSurname() {
        return surname;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
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

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
