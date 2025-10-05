package com.backend.onebus.dto;

import jakarta.validation.constraints.*;

public class BusCompanyCreateDTO {
    
    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 100, message = "Company name must be between 2 and 100 characters")
    private String name;
    
    @NotBlank(message = "Registration number is required")
    @Size(min = 2, max = 50, message = "Registration number must be between 2 and 50 characters")
    private String registrationNumber;
    
    @NotBlank(message = "Company code is required")
    @Size(min = 2, max = 10, message = "Company code must be between 2 and 10 characters")
    private String companyCode;
    
    @Email(message = "Please provide a valid email address")
    private String email;
    
    // Allow digits and common phone punctuation (spaces, hyphens, parentheses), optional leading +
    // Minimum 7 and maximum 20 characters overall (digits + punctuation)
    @Pattern(regexp = "^[+]?[0-9()\\-\\s]{7,20}$", message = "Please provide a valid phone number")
    private String phone;
    
    @Size(max = 200, message = "Address cannot exceed 200 characters")
    private String address;
    
    @Size(max = 50, message = "City cannot exceed 50 characters")
    private String city;
    
    @Size(max = 20, message = "Postal code cannot exceed 20 characters")
    private String postalCode;
    
    @Size(max = 50, message = "Country cannot exceed 50 characters")
    private String country;
    
    private Boolean isActive = true;
    
    // Default constructor
    public BusCompanyCreateDTO() {}
    
    // Constructor
    public BusCompanyCreateDTO(String name, String registrationNumber, String companyCode) {
        this.name = name;
        this.registrationNumber = registrationNumber;
        this.companyCode = companyCode;
    }
    
    // Getters and Setters
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
    
    public Boolean getIsActive() {
        return isActive;
    }
    
    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
