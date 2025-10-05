package com.backend.onebus.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDateTime;
import java.util.Set;

@Entity
@Table(name = "bus_companies")
public class BusCompany {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @NotBlank(message = "Registration number is required")
    @Size(min = 2, max = 50, message = "Registration number must be between 2 and 50 characters")
    @Column(name = "registration_number", nullable = false, unique = true, length = 50)
    private String registrationNumber;
    
    @NotBlank(message = "Company code is required")
    @Size(min = 2, max = 10, message = "Company code must be between 2 and 10 characters")
    @Column(name = "company_code", nullable = false, unique = true, length = 10)
    private String companyCode;
    
    @Email(message = "Please provide a valid email address")
    @Column(name = "email", length = 100)
    private String email;
    
    // Allow digits and common phone punctuation (spaces, hyphens, parentheses), optional leading +
    // Minimum 7 and maximum 20 characters overall (digits + punctuation)
    @Pattern(regexp = "^[+]?[0-9()\\-\\s]{7,20}$", message = "Please provide a valid phone number")
    @Column(name = "phone", length = 20)
    private String phone;
    
    @Size(max = 200, message = "Address cannot exceed 200 characters")
    @Column(name = "address", length = 200)
    private String address;
    
    @Size(max = 50, message = "City cannot exceed 50 characters")
    @Column(name = "city", length = 50)
    private String city;
    
    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    @Column(name = "postal_code", length = 20)
    private String postalCode;
    
    @Size(max = 50, message = "Country cannot exceed 50 characters")
    @Column(name = "country", length = 50)
    private String country;
    
    @Size(max = 255, message = "Image path cannot exceed 255 characters")
    @Column(name = "image_path", length = 255)
    private String imagePath;
    
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    // Relationship with buses
    @OneToMany(mappedBy = "busCompany", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Set<Bus> buses;
    
    // Default constructor
    public BusCompany() {}
    
    // Constructor with required fields
    public BusCompany(String name, String registrationNumber, String companyCode) {
        this.name = name;
        this.registrationNumber = registrationNumber;
        this.companyCode = companyCode;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getRegistrationNumber() {
        return registrationNumber;
    }
    
    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }
    
    public String getCompanyCode() {
        return companyCode;
    }
    
    public void setCompanyCode(String companyCode) {
        this.companyCode = companyCode;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public String getPhone() {
        return phone;
    }
    
    public void setPhone(String phone) {
        this.phone = phone;
    }
    
    public String getAddress() {
        return address;
    }
    
    public void setAddress(String address) {
        this.address = address;
    }
    
    public String getCity() {
        return city;
    }
    
    public void setCity(String city) {
        this.city = city;
    }
    
    public String getPostalCode() {
        return postalCode;
    }
    
    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }
    
    public String getCountry() {
        return country;
    }
    
    public void setCountry(String country) {
        this.country = country;
    }
    
    public String getImagePath() {
        return imagePath;
    }
    
    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public Set<Bus> getBuses() {
        return buses;
    }
    
    public void setBuses(Set<Bus> buses) {
        this.buses = buses;
    }
    
    @Override
    public String toString() {
        return "BusCompany{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", registrationNumber='" + registrationNumber + '\'' +
                ", companyCode='" + companyCode + '\'' +
                ", email='" + email + '\'' +
                ", phone='" + phone + '\'' +
                ", city='" + city + '\'' +
                ", imagePath='" + imagePath + '\'' +
                ", isActive=" + isActive +
                '}';
    }
}
