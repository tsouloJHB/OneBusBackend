package com.backend.onebus.repository;

import com.backend.onebus.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    java.util.List<User> findByCompanyId(Long companyId);
    long countByCompanyId(Long companyId);
}
