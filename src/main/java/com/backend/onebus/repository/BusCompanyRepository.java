package com.backend.onebus.repository;

import com.backend.onebus.model.BusCompany;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BusCompanyRepository extends JpaRepository<BusCompany, Long> {
    
    /**
     * Find bus company by registration number
     */
    Optional<BusCompany> findByRegistrationNumber(String registrationNumber);
    
    /**
     * Find bus company by company code
     */
    Optional<BusCompany> findByCompanyCode(String companyCode);
    
    /**
     * Find bus company by name (case insensitive)
     */
    Optional<BusCompany> findByNameIgnoreCase(String name);
    
    /**
     * Find all active bus companies
     */
    List<BusCompany> findByIsActiveTrue();
    
    /**
     * Find all inactive bus companies
     */
    List<BusCompany> findByIsActiveFalse();
    
    /**
     * Check if registration number exists
     */
    boolean existsByRegistrationNumber(String registrationNumber);
    
    /**
     * Check if company code exists
     */
    boolean existsByCompanyCode(String companyCode);
    
    /**
     * Find companies by name containing (case insensitive search)
     */
    @Query("SELECT bc FROM BusCompany bc WHERE LOWER(bc.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    List<BusCompany> findByNameContainingIgnoreCase(@Param("name") String name);
    
    /**
     * Find companies by city
     */
    List<BusCompany> findByCityIgnoreCase(String city);
    
    /**
     * Find companies by country
     */
    List<BusCompany> findByCountryIgnoreCase(String country);
    
    /**
     * Get companies with bus count
     */
    @Query("SELECT bc, COUNT(b) as busCount FROM BusCompany bc LEFT JOIN bc.buses b GROUP BY bc.id")
    List<Object[]> findAllWithBusCount();
    
    /**
     * Find companies by email
     */
    Optional<BusCompany> findByEmailIgnoreCase(String email);
}
