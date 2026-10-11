package com.loanengine.service;

import com.loanengine.domain.LendingRule;
import com.loanengine.dto.ApplicationRequest;
import com.loanengine.repo.RuleRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RuleEngine {

    public record Hit(Long ruleId, String name, String outcome, String message, double actual, double threshold) {
    }

    private final RuleRepository rules;

    public RuleEngine(RuleRepository rules) {
        this.rules = rules;
    }

    public List<Hit> evaluate(ApplicationRequest app) {
        List<Hit> hits = new ArrayList<>();
        for (LendingRule r : rules.findByActiveTrue()) {
            double actual = Metrics.value(app, r.getField());
            double limit = r.getThreshold().doubleValue();
            if (compare(actual, r.getOperator(), limit)) {
                hits.add(new Hit(r.getId(), r.getName(), r.getOutcome(), r.getMessage(), round(actual), limit));
            }
        }
        return hits;
    }

    /** every edit to a rule bumps its version, so the sum only ever goes up */
    public String rulesVersion() {
        int sum = rules.findAll().stream().mapToInt(LendingRule::getVersion).sum();
        return "rules-v" + sum;
    }

    static boolean compare(double actual, String op, double limit) {
        switch (op) {
            case "GT":
                return actual > limit;
            case "GTE":
                return actual >= limit;
            case "LT":
                return actual < limit;
            case "LTE":
                return actual <= limit;
            default:
                throw new IllegalArgumentException("bad operator " + op);
        }
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
