package com.loanengine;

import com.loanengine.domain.LendingRule;
import com.loanengine.dto.ApplicationRequest;
import com.loanengine.repo.RuleRepository;
import com.loanengine.service.RuleEngine;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RuleEngineTest {

    private LendingRule rule(String field, String op, String threshold, String outcome) {
        LendingRule r = new LendingRule();
        r.setId(1L);
        r.setName(field + " " + op);
        r.setField(field);
        r.setOperator(op);
        r.setThreshold(new BigDecimal(threshold));
        r.setOutcome(outcome);
        r.setMessage("msg");
        r.setActive(true);
        r.setVersion(1);
        return r;
    }

    private ApplicationRequest app(int creditScore, int delinquencies) {
        return new ApplicationRequest("Test", 40, new BigDecimal("80000"), new BigDecimal("500"),
                new BigDecimal("10000"), 36, creditScore, delinquencies, 4.0);
    }

    @Test
    void firesOnlyTheRulesThatApply() {
        RuleRepository repo = mock(RuleRepository.class);
        when(repo.findByActiveTrue()).thenReturn(List.of(
                rule("credit_score", "LT", "550", "DECLINE"),
                rule("delinquencies", "GTE", "3", "DECLINE")));
        RuleEngine engine = new RuleEngine(repo);

        var hits = engine.evaluate(app(500, 0));
        assertEquals(1, hits.size());
        assertEquals("DECLINE", hits.get(0).outcome());

        assertTrue(engine.evaluate(app(700, 1)).isEmpty());
    }

    @Test
    void rulesVersionGrowsWhenARuleChanges() {
        RuleRepository repo = mock(RuleRepository.class);
        LendingRule r = rule("dti", "GT", "40", "REFER");
        when(repo.findAll()).thenReturn(List.of(r));
        RuleEngine engine = new RuleEngine(repo);

        String before = engine.rulesVersion();
        r.setVersion(2);
        assertTrue(!before.equals(engine.rulesVersion()));
    }
}
