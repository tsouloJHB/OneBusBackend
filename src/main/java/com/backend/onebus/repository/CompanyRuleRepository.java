package com.backend.onebus.repository;

import com.backend.onebus.model.CompanyRule;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CompanyRuleRepository extends JpaRepository<CompanyRule, Long> {

    List<CompanyRule> findByCompanyId(Long companyId);

    Optional<CompanyRule> findByCompanyIdAndRuleKey(Long companyId, String ruleKey);
}
