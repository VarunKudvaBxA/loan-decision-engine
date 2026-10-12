package com.loanengine.web;

import com.loanengine.domain.LendingRule;
import com.loanengine.dto.ApplicationRequest;
import com.loanengine.dto.Dtos.RuleUpdate;
import com.loanengine.repo.RuleRepository;
import com.loanengine.service.AuditService;
import com.loanengine.service.DecisionService;
import com.loanengine.service.EvaluationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/** smaller endpoints: what-if simulator, rules, stats and audit check */
@RestController
@RequestMapping("/api")
public class InsightController {
    private final EvaluationService evaluator;
    private final RuleRepository rules;
    private final DecisionService decisions;
    private final AuditService audit;

    public InsightController(EvaluationService evaluator, RuleRepository rules, DecisionService decisions, AuditService audit) {
        this.evaluator = evaluator;
        this.rules = rules;
        this.decisions = decisions;
        this.audit = audit;
    }

    @PostMapping("/simulate")
    public EvaluationService.Evaluation simulate(@Valid @RequestBody ApplicationRequest req) {
        return evaluator.evaluate(req);
    }

    @GetMapping("/rules")
    public List<LendingRule> listRules() {
        return rules.findAllByOrderByIdAsc();
    }

    @PutMapping("/rules/{id}")
    @PreAuthorize("hasRole('SENIOR')")
    @Transactional
    public LendingRule updateRule(@PathVariable Long id, @RequestBody RuleUpdate body, Authentication auth) {
        LendingRule r = rules.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Rule not found"));
        String before = r.getThreshold() + "/" + r.getActive();
        if (body.threshold() != null) r.setThreshold(body.threshold());
        if (body.active() != null) r.setActive(body.active());
        r.setVersion(r.getVersion() + 1);
        audit.log("RULE", r.getId(), "RULE_CHANGED", auth.getName(),
                r.getName() + ": " + before + " -> " + r.getThreshold() + "/" + r.getActive());
        return r;
    }

    @GetMapping("/stats")
    public Map<String, Object> stats() {
        return decisions.stats();
    }

    @GetMapping("/audit/verify")
    public AuditService.Verification verifyAudit() {
        return audit.verify();
    }
}
