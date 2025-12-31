package com.backend.onebus.service;

import com.backend.onebus.constants.RuleConstants;
import com.backend.onebus.model.CompanyRule;
import com.backend.onebus.repository.BusCompanyRepository;
import com.backend.onebus.repository.CompanyRuleRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RuleEngineService {

    private static final Logger logger = LoggerFactory.getLogger(RuleEngineService.class);

    private final CompanyRuleRepository companyRuleRepository;
    private final BusCompanyRepository busCompanyRepository;

    // lightweight cache for company rules
    private final Map<Long, Map<String, CompanyRule>> ruleCache = new ConcurrentHashMap<>();

    public RuleEngineService(CompanyRuleRepository companyRuleRepository, BusCompanyRepository busCompanyRepository) {
        this.companyRuleRepository = companyRuleRepository;
        this.busCompanyRepository = busCompanyRepository;
    }

    public Map<String, CompanyRule> getRulesForCompany(Long companyId) {
        if (companyId == null) {
            return Collections.emptyMap();
        }

        return ruleCache.computeIfAbsent(companyId, id -> {
            List<CompanyRule> rules = companyRuleRepository.findByCompanyId(id);
            Map<String, CompanyRule> map = new HashMap<>();
            for (CompanyRule rule : rules) {
                map.put(rule.getRuleKey(), rule);
            }
            return map;
        });
    }

    public boolean isEnabled(Long companyId, String ruleKey) {
        return isEnabled(companyId, ruleKey, false);
    }

    public boolean isEnabled(Long companyId, String ruleKey, boolean defaultValue) {
        CompanyRule rule = getRulesForCompany(companyId).get(ruleKey);
        if (rule == null) {
            return defaultValue;
        }
        return rule.isEnabled();
    }

    public Optional<String> getValue(Long companyId, String ruleKey) {
        CompanyRule rule = getRulesForCompany(companyId).get(ruleKey);
        if (rule == null || !rule.isEnabled()) {
            return Optional.empty();
        }
        return Optional.ofNullable(rule.getRuleValue());
    }

    public boolean isDirectionAutoFlip(Long companyId) {
        return isEnabled(companyId, RuleConstants.AUTO_FLIP_AT_TERMINAL);
    }

    public boolean isCircularMode(Long companyId) {
        return isEnabled(companyId, RuleConstants.CIRCULAR_ROUTE_MODE);
    }

    public Optional<Long> resolveCompanyIdByName(String companyName) {
        return busCompanyRepository.findByNameIgnoreCase(companyName).map(company -> {
            Long id = company.getId();
            if (id != null) {
                // warm cache for this company
                getRulesForCompany(id);
            }
            return id;
        });
    }

    @Transactional
    public CompanyRule upsertRule(Long companyId, String ruleKey, String ruleValue, boolean enabled) {
        CompanyRule rule = companyRuleRepository
                .findByCompanyIdAndRuleKey(companyId, ruleKey)
                .orElse(new CompanyRule(companyId, ruleKey, ruleValue, enabled));
        rule.setRuleValue(ruleValue);
        rule.setEnabled(enabled);
        CompanyRule saved = companyRuleRepository.save(rule);
        ruleCache.remove(companyId); // drop cache to force reload next read
        logger.info("Upserted rule {} for company {} (enabled={})", ruleKey, companyId, enabled);
        return saved;
    }

    @Transactional
    public void deleteRule(Long companyId, String ruleKey) {
        companyRuleRepository.findByCompanyIdAndRuleKey(companyId, ruleKey)
                .ifPresent(rule -> {
                    companyRuleRepository.delete(rule);
                    ruleCache.remove(companyId);
                    logger.info("Deleted rule {} for company {}", ruleKey, companyId);
                });
    }
}
