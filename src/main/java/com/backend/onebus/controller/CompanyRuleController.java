package com.backend.onebus.controller;

import com.backend.onebus.model.CompanyRule;
import com.backend.onebus.service.RuleEngineService;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.DeleteMapping;

@RestController
@RequestMapping("/api/company-rules")
public class CompanyRuleController {

    private final RuleEngineService ruleEngineService;

    public CompanyRuleController(RuleEngineService ruleEngineService) {
        this.ruleEngineService = ruleEngineService;
    }

    @GetMapping("/{companyId}")
    public ResponseEntity<List<CompanyRuleResponse>> getRules(@PathVariable Long companyId) {
        Map<String, CompanyRule> rules = ruleEngineService.getRulesForCompany(companyId);
        List<CompanyRuleResponse> response = rules.values().stream()
                .map(CompanyRuleResponse::fromEntity)
                .collect(Collectors.toList());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{companyId}")
    public ResponseEntity<CompanyRuleResponse> upsertRule(@PathVariable Long companyId, @RequestBody CompanyRuleRequest request) {
        CompanyRule saved = ruleEngineService.upsertRule(companyId, request.ruleKey(), request.ruleValue(), request.enabled());
        return ResponseEntity.status(HttpStatus.CREATED).body(CompanyRuleResponse.fromEntity(saved));
    }

    @DeleteMapping("/{companyId}/{ruleKey}")
    public ResponseEntity<Void> deleteRule(@PathVariable Long companyId, @PathVariable String ruleKey) {
        ruleEngineService.deleteRule(companyId, ruleKey);
        return ResponseEntity.noContent().build();
    }

    public record CompanyRuleRequest(String ruleKey, String ruleValue, boolean enabled) {}

    public record CompanyRuleResponse(Long id, Long companyId, String ruleKey, String ruleValue, boolean enabled) {
        static CompanyRuleResponse fromEntity(CompanyRule rule) {
            return new CompanyRuleResponse(rule.getId(), rule.getCompanyId(), rule.getRuleKey(), rule.getRuleValue(), rule.isEnabled());
        }
    }
}
